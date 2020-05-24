/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lcn.internal.connection;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.lcn.internal.LcnBindingConstants;
import org.openhab.binding.lcn.internal.common.LcnAddrMod;
import org.openhab.binding.lcn.internal.common.LcnChannelGroup;
import org.openhab.binding.lcn.internal.common.LcnDefs;
import org.openhab.binding.lcn.internal.common.LcnException;
import org.openhab.binding.lcn.internal.common.PckGenerator;
import org.openhab.binding.lcn.internal.common.Variable;
import org.openhab.binding.lcn.internal.common.VariableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds data of an LCN module.
 * <ul>
 * <li>Stores the module's firmware version (if requested)
 * <li>Manages the scheduling of status-requests
 * <li>Manages the scheduling of acknowledged commands
 * </ul>
 *
 * @author Tobias Jüttner - Initial Contribution
 * @author Fabian Wolter - Migration to OH2
 */
@NonNullByDefault
public class ModInfo {
    private final Logger logger = LoggerFactory.getLogger(ModInfo.class);
    /** Total number of request to sent before going into failed-state. */
    private static final int NUM_TRIES = 3;

    /** Poll interval for status values that automatically send their values on change. */
    private static final int MAX_STATUS_EVENTBASED_VALUEAGE_MSEC = 600000;

    /** Poll interval for status values that do not send their values on change (always polled). */
    private static final int MAX_STATUS_POLLED_VALUEAGE_MSEC = 30000;

    /** Status request delay after a command has been send which potentially changed that status. */
    private static final int STATUS_REQUEST_DELAY_AFTER_COMMAND_MSEC = 2000;

    /** The LCN module's address. */
    private final LcnAddrMod addr;

    /** Firmware date of the LCN module. -1 means "unknown". */
    private int firmwareVersion = -1;

    /** Firmware version request status. */
    private final RequestStatus requestFirmwareVersion = new RequestStatus(-1, NUM_TRIES, "Firmware Version");

    /** Output-port request status (0..3). */
    private final ArrayList<RequestStatus> requestStatusOutputs = new ArrayList<>();

    /** Relays request status (all 8). */
    private final RequestStatus requestStatusRelays = new RequestStatus(MAX_STATUS_EVENTBASED_VALUEAGE_MSEC, NUM_TRIES,
            "Relays");

    /** Binary-sensors request status (all 8). */
    private final RequestStatus requestStatusBinSensors = new RequestStatus(MAX_STATUS_EVENTBASED_VALUEAGE_MSEC,
            NUM_TRIES, "Binary Sensors");

    /**
     * Variables request status.
     * Lazy initialization: Will be filled once the firmware version is known.
     */
    private final Map<Variable, RequestStatus> requestStatusVars = new TreeMap<>();

    /**
     * Caches the values of the variables, needed for changing the values.
     */
    private final Map<Variable, VariableValue> variableValue = new TreeMap<>();

    /** LEDs and logic-operations request status (all 12+4). */
    private final RequestStatus requestStatusLedsAndLogicOps = new RequestStatus(MAX_STATUS_POLLED_VALUEAGE_MSEC,
            NUM_TRIES, "LEDs and Logic");

    /** Key lock-states request status (all tables, A-D). */
    private final RequestStatus requestStatusLockedKeys = new RequestStatus(MAX_STATUS_POLLED_VALUEAGE_MSEC, NUM_TRIES,
            "Key Locks");

    /**
     * Holds the last LCN variable requested whose response will not contain the variable's type.
     * {@link Variable#UNKNOWN} means there is currently no such request.
     */
    private Variable lastRequestedVarWithoutTypeInResponse = Variable.UNKNOWN;

    /**
     * List of queued PCK commands to be acknowledged by the LCN module.
     * Commands are always without address header.
     * Note that the first one might currently be "in progress".
     */
    private final LinkedList<@Nullable ByteBuffer> pckCommandsWithAck = new LinkedList<>();

    /** Status data for the currently processed {@link PckCommandWithAck}. */
    private final RequestStatus requestCurrentPckCommandWithAck = new RequestStatus(-1, NUM_TRIES, "Commands with Ack");

