package com.elab.webcomm.usb;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.elab.webcomm.arinst.IStreamReceiverArinst;
import com.elab.webcomm.pfeiffer.IStreamReceiverPfeiffer;
import com.elab.webcomm.protocol.SerialProtocol;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;


// https://github.com/java-native/jssc/blob/master/src/main/java/jssc/SerialPort.java
// https://www.codota.com/code/java/classes/jssc.SerialPortEvent
// https://gist.github.com/aemyers/704d0a1ac13fea62d703b1125145f82e - jSerialComm
// https://fazecast.github.io/jSerialComm/javadoc/index-all.html

@Service
public class UsbDataProvider implements SerialPortMessageListener {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final String ARINST = "streamReceiverArinst";
    private static final String PFEIFFER = "streamReceiverPfeiffer";
    
    private static final Integer BAUDRATE_115200 = 115200;
    private static final Integer BAUDRATE_9600 = 9600;
    private static final Integer DATABITS_8 = 8;
    private static final Integer STOPBITS_1 = SerialPort.ONE_STOP_BIT;
    private static final Integer PARITY_NONE = SerialPort.NO_PARITY;
    private static final Boolean RS485_MODE = true;
    
    private static SerialPort serialPort;

    private String streamReceiver = "";
    private IStreamReceiverArinst   streamReceiverArinst;
    private IStreamReceiverPfeiffer streamReceiverPfeiffer;
    
    private int messageIndex = 0;
    private byte[] message = new byte[2048];


    public UsbDataProvider() {
    }


    public void initStreamReceiverArinst(IStreamReceiverArinst streamReceiverArinst) {
        this.streamReceiverArinst = streamReceiverArinst;
        streamReceiver = ARINST;
    }

    
    public void initStreamReceiverPfeiffer(IStreamReceiverPfeiffer streamReceiverPfeiffer) {
        this.streamReceiverPfeiffer = streamReceiverPfeiffer;
        streamReceiver = PFEIFFER;
    }

    @SuppressWarnings("static-access")
    public SerialPort connect(String portName, SerialProtocol protocol) {
        disconnect();
        
        // logger.debug("portName : " + String.valueOf(portName) + "    " + "SerialProtocol : " + String.valueOf(protocol));
        serialPort = SerialPort.getCommPort(portName);

        boolean opened = serialPort.openPort();
        // logger.debug("portName : " + String.valueOf(portName) + "    " + "opened : " + String.valueOf(opened));
        
        if (protocol == SerialProtocol.RS232) {
            serialPort.setComPortParameters(
                BAUDRATE_115200,
                DATABITS_8,
                STOPBITS_1,
                PARITY_NONE
            );
        }
        
        if (protocol == SerialProtocol.RS485) {
            serialPort.setComPortParameters(
                BAUDRATE_9600,
                DATABITS_8,
                STOPBITS_1,
                PARITY_NONE,
                RS485_MODE
            );
        }
        
        // logger.debug("UsbDataProvider 1 : " + streamReceiver);
        
        if (!opened) {
            try {
                throw new IOException("UsbDataProvider : unable to open port : " + String.valueOf(portName));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                return null;
            }
        }
        
        final int stale = serialPort.bytesAvailable();
        
        if (stale > 0) {
            logger.debug("flushing " + String.valueOf(stale) + " stale bytes...");
            serialPort.readBytes(new byte[stale], stale);
        }
        
        if (streamReceiver == ARINST) {
            int flowcontrolMask = SerialPort.FLOW_CONTROL_RTS_ENABLED + SerialPort.FLOW_CONTROL_CTS_ENABLED + SerialPort.FLOW_CONTROL_DSR_ENABLED;
            serialPort.setFlowControl(flowcontrolMask);
        }
        
        // logger.debug("UsbDataProvider 2 : " + streamReceiver);
        
        final boolean listening = serialPort.addDataListener(this);
        
        if (!listening) {
            try {
                throw new IOException("UsbDataProvider : unable to listen");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                return null;
            }
        }
        
        // logger.debug("connect : return !!! " + String.valueOf(serialPort.getSystemPortName()));
        
        // logger.debug("UsbDataProvider 3 : " + streamReceiver);
        
        return serialPort;
    }

    public void disconnect() {
        if (serialPort != null) {
            serialPort.closePort();
            serialPort = null;
        }
    }

    public boolean isConnected() {
        return serialPort != null;
    }

    public boolean isUsbDeviceConnected(String portName) {
        return serialPort != null && serialPort.getSystemPortName().contentEquals(portName);
    }

    
    public int write(byte[] buffer) {
        
        if (serialPort != null) {
            // logger.debug("UsbDataProvider write : buffer " + String.valueOf(buffer) + "    " + String.valueOf(buffer.length));
            // for (byte b1: buffer) {
            //     logger.debug("UsbDataProvider write byte : " + b1);
            // }
            
            return serialPort.writeBytes(buffer, buffer.length);
        }
        
        return -1;
    }

    
    @Override
    public int getListeningEvents() {
        if (streamReceiver == PFEIFFER) {
            return serialPort.LISTENING_EVENT_DATA_WRITTEN; 
        }
        
        return serialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }


    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        logger.debug("UsbDataProvider SerialEvent streamReceiver : " + String.valueOf(streamReceiver));
        logger.debug("UsbDataProvider SerialEvent Type : " + String.valueOf(serialPortEvent.getEventType()));
        // logger.debug("UsbDataProvider SerialEvent LISTENING_EVENT_DATA_AVAILABLE : " + String.valueOf(serialPort.LISTENING_EVENT_DATA_AVAILABLE));
        // logger.debug("UsbDataProvider SerialEvent LISTENING_EVENT_DATA_WRITTEN : " + String.valueOf(serialPort.LISTENING_EVENT_DATA_WRITTEN));
        // logger.debug("UsbDataProvider SerialEvent LISTENING_EVENT_DATA_RECEIVED : " + String.valueOf(serialPort.LISTENING_EVENT_DATA_RECEIVED));

        if (streamReceiver.equals(PFEIFFER)) {
            if (serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_WRITTEN) {
                return;
            }        
            
            try {
                Thread.sleep(100);
                
                Integer RETRY_TIMES = 1;
                for (int i = 0; i < RETRY_TIMES; i++) {
                    byte[] buffer = new byte[serialPortEvent.getSerialPort().bytesAvailable()];
                    // logger.debug("UsbDataProvider SerialEvent bytesAvailable : " + buffer + "   " + String.valueOf(serialPortEvent.getSerialPort().bytesAvailable()));
                    // logger.debug("UsbDataProvider SerialEvent buffer : " + String.valueOf(buffer.length));
                    int read = serialPortEvent.getSerialPort().readBytes(buffer, buffer.length);
                    logger.debug("UsbDataProvider SerialEvent read : " + String.valueOf(read));
                    
                    // for (byte charData : buffer) {
                    //    logger.debug("UsbDataProvider SerialEvent charData : " + String.valueOf(charData) + "   " + (char) charData);
                    // }
                    
                    if (read > 0) {
                        disconnect();
                        streamReceiverPfeiffer.processDeviceResponse(buffer);
                    }
                }

                disconnect();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        
        
        if (serialPortEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }

        byte[] buffer = new byte[serialPortEvent.getSerialPort().bytesAvailable()];
        
        if (buffer != null) {
        
            int read = serialPortEvent.getSerialPort().readBytes(buffer, buffer.length);
            // logger.debug("SerialEvent read : " + String.valueOf(read));
            
            if (streamReceiver.equals(ARINST)) {
                for (byte charData : buffer) {
                    if (messageIndex >= 1 && message[messageIndex - 1] == 13 && charData == 10) {
                        if (streamReceiverArinst != null) {
                            byte[] dst = new byte[messageIndex - 1];
                            System.arraycopy(message, 0, dst, 0, messageIndex - 1);
                            
                            // logger.debug("SerialEvent dst : " + String.valueOf(new String(dst)));
                            streamReceiverArinst.processDeviceResponse(dst);
                        }
        
                        for (int j = 0; j < messageIndex; j++) {
                            message[j] = 0;
                        }
                        messageIndex = 0;
                    } else {
                        if (messageIndex < message.length) {
                            message[messageIndex++] = charData;
                        } else {
                            for (int j = 0; j < messageIndex; j++) {
                                message[j] = 0;
                            }
                            messageIndex = 0;
                        }
                    }
                }
            }

        }

    }


    public ArrayList<String> getUsbDevices() {
        SerialPort[] portList = SerialPort.getCommPorts();
        String[] portNames = new String[portList.length];
        
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < portList.length; i++) {
            portNames[i] = portList[i].getSystemPortName();
            
        }
        Collections.addAll(result, portNames);

        return result;
    }
    
    @Override
    public boolean delimiterIndicatesEndOfMessage() {
        // TODO Auto-generated method stub
        return true;
    }


    @Override
    public byte[] getMessageDelimiter() {

        return new byte[] { (byte)0x0B, (byte)0x65 };
    }


}
