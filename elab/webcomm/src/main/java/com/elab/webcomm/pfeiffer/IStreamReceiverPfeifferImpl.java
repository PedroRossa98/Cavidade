package com.elab.webcomm.pfeiffer;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.elab.webcomm.protocol.SerialProtocol;
import com.elab.webcomm.usb.UsbDataProvider;
import com.fazecast.jSerialComm.SerialPort;

// https://pypi.org/project/pfeiffer-vacuum-protocol/

@Service
public class IStreamReceiverPfeifferImpl  implements IStreamReceiverPfeiffer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final String requestPattern = "%1$3d00%2$3d02=?";   // {:03d}00{:03d}02=?
    private static final String commandPattern = "%1$d10%2$d%3$d$s";   // {:03d}10{:03d}{:02d}{:s}
    
    // error types
    private static final int NO_ERROR                  = 1;
    private static final int DEFECTIVE_TRANSMITTER     = 2;
    private static final int DEFECTIVE_MEMORY          = 3;
    
    // request / command types
    private static final int ERROR_CODE                = 303;
    private static final int PRESSURE                  = 740;
    private static final int PRESSURE_SETPOINT         = 741;
    private static final int CORRECTION_VALUE          = 742;
    
    
    private UsbDataProvider usbDataProvider;
    
    private Integer commandNum;
    private Boolean readingValue = false;
    private Double  pressureValue;
    private Integer pressureAddr;
    
    private Integer addr;
    private Integer rw;
    private Integer paramNum;
    private byte[]  data;
    
    
    @Override
    public void init() {
        usbDataProvider = new UsbDataProvider();
        usbDataProvider.initStreamReceiverPfeiffer(this);
    }

    @Override
    public void readPressure(String port, Integer addr) {
        usbDataProvider.disconnect();

        SerialPort serialPort = usbDataProvider.connect(port, SerialProtocol.RS232);
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 0);
        
        pressureValue = -1.0;
        
        commandNum = PRESSURE;
        pressureAddr = addr;
        
        sendDataRequest(pressureAddr, commandNum);
    }
    
    
    @Override
    public void setPressureValue(Double value) {
        readingValue = false;
        pressureValue = value;
    }
    
    
    @Override
    public Double getPressureValue() {
        return pressureValue;
    }
    
    
    @Override
    public Boolean getReadingValue() {
        return readingValue;
    }
    
    
    private int sendDataRequest(Integer addr, Integer paramNum) {

        String command = String.format(
                requestPattern,
                addr,
                paramNum
                );
        
        Integer checksum = computeChecksum(command);
        command += String.format("%1$3d\r", checksum);
        
        // logger.debug("sendDataRequest command : " + String.valueOf(command));
        readingValue = false;
        
        return usbDataProvider.write(command.getBytes());
    }
    
    
    private int sendControlCommand(Integer addr, Integer paramNum, String cmd) {
        
        String command = String.format(
                commandPattern,
                addr,
                paramNum,
                cmd.length(),
                cmd
                ); 
        
        commandNum = paramNum;
        
        Integer checksum = computeChecksum(command);
        command += String.format("%1$3d\r", checksum);
        
        // logger.debug("sendControlCommand command : " + String.valueOf(command));
        
        return usbDataProvider.write(command.getBytes());
    }
    
    
    @Override
    public void processDeviceResponse(byte[] message) {
        int messageLength = message.length;
        
        // Check the length
        if (messageLength < 14) {
            logger.debug("processDeviceResponse message : " + String.valueOf("gauge response too short to be valid"));
            try {
                throw new IOException("processDeviceResponse : gauge response too short to be valid");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // Check it is terminated correctly
        char terminator = (char) message[messageLength-1];
        if (terminator != '\r') {
            logger.debug("processDeviceResponse message : " + String.valueOf("gauge response incorrectly terminated"));
            try {
                throw new IOException("processDeviceResponse : gauge response incorrectly terminated");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // Evaluate the checksum
        byte[] byteChecksum =  Arrays.copyOfRange(message, messageLength-4, messageLength-1);
        byte[] byteMessage = Arrays.copyOfRange(message, 0, messageLength-5);
        Integer intChecksum = Integer.valueOf(Arrays.toString(byteChecksum));
        
        if (intChecksum != computeChecksum(byteMessage)) {
            logger.debug("processDeviceResponse message : " + String.valueOf("invalid checksum in gauge response"));
            try {
                throw new IOException("processDeviceResponse : invalid checksum in gauge response");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // Pull out the address
        addr      = Integer.valueOf(Arrays.copyOfRange(message, 0, 2).toString()); // int(r[:3])
        rw        = Integer.valueOf(Arrays.copyOfRange(message, 3, 3).toString()); // int(r[3:4])
        paramNum  = Integer.valueOf(Arrays.copyOfRange(message, 5, 7).toString()); // int(r[5:8])
        data      = Arrays.copyOfRange(message, 10, messageLength-5); //  r[10:-4]

        // Check for errors
        if (data.toString().equals("NO_DEF")) {
            logger.debug("processDeviceResponse message : " + String.valueOf("undefined parameter number"));
            try {
                throw new IOException("processDeviceResponse : undefined parameter number");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        if (data.toString().equals("_RANGE")) { 
            logger.debug("processDeviceResponse message : " + String.valueOf("data is out of range"));
            try {
                throw new IOException("processDeviceResponse : data is out of range");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        if (data.toString().equals("_LOGIC")) {
            logger.debug("processDeviceResponse message : " + String.valueOf("logic access violation"));
            try {
                throw new IOException("processDeviceResponse : logic access violation");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        switch (commandNum) {
            case PRESSURE:
                if (addr != pressureAddr || rw != 1 || paramNum != PRESSURE) {
                    logger.debug("processDeviceResponse message : " + String.valueOf("invalid response from gauge"));
                    try {
                        throw new IOException("processDeviceResponse : invalid response from gauge");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                }
                
                // Convert to a float
                Integer mantissa = Integer.valueOf(Arrays.copyOfRange(message, 0, 3).toString()); // int(data[:4])
                Integer exponent = Integer.valueOf(Arrays.copyOfRange(message, 4, data.length-1).toString()); // int(data[4:])
               
                pressureValue = Math.pow(mantissa*10, exponent-26); // float(mantissa*10**(exponent-26))
                readingValue = true;
                break;
            default:
                break;
        }

    }

    
    private Integer computeChecksum(byte[] message) {
        Integer checksum = -1;
        // sum([ord(x) for x in message]) % 256
        Integer sum = 0;
        for (int i = 0; i < message.length; i++){
            sum += (int) message[i];        
        }
        
        checksum = sum % 256;
        
        return checksum;
    }
    

    private Integer computeChecksum(String message) {
        Integer checksum = -1;
        
        // sum([ord(x) for x in message]) % 256
        Integer sum = 0;
        for (int i = 0; i < message.length(); i++){
            sum += (int) message.charAt(i);        
        }
        
        checksum = sum % 256;
        
        return checksum;
    }
    
    
    private void processMessage(byte[] message) {
        int messageLength = message.length;
    }
    
    
    private void processMessage(String readMessage) {
    }

}
