package com.elab.webcomm.pfeiffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class ReceiverPfeifferPressure {
    
    private static final Logger logger = LoggerFactory.getLogger(ReceiverPfeifferPressure.class);

    private static final String requestPattern = "%1$03d00%2$03d02=?";   // {:03d}00{:03d}02=?
    
    private static final int PORT_RETRY_TIMES = 20;
    
    
    private static final Integer BAUDRATE_9600 = 9600;
    private static final Integer DATABITS_8 = 8;
    private static final Integer STOPBITS_1 = SerialPort.ONE_STOP_BIT;
    private static final Integer PARITY_NONE = SerialPort.NO_PARITY;
    private static final Boolean RS485_MODE = true;
    
    private static final int PRESSURE                  = 740;
    
    
    public static Double execReadPressure() {
        Boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        
        String command = "python PTT200.py";
        
        
        if (isWindows) {
            logger.debug("ReceiverPfeifferPressure execReadPressure command : " +  String.valueOf(command));
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        
        // builder.directory(new File(System.getProperty("user.home")));
        builder.directory(new File("D:\\Develop\\pfeiffer-vacuum"));
         
        try {
            Process process = builder.start();
            
            logger.debug("ReceiverPfeifferPressure execReadPressure getInputStream : " +  String.valueOf(process.getInputStream())); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder strBuilder = new StringBuilder();
            String line = null;
            while ( (line = reader.readLine()) != null) {
                strBuilder.append(line);
                strBuilder.append(System.getProperty("line.separator"));
            }
            String result = strBuilder.toString();
            logger.debug("ReceiverPfeifferPressure execReadPressure getInputStream result : " +  String.valueOf(result)); 
            
            try {
                int exitCode = process.waitFor();
                logger.debug("ReceiverPfeifferPressure execReadPressure exitCode : " +  String.valueOf(exitCode)); 
                if (exitCode == 0) {
                    Integer pressureIndex = result.indexOf("Pressure");
                    logger.debug("ReceiverPfeifferPressure execReadPressure exitCode : " +  String.valueOf(pressureIndex)); 
                    if (pressureIndex != -1) {
                        String strPressure = result.substring(pressureIndex);
                        String strList[] = strPressure.split(" ");
                        if (strList.length > 1) {
                            logger.debug("execReadPressure pressure : " +  String.valueOf(strPressure)); 
                            Double pressure = Double.valueOf(strList[1]);
                            logger.debug("execReadPressure pressure : " +  String.valueOf(pressure));
                            return pressure;
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
        
        return -1.0;
    }

    public static Double readPressure(String port, Integer addr) {

        SerialPort serialPort = SerialPort.getCommPort(port);
        Integer commandNum = PRESSURE;
        
        if (serialPort != null) {
            
            boolean opened = serialPort.openPort();
            
            serialPort.setComPortParameters(
                    BAUDRATE_9600,
                    DATABITS_8,
                    STOPBITS_1,
                    PARITY_NONE,
                    RS485_MODE
                );
            
            String command = String.format(
                    requestPattern,
                    addr,
                    commandNum
                    );
            
            // logger.debug("ReceiverPfeifferPressure sendDataRequest command temp : " + String.valueOf(command) + "   " + String.valueOf(command.getBytes().length));

            Integer checksum = computeChecksum(command);
            // logger.debug("ReceiverPfeifferPressure sendDataRequest checksum : " + String.valueOf(checksum));
            
            command += String.format("%1$3d\r", checksum);
 
            // logger.debug("ReceiverPfeifferPressure sendDataRequest command temp : " + String.valueOf(command) + "   " + String.valueOf(command.getBytes().length));

            serialPort.writeBytes(command.getBytes(), command.getBytes().length);
 
            Integer retryCount = 0;
            
            try {
                while (serialPort.bytesAvailable() == 0 && retryCount < PORT_RETRY_TIMES) {
                    logger.debug("ReceiverPfeifferPressure serialPort sleep retryCount : " + retryCount);
                    retryCount++;
                    Thread.sleep(10);
                }
                
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int read = serialPort.readBytes(buffer, buffer.length);
                    // logger.debug("ReceiverPfeifferPressure serialPort read : " + String.valueOf(read));
                    String strBuffer = new String(buffer, StandardCharsets.UTF_8);
                    // logger.debug("ReceiverPfeifferPressure serialPort strBuffer : " + String.valueOf(strBuffer));
                    
                    serialPort.closePort();
                    
                    byte data[] = Arrays.copyOfRange(buffer, 10, buffer.length-4); //  r[10:-4]
                    String strData = new String(data, StandardCharsets.UTF_8);
                    // logger.debug("ReceiverPfeifferPressure serialPort strData : " + String.valueOf(strData));
                    
                    // Convert to a float
                    Double mantissa = Double.valueOf(strData.substring(0, 4)); // int(data[:4])
                    Integer exponent = Integer.valueOf(strData.substring(4)); // int(data[4:])
                    
                    return mantissa * Math.pow(10, exponent-26); // float(mantissa*10**(exponent-26))
                }
                
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            
            serialPort.closePort();
        }
        
        return -1.0;
    }
    
    private static Integer computeChecksum(String message) {
        Integer checksum = -1;
        
        // sum([ord(x) for x in message]) % 256
        Integer sum = 0;
        for (int i = 0; i < message.length(); i++){
            sum += (int) message.charAt(i);        
        }
        
        checksum = sum % 256;
        
        return checksum;
    }

}