    /**
     * Constructor.
     *
     * @param addr the module's address
     */
    public ModInfo(LcnAddrMod addr) {
        this.addr = addr;
        for (int i = 0; i < LcnChannelGroup.OUTPUT.getCount(); ++i) {
            this.requestStatusOutputs
                    .add(new RequestStatus(MAX_STATUS_EVENTBASED_VALUEAGE_MSEC, NUM_TRIES, "Output " + (i + 1)));
        }

        for (Variable var : Variable.values()) {
            if (var != Variable.UNKNOWN) {
                this.requestStatusVars.put(var, new RequestStatus(MAX_STATUS_POLLED_VALUEAGE_MSEC, NUM_TRIES,
                        var.getType() + " " + (var.getNumber() + 1)));
            }
        }
    }

    /**
     * Gets the last requested variable whose response will not contain the variables type.
     *
     * @return the "typeless" variable
     */
    public Variable getLastRequestedVarWithoutTypeInResponse() {
        return this.lastRequestedVarWithoutTypeInResponse;
    }

    /**
     * Sets the last requested variable whose response will not contain the variables type.
     *
     * @param var the "typeless" variable
     */
    public void setLastRequestedVarWithoutTypeInResponse(Variable var) {
        this.lastRequestedVarWithoutTypeInResponse = var;
    }

    /**
     * Queues a PCK command to be sent.
     * It will request an acknowledge from the LCN module on receipt.
     * If there is no response within the request timeout, the command is retried.
     *
     * @param data the PCK command to send (without address header)
     * @param timeoutMSec the time to wait for a response before retrying a request
     * @param currTime the current time stamp
     */
    public void queuePckCommandWithAck(ByteBuffer data, Connection conn, long timeoutMSec, long currTime) {
        this.pckCommandsWithAck.add(data);
        // Try to process the new acknowledged command. Will do nothing if another one is still in progress.
        this.tryProcessNextCommandWithAck(conn, timeoutMSec, currTime);
    }

    /**
     * Called whenever an acknowledge is received from the LCN module.
     *
     * @param code the LCN internal code. -1 means "positive" acknowledge
     * @param timeoutMSec the time to wait for a response before retrying a request
     * @param currTime the current time stamp
     */
    public void onAck(int code, Connection conn, long timeoutMSec, long currTime) {
        if (this.requestCurrentPckCommandWithAck.isActive()) { // Check if we wait for an ack.
            this.pckCommandsWithAck.pollFirst();
            this.requestCurrentPckCommandWithAck.reset();
            // Try to process next acknowledged command
            this.tryProcessNextCommandWithAck(conn, timeoutMSec, currTime);
        }
    }

    /**
     * Sends the next acknowledged command from the queue.
     *
     * @param conn the {@link Connection} belonging to this {@link ModInfo}
     * @param timeoutMSec the time to wait for a response before retrying a request
     * @param currTime the current time stamp
     * @return true if a new command was sent
     * @throws LcnException when a command response timed out
     */
    private boolean tryProcessNextCommandWithAck(Connection conn, long timeoutMSec, long currTime) {
        // Use the chance to remove a failed command first
        if (this.requestCurrentPckCommandWithAck.isFailed(timeoutMSec, currTime)) {
            ByteBuffer failedCommand = this.pckCommandsWithAck.pollFirst();
            this.requestCurrentPckCommandWithAck.reset();

            if (failedCommand != null) {
                try {
                    logger.warn("{}: Module did not respond to command: {}", addr,
                            new String(failedCommand.array(), LcnDefs.LCN_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    // ignore
                }
            }
        }
        // Peek new command
        if (!this.pckCommandsWithAck.isEmpty() && !this.requestCurrentPckCommandWithAck.isActive()) {
            this.requestCurrentPckCommandWithAck.nextRequestIn(0, currTime);
        }
        ByteBuffer command = this.pckCommandsWithAck.peekFirst();
        if (command == null) {
            return false;
        }
        try {
            if (requestCurrentPckCommandWithAck.shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queueAndSend(new SendDataPck(addr, true, command));
                this.requestCurrentPckCommandWithAck.onRequestSent(currTime);
            }
        } catch (LcnException e) {
            try {
                logger.warn("{}: Could not send command: {}: {}", addr,
                        new String(command.array(), LcnDefs.LCN_ENCODING), e.getMessage());
            } catch (UnsupportedEncodingException e1) {
                // ignore
            }
        }
        return true;
    }

    /**
     * Triggers a request to retrieve the firmware version of the LCN module, if it is not known, yet.
     */
    public void requestFirmwareVersion() {
        if (firmwareVersion == -1) {
            requestFirmwareVersion.refresh();
        }
    }

    /**
     * Used to check if the module has the measurement processing firmware (since Feb. 2013).
     *
     * @return if the module has at least 4 threshold registers and 12 variables
     */
    public boolean hasExtendedMeasurementProcessing() {
        if (firmwareVersion == -1) {
            logger.warn("LCN module firmware version unknown");
            return false;
        }
        return firmwareVersion >= LcnBindingConstants.FIRMWARE_2013;
    }

    /**
     * Keeps the request logic active.
     * Must be called periodically.
     *
     * @param conn the {@link Connection} belonging to this {@link ModInfo}
     * @param timeoutMSec the time to wait for a response before retrying a request
     * @param currTime the current time stamp
     */
    void update(Connection conn, long timeoutMSec, long currTime) {
        RequestStatus r;
        try {
            // Firmware request
            if ((r = this.requestFirmwareVersion).shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queue(this.addr, false, PckGenerator.requestSn());
                r.onRequestSent(currTime);
                return;
            }
            // Output-port requests
            for (int i = 0; i < 4; ++i) {
                if ((r = this.requestStatusOutputs.get(i)).shouldSendNextRequest(timeoutMSec, currTime)) {
                    conn.queue(this.addr, false, PckGenerator.requestOutputStatus(i));
                    r.onRequestSent(currTime);
                    return;
                }
            }
            // Relays request
            if ((r = this.requestStatusRelays).shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queue(this.addr, false, PckGenerator.requestRelaysStatus());
                r.onRequestSent(currTime);
                return;
            }
            // Binary-sensors request
            if ((r = this.requestStatusBinSensors).shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queue(this.addr, false, PckGenerator.requestBinSensorsStatus());
                r.onRequestSent(currTime);
                return;
            }
            // Variable requests
            if (this.firmwareVersion != -1) { // Firmware version is required
                // Use the chance to remove a failed "typeless variable" request
                if (this.lastRequestedVarWithoutTypeInResponse != Variable.UNKNOWN) {
                    if (this.requestStatusVars.get(this.lastRequestedVarWithoutTypeInResponse).isTimeout(timeoutMSec,
                            currTime)) {
                        this.lastRequestedVarWithoutTypeInResponse = Variable.UNKNOWN;
                    }
                }
                // Variables
                for (Map.Entry<Variable, RequestStatus> kv : this.requestStatusVars.entrySet()) {
                    if ((r = kv.getValue()).shouldSendNextRequest(timeoutMSec, currTime)) {
                        // Detect if we can send immediately or if we have to wait for a "typeless" request first
                        boolean hasTypeInResponse = kv.getKey().hasTypeInResponse(this.firmwareVersion);
                        if (hasTypeInResponse || this.lastRequestedVarWithoutTypeInResponse == Variable.UNKNOWN) {
                            try {
                                conn.queue(this.addr, false,
                                        PckGenerator.requestVarStatus(kv.getKey(), this.firmwareVersion));
                                r.onRequestSent(currTime);
                                if (!hasTypeInResponse) {
                                    this.lastRequestedVarWithoutTypeInResponse = kv.getKey();
                                }
                                return;
                            } catch (LcnException ex) {
                                r.reset();
                            }
                        }
                    }
                }
            }
            // LEDs and logic-operations request
            if ((r = this.requestStatusLedsAndLogicOps).shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queue(this.addr, false, PckGenerator.requestLedsAndLogicOpsStatus());
                r.onRequestSent(currTime);
                return;
            }
            // Key-locks request
            if ((r = this.requestStatusLockedKeys).shouldSendNextRequest(timeoutMSec, currTime)) {
                conn.queue(this.addr, false, PckGenerator.requestKeyLocksStatus());
                r.onRequestSent(currTime);
                return;
            }
            // Try to send next acknowledged command. Will also detect failed ones.
            this.tryProcessNextCommandWithAck(conn, timeoutMSec, currTime);
        } catch (LcnException e) {
            logger.warn("{}: Failed to receive status message: {}", addr, e.getMessage());
        }
    }

    /**
     * Gets the LCN module's firmware date.
     *
     * @return the date
     */
    public int getFirmwareVersion() {
        return this.firmwareVersion;
    }

    /**
     * Sets the LCN module's firmware date.
     *
     * @param firmwareVersion the date
     */
    public void setFirmwareVersion(int firmwareVersion) {
        this.firmwareVersion = firmwareVersion;

        requestFirmwareVersion.onResponseReceived();

        // increase poll interval, if the LCN module sends status updates of a variable event-based
        requestStatusVars.entrySet().stream().filter(e -> e.getKey().isEventBased(firmwareVersion))
                .forEach(e -> e.getValue().setMaxAgeMSec(MAX_STATUS_EVENTBASED_VALUEAGE_MSEC));
    }

    /**
     * Updates the variable value cache.
     *
     * @param variable the variable to update
     * @param value the new value
     */
    public void updateVariableValue(Variable variable, VariableValue value) {
        variableValue.put(variable, value);
    }

    /**
     * Gets the current value of a variable from the cache.
     *
     * @param variable the variable to retrieve the value for
     * @return the value of the variable
     * @throws LcnException when the variable is not in the cache
     */
    public long getVariableValue(Variable variable) throws LcnException {
        return Optional.ofNullable(variableValue.get(variable)).map(v -> v.toNative(variable.useLcnSpecialValues()))
                .orElseThrow(() -> new LcnException("Current variable value unknown"));
    }

    /**
     * Requests the current value of all dimmer outputs.
     */
    public void refreshAllOutputs() {
        requestStatusOutputs.forEach(RequestStatus::refresh);
    }

    /**
     * Requests the current value of the given dimmer output.
     *
     * @param number 0..3
     */
    public void refreshOutput(int number) {
        requestStatusOutputs.get(number).refresh();
    }

    /**
     * Requests the current value of all relays.
     */
    public void refreshRelays() {
        requestStatusRelays.refresh();
    }

    /**
     * Requests the current value of all binary sensor.
     */
    public void refreshBinarySensors() {
        requestStatusBinSensors.refresh();
    }

    /**
     * Requests the current value of the given variable.
     *
     * @param variable the variable to request
     */
    public void refreshVariable(Variable variable) {
        requestStatusVars.get(variable).refresh();
    }

    /**
     * Requests the current value of all LEDs and logic operations.
     */
    public void refreshLedsAndLogic() {
        requestStatusLedsAndLogicOps.refresh();
    }

    /**
     * Requests the current value of all LEDs and logic operations, after a LED has been changed by openHAB.
     */
    public void refreshStatusLedsAnLogicAfterChange() {
        requestStatusLedsAndLogicOps.nextRequestIn(STATUS_REQUEST_DELAY_AFTER_COMMAND_MSEC, System.nanoTime());
    }

    /**
     * Requests the current locking states of all keys.
     */
    public void refreshStatusLockedKeys() {
        requestStatusLockedKeys.refresh();
    }

    /**
     * Requests the current locking states of all keys, after a lock state has been changed by openHAB.
     */
    public void refreshStatusStatusLockedKeysAfterChange() {
        requestStatusLockedKeys.nextRequestIn(STATUS_REQUEST_DELAY_AFTER_COMMAND_MSEC, System.nanoTime());
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: Dimmer Output
     *
     * @param outputId 0..3
     */
    public void onOutputResponseReceived(int outputId) {
        requestStatusOutputs.get(outputId).onResponseReceived();
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: Relay
     */
    public void onRelayResponseReceived() {
        requestStatusRelays.onResponseReceived();
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: Binary Sensor
     */
    public void onBinarySensorsResponseReceived() {
        requestStatusBinSensors.onResponseReceived();
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: Variable
     *
     * @param variable the received variable type
     */
    public void onVariableResponseReceived(Variable variable) {
        requestStatusVars.get(variable).onResponseReceived();
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: LEDs and logic
     */
    public void onLedsAndLogicResponseReceived() {
        requestStatusLedsAndLogicOps.onResponseReceived();
    }

    /**
     * Resets the value request logic, when a requested value has been received from the LCN module: Keys lock state
     */
    public void onLockedKeysResponseReceived() {
        requestStatusLockedKeys.onResponseReceived();
    }
}
