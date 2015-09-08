package org.openhab.binding.modbus.handler;

import net.wimpi.modbus.io.ModbusTransaction;

public interface BridgeConnector {
    public boolean isConnected();

    public boolean connect();

    public void resetConnection();

    public ModbusTransaction getTransaction();

    public boolean isHeadless();
}
