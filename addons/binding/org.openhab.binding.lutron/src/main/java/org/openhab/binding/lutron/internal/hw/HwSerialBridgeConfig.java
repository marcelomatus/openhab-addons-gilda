package org.openhab.binding.lutron.internal.hw;

/**
 * Configuration settings for an {@link org.openhab.binding.lutron.handler.HWSerialBridgeHandler}.
 *
 * @author Andrew Shilliday - Initial contribution
 */
public class HwSerialBridgeConfig {
    public static final String SERIAL_PORT = "serialPort";
    public static final String BAUD = "baudRate";
    public static final String UPDATE_TIME = "updateTime";

    private String serialPort;
    private Integer baudRate;
    private Boolean updateTime;

    public String getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(String serialPort) {
        this.serialPort = serialPort;
    }

    public Integer getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
    }

    public Boolean getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Boolean updateTime) {
        this.updateTime = updateTime;
    }

}