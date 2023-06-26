/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.freeboxos.internal.api.rest;

import static org.openhab.binding.freeboxos.internal.FreeboxOsBindingConstants.THING_VM;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.freeboxos.internal.api.FreeboxException;
import org.openhab.binding.freeboxos.internal.api.Response;
import org.openhab.binding.freeboxos.internal.api.rest.LoginManager.Session.Permission;

import inet.ipaddr.mac.MACAddress;

/**
 * The {@link VmManager} is the Java class used to handle api requests related to virtual machines
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class VmManager extends ListableRest<VmManager.VirtualMachine, VmManager.VirtualMachineResponse> {
    private static final String PATH = THING_VM.toLowerCase();

    public class VirtualMachineResponse extends Response<VirtualMachine> {
    }

    public static enum Status {
        STOPPED,
        RUNNING,
        UNKNOWN;
    }

    public static record VirtualMachine(int id, String name, MACAddress mac, Status status) {
    }

    public VmManager(FreeboxOsSession session) throws FreeboxException {
        super(session, Permission.VM, VirtualMachineResponse.class, session.getUriBuilder().path(PATH));
    }

    public void power(int vmId, boolean startIt) throws FreeboxException {
        post(Integer.toString(vmId), startIt ? "start" : "powerbutton");
    }
}
