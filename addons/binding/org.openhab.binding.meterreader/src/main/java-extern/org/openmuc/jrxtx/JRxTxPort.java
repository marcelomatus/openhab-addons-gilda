package org.openmuc.jrxtx;

import static gnu.io.SerialPort.*;
import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import gnu.io.factory.DefaultSerialPortFactory;
import gnu.io.factory.SerialPortFactory;

@SuppressWarnings("deprecation")
class JRxTxPort implements SerialPort {

    private volatile boolean closed;

    private gnu.io.SerialPort rxtxPort;

    private SerialInputStream serialIs;
    private SerialOutputStream serial0s;

    private String portName;

    private DataBits dataBits;

    private Parity parity;

    private StopBits stopBits;

    private int baudRate;

    private int serialPortTimeout;

    private FlowControl flowControl;

    public static JRxTxPort openSerialPort(String portName, int baudRate, Parity parity, DataBits dataBits,
            StopBits stopBits, FlowControl flowControl) throws IOException {
        try {
//            SerialPortUtil.appendSerialPortProperty(portName);
            SerialPortFactory serialFactory = new DefaultSerialPortFactory();
            gnu.io.SerialPort comPort = serialFactory.createSerialPort(portName);

            // if (!(comPort instanceof RXTXPort)) {
            // throw new SerialPortException("Unable to open the serial port. Port is not RXTX.");
            // }

            try {
                comPort.setSerialPortParams(baudRate, dataBits.getOldValue(), stopBits.getOldValue(),
                        parity.getOldValue());

                setFlowControl(flowControl, comPort);
            } catch (UnsupportedCommOperationException e) {
                String message = format("Unable to apply config on serial port.\n{0}", e.getMessage());
                throw new SerialPortException(message);
            }

            return new JRxTxPort(comPort, portName, baudRate, parity, dataBits, stopBits, flowControl);
        } catch (NoSuchPortException e) {
            String errMessage = format("Serial port {0} not found or port is busy.", portName);
            throw new PortNotFoundException(errMessage);
        } catch (PortInUseException e) {
            String errMessage = format("Serial port {0} is already in use.", portName);
            throw new PortNotFoundException(errMessage);
        } catch (UnsupportedCommOperationException e1) {
            throw new IOException(e1);
        }

    }

    private static void setFlowControl(FlowControl flowControl, gnu.io.SerialPort rxtxPort) throws IOException {
        try {
            switch (flowControl) {
                case RTS_CTS:
                    rxtxPort.setFlowControlMode(FLOWCONTROL_RTSCTS_IN | FLOWCONTROL_RTSCTS_OUT);
                    break;
                case XON_XOFF:
                    rxtxPort.setFlowControlMode(FLOWCONTROL_XONXOFF_IN | FLOWCONTROL_XONXOFF_OUT);
                    
                    break;
                    
                case NONE:
                default:
                    rxtxPort.setFlowControlMode(FLOWCONTROL_NONE);
                    break;
            }
        } catch(UnsupportedCommOperationException e) {
            throw new IOException("Failed to set FlowControl mode", e);
        }
    }

    private JRxTxPort(gnu.io.SerialPort comPort, String portName, int baudRate, Parity parity, DataBits dataBits,
            StopBits stopBits, FlowControl flowControl) throws IOException {
        this.rxtxPort = comPort;
        this.portName = portName;
        this.baudRate = baudRate;
        this.parity = parity;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.flowControl = flowControl;

        this.closed = false;

        this.serial0s = new SerialOutputStream(this.rxtxPort.getOutputStream());
        this.serialIs = new SerialInputStream();

    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (isClosed()) {
            throw new SerialPortException("Serial port is closed");
        }
        return this.serialIs;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isClosed()) {
            throw new SerialPortException("Serial port is closed");
        }

        return this.serial0s;
    }

    @Override
    public synchronized void close() throws IOException {
        if (isClosed()) {
            return;
        }

        try {
            this.serial0s.closeStream();
            this.serialIs.closeStream();
            this.rxtxPort.close();
            this.serial0s = null;
            this.serialIs = null;
            this.rxtxPort = null;
        } finally {
            this.closed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    private class SerialInputStream extends InputStream {
        private static final long SLEEP_TIME = 10L; // sleep appropriate time

        @Override
        public synchronized int read() throws IOException {
            long elapsedTime = 0;

            InputStream serialInputStream = rxtxPort.getInputStream();
            do {
                if (serialInputStream.available() > 0) {
                    return serialInputStream.read();
                }
                try {
                    Thread.sleep(SLEEP_TIME);
                    elapsedTime += SLEEP_TIME;
                } catch (InterruptedException e) {
                    // ignore
                }

                if (isClosed()) {
                    throw new SerialPortException("Serial port has been closed.");
                }
            } while (getSerialPortTimeout() == 0 || elapsedTime <= getSerialPortTimeout());

            throw new SerialPortTimeoutException();
        }

        @Override
        public int available() throws IOException {
            return rxtxPort.getInputStream().available();
        }

        private void closeStream() throws IOException {
            rxtxPort.getInputStream().close();
        }

        @Override
        public void close() throws IOException {
            JRxTxPort.this.close();
        }
    }

    private class SerialOutputStream extends OutputStream {

        private OutputStream serialOutputStream;

        public SerialOutputStream(OutputStream serialOutputStream) {
            this.serialOutputStream = serialOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            checkIfOpen();

            this.serialOutputStream.write(b);
        }

        private void checkIfOpen() throws SerialPortException {
            if (isClosed()) {
                throw new SerialPortException("Port has been closed.");
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkIfOpen();
            this.serialOutputStream.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            checkIfOpen();
            this.serialOutputStream.write(b);
        }

        @Override
        public void flush() throws IOException {
            checkIfOpen();
            this.serialOutputStream.flush();
        }

        private void closeStream() throws IOException {
            this.serialOutputStream.close();
        }

        @Override
        public void close() throws IOException {
            JRxTxPort.this.close();
        }
    }

    @Override
    public String getPortName() {
        return this.portName;
    }

    @Override
    public DataBits getDataBits() {
        return this.dataBits;
    }

    @Override
    public void setDataBits(DataBits dataBits) throws IOException {
        this.dataBits = dataBits;
        updateWrappedPort();
    }

    @Override
    public Parity getParity() {
        return this.parity;
    }

    @Override
    public void setParity(Parity parity) throws IOException {
        this.parity = parity;
        updateWrappedPort();
    }

    @Override
    public StopBits getStopBits() {
        return this.stopBits;
    }

    @Override
    public void setStopBits(StopBits stopBits) throws IOException {
        this.stopBits = stopBits;
        updateWrappedPort();
    }

    @Override
    public int getBaudRate() {
        return this.baudRate;
    }

    @Override
    public void setBaudRate(int baudRate) throws IOException {
        this.baudRate = baudRate;
        updateWrappedPort();
    }

    private void updateWrappedPort() throws IOException {
        try {
            this.rxtxPort.setSerialPortParams(this.baudRate, this.dataBits.getOldValue(), this.stopBits.getOldValue(),
                    this.parity.getOldValue());
        } catch (UnsupportedCommOperationException e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public int getSerialPortTimeout() {
        return this.serialPortTimeout;
    }

    @Override
    public void setSerialPortTimeout(int serialPortTimeout) throws IOException {
        this.serialPortTimeout = serialPortTimeout;
    }

    @Override
    public void setFlowControl(FlowControl flowControl) throws IOException {
        setFlowControl(flowControl, this.rxtxPort);
        this.flowControl = flowControl;
    }

    @Override
    public FlowControl getFlowControl() {
        return this.flowControl;
    }

}
