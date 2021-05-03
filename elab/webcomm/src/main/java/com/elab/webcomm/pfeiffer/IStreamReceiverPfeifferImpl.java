package com.elab.webcomm.pfeiffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
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
    
    private static final String requestPattern = "%1$03d00%2$03d02=?";   // {:03d}00{:03d}02=?
    private static final String commandPattern = "%1$03d10%2$03d%3$02d$s";   // {:03d}10{:03d}{:02d}{:s}
    
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
    public void execReadPressure() {
        Boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        
        String command = "python PTT200.py";
        
        
        if (isWindows) {
            logger.debug("execReadPressure command : " +  String.valueOf(command));
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        
        // builder.directory(new File(System.getProperty("user.home")));
        builder.directory(new File("D:\\Develop\\pfeiffer-vacuum"));
         
        try {
            Process process = builder.start();
            
            logger.debug("execReadPressure getInputStream : " +  String.valueOf(process.getInputStream())); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder strBuilder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null) {
                strBuilder.append(line);
                strBuilder.append(System.getProperty("line.separator"));
            }
            String result = strBuilder.toString();
            logger.debug("execReadPressure getInputStream result : " +  String.valueOf(result)); 
            
            try {
                int exitCode = process.waitFor();
                logger.debug("execReadPressure exitCode : " +  String.valueOf(exitCode)); 
                if (exitCode == 0) {
                    Integer pressureIndex = result.indexOf("Pressure");
                    logger.debug("execReadPressure exitCode : " +  String.valueOf(pressureIndex)); 
                    if (pressureIndex != -1) {
                        String strPressure = result.substring(pressureIndex);
                        String strList[] = strPressure.split(" ");
                        if (strList.length > 1) {
                            logger.debug("execReadPressure pressure : " +  String.valueOf(strPressure)); 
                            Double pressure = Double.valueOf(strList[1]);
                            logger.debug("execReadPressure pressure : " +  String.valueOf(pressure));
                            pressureValue = pressure;
                        }
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                // return "InterruptedException : " + e.getMessage();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            // return "IOException : " + e.getMessage();
        }
     }

    
    @Override
    public void readPressure(String port, Integer addr) {
        usbDataProvider.disconnect();

        SerialPort serialPort = usbDataProvider.connect(port, SerialProtocol.RS485);
        
        pressureValue = -1.0;
        
        if (serialPort != null) {
            commandNum = PRESSURE;
            pressureAddr = addr;
            
            sendDataRequest(pressureAddr, commandNum);
        }
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
        
        // logger.debug("IStreamReciverPfeifferImpl sendDataRequest command temp : " + String.valueOf(command) + "   " + String.valueOf(command.getBytes().length));

        Integer checksum = computeChecksum(command);
        // logger.debug("IStreamReciverPfeifferImpl sendDataRequest checksum : " + String.valueOf(checksum));
        
        command += String.format("%1$3d\r", checksum);
        
        // logger.debug("IStreamReciverPfeifferImpl sendDataRequest command : " + String.valueOf(command) + "   " + String.valueOf(command.getBytes().length));
        
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
        
        return usbDataProvider.write(command.getBytes());
    }
    
    
    @Override
    public void processDeviceResponse(byte[] message) {
        int messageLength = message.length;
        
        String strMessage = new String(message, StandardCharsets.UTF_8);
        logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 1 : " + strMessage);
        
        // Check the length
        if (messageLength < 14) {
            logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("gauge response too short to be valid"));
            try {
                throw new IOException("processDeviceResponse : gauge response too short to be valid");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }

        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 2 : ");
        
        // Check it is terminated correctly
        char terminator = (char) message[messageLength-1];
        if (terminator != '\r') {
            logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("gauge response incorrectly terminated"));
            try {
                throw new IOException("processDeviceResponse : gauge response incorrectly terminated");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 3 : ");
        
        // Evaluate the checksum
        byte[] byteChecksum = Arrays.copyOfRange(message, messageLength-4, messageLength-1);
        String strByteChecksum = new String(byteChecksum, StandardCharsets.UTF_8);
        byte[] byteMessage = Arrays.copyOfRange(message, 0, messageLength-4);
        String strByteMessage = new String(byteMessage, StandardCharsets.UTF_8);
        Integer intChecksum = Integer.valueOf(strByteChecksum);
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message byteChecksum 3 : " + String.valueOf(byteChecksum.length));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message strByteChecksum 3 : " + String.valueOf(strByteChecksum));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message strByteMessage 3 : " +  String.valueOf(strByteMessage));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message intChecksum 3 : " + String.valueOf(intChecksum));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message computeChecksum 3 : " + String.valueOf(computeChecksum(byteMessage)));
        
        if (intChecksum != computeChecksum(byteMessage)) {
            logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("invalid checksum in gauge response"));
            try {
                throw new IOException("processDeviceResponse : invalid checksum in gauge response");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 4 : ");
        
        // Pull out the address
        addr      = Integer.valueOf(strMessage.substring(0, 3)); // int(r[:3])
        rw        = Integer.valueOf(strMessage.substring(3, 4)); // int(r[3:4])
        paramNum  = Integer.valueOf(strMessage.substring(5, 8)); // int(r[5:8])
        data      = Arrays.copyOfRange(message, 10, messageLength-4); //  r[10:-4]
        String strData = new String(data, StandardCharsets.UTF_8);
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse commandNum  : " + String.valueOf(commandNum));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse addr  : " + String.valueOf(addr));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse rw  : " + String.valueOf(rw));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse paramNum  : " + String.valueOf(paramNum));
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse data  : " + String.valueOf(data));
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 5 : " + strData);
        
        if (data == null) {
            try {
                throw new IOException("IStreamReceiverPfeifferImpl processDeviceResponse : no data available");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           return;             
        }
        
        // Check for errors
        if (data.toString().equals("NO_DEF")) {
            logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("undefined parameter number"));
            try {
                throw new IOException("IStreamReceiverPfeifferImpl processDeviceResponse : undefined parameter number");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 6 : ");
        
        if (data.toString().equals("_RANGE")) { 
            logger.debug("processDeviceResponse message : " + String.valueOf("data is out of range"));
            try {
                throw new IOException("IStreamReceiverPfeifferImpl processDeviceResponse : data is out of range");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 7 : ");
        
        if (data.toString().equals("_LOGIC")) {
            logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("logic access violation"));
            try {
                throw new IOException("IStreamReceiverPfeifferImpl processDeviceResponse : logic access violation");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
        }
        
        // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 8 : ");
        
        switch (commandNum) {
            case PRESSURE:
                if (addr != pressureAddr || rw != 1 || paramNum != PRESSURE) {
                    logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message : " + String.valueOf("invalid response from gauge"));
                    try {
                        throw new IOException("IStreamReceiverPfeifferImpl processDeviceResponse : invalid response from gauge");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                }
                
                // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse message 9 : ");
                
                // Convert to a float
                Double mantissa = Double.valueOf(strData.substring(0, 4)); // int(data[:4])
                Integer exponent = Integer.valueOf(strData.substring(4)); // int(data[4:])
               
                // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse mantissa : " + String.valueOf(mantissa));
                // logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse exponent : " + String.valueOf(exponent));
               
                pressureValue = mantissa * Math.pow(10, exponent-26); // float(mantissa*10**(exponent-26))

                logger.debug("IStreamReceiverPfeifferImpl processDeviceResponse pressureValue : " + String.valueOf(pressureValue));
                
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
