package com.elab.webcomm.arinst;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class ReceiverArinst22 {

    private static final Logger logger = LoggerFactory.getLogger(ReceiverArinst22.class);
    
    private static final String commandGONPattern = "gon %1$d \r\n"; 
    private static final String commandSGAPattern = "sga %1$d %2$d \r\n"; 
    private static final String command22Pattern  = "scn22 %1$d %2$d %3$d %4$d %5$d %6$d %7$d %8$d \r\n";
    
    private static final int      SCAN_RETRY_TIMES = 1;
    private static final int      PORT_RETRY_TIMES = 100;
    
    private static final Integer  GENERATOR_POWER    = -25;
    private static final Integer  ATTENUATION        = -10;
    private static final Long     INTERMEDIATE_FREQ  = 10700000L;
    private static final Integer  POINT_TIMEOUT      = 200;
    private static final Integer  POINT_ADC_SAMPLES  = 20;
    
    private static final Integer BAUDRATE_115200     = 115200;
    private static final Integer BAUDRATE_9600       = 9600;
    private static final Integer DATABITS_8          = 8;
    private static final Integer STOPBITS_1          = SerialPort.ONE_STOP_BIT;
    private static final Integer PARITY_NONE         = SerialPort.NO_PARITY;
    
    private static final int     VALUE_MASK          = 0x07FF;
    private static final int     INDEX_MASK          = 0xF800;   
    private static final String  NEW_LINE            = System.getProperty("line.separator");
    private static final double  SLEEP_FACTOR        = 1.6;     // to wait for long scans
    
    
    
    public static List <Map<String, String>> startScan(String port, Long start, Long stop, Long step) throws IOException {
        
        SerialPort serialPort = SerialPort.getCommPort(port);
        
        if (serialPort != null) {
            
            boolean opened = serialPort.openPort();
            
            if (!opened) {
                 logger.debug("ReceiverArinst22 : unable to open port : " + String.valueOf(port));
                 return new ArrayList <Map<String, String>> ();
            }
            
            serialPort.setComPortParameters(
                    BAUDRATE_115200,
                    DATABITS_8,
                    STOPBITS_1,
                    PARITY_NONE
            );
            
            final int stale = serialPort.bytesAvailable();
            
            if (stale > 0) {
                logger.debug("ReceiverArinst22 flushing " + String.valueOf(stale) + " stale bytes...");
                serialPort.readBytes(new byte[stale], stale);
            }
            
            Integer commandIndex = 0;
            Integer commandAttenuation = (100 * 100) + ((GENERATOR_POWER + 15) * 100); 
            
            setGeneratorON(serialPort, commandIndex);
            setOutputAttenuation(serialPort, commandAttenuation, commandIndex);
            
            Integer scanCount = 0;
            List <Map<String, String>> infoList = new ArrayList <Map<String, String>> ();
            
            while (scanCount < SCAN_RETRY_TIMES) {
                infoList = scanData(serialPort, start, stop, step, 
                        POINT_TIMEOUT, POINT_ADC_SAMPLES, INTERMEDIATE_FREQ, commandAttenuation, commandIndex);
            
                if (infoList.size() == 0) {
                    scanCount++;
                } else {
                    break;
                }
            }
            
            serialPort.closePort();
            
            return infoList;
        }
        
        return new ArrayList <Map<String, String>> ();
    }
    
    private static void setGeneratorON(SerialPort serialPort, Integer commandIndex) {

        String command = String.format(
                commandGONPattern,
                commandIndex
            );
        
        logger.debug("ReceiverArinst22 setGeneratorON : " + String.valueOf(command));
        
        serialPort.writeBytes(command.getBytes(), command.getBytes().length);
    }
    
    
    private static void setOutputAttenuation(SerialPort serialPort, Integer commandAttenuation, Integer commandIndex) {
        
        String command = String.format(
                commandSGAPattern,
                commandAttenuation,
                commandIndex
            );
        
        logger.debug("ReceiverArinst22 setOutputAttenuation : " + String.valueOf(command));
        
        serialPort.writeBytes(command.getBytes(), command.getBytes().length);
    }
 
    
    private static List <Map<String, String>> scanData(SerialPort serialPort, Long start, Long stop, Long step, 
                             Integer pointTimeout, Integer pointADCSamples, 
                                            Long intermediateFrequency, Integer commandAttenuation, Integer commandIndex) {
        
        List <Map<String, String>> infoList = new ArrayList <Map<String, String>> ();
       
        List<Double> amplitudeResult =  new ArrayList<Double>();
        
        String command = String.format(
            command22Pattern,
            start,
            stop,
            step,
            pointTimeout,
            pointADCSamples,
            intermediateFrequency,
            commandAttenuation,
            commandIndex               // command id (can be a random integer value)
        );
        
        logger.debug("ReceiverArinst22 scanData : " + String.valueOf(command));
        
        serialPort.writeBytes(command.getBytes(), command.getBytes().length);
        
        Long bufferSize = (stop - start) / step;
        
        try {
            Thread.sleep(100);
            
            logger.debug("ReceiverArinst22 bufferSize : " + String.valueOf(bufferSize));
            byte[] byteBufferData = readSerialPort(serialPort, bufferSize);
            
            if (byteBufferData.length > 0) {
                char[] chrBufferData = getBufferData(byteBufferData);
                
                if (byteBufferData.length > 0) {
                    infoList = processBufferData(chrBufferData, start, stop, step);
                }
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        logger.debug("ReceiverArinst22 scanData end infoList size : " + String.valueOf(infoList.size()));
        logger.debug("ReceiverArinst22 scanData end start stop step bufferSize : " + 
                String.valueOf(start) + "    " + String.valueOf(stop) + "    " + String.valueOf(step) + "    " + String.valueOf(bufferSize));
        
        return infoList;
    }
    
    
    private static byte[] readSerialPort(SerialPort serialPort, Long bufferSize) {
        Integer retryCount = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            if (bufferSize < 1500) {
                Thread.sleep(2000);
            } else {
                Thread.sleep((long)(bufferSize * SLEEP_FACTOR));
            }
            
            while (serialPort.bytesAvailable() == 0 && retryCount < PORT_RETRY_TIMES) {
                logger.debug("ReceiverArinst22 readSerialPort sleep retryCount : " + retryCount);
                retryCount++;
                Thread.sleep(50);
            }
        
            while (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                int read = serialPort.readBytes(buffer, buffer.length);
                logger.debug("ReceiverArinst22 scanData read : " + String.valueOf(read));

                // String strData = new String(buffer, StandardCharsets.UTF_8);
                // logger.debug("ReceiverArinst22 scanData buffer length : " + String.valueOf(buffer.length));
                // logger.debug("ReceiverArinst22 scanData buffer strData : " + String.valueOf(buffer));
                
                if (read > 0) {
                    try {
                        outputStream.write(buffer);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }  
                }
            }
            

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        
        // logger.debug("ReceiverArinst22 scanData buffer outputStream : " + String.valueOf(outputStream.size()));
        // logger.debug("ReceiverArinst22 scanData buffer outputStream : " + String.valueOf(outputStream));

        return outputStream.toByteArray();
    }
    
    
    private static char[] getBufferData(byte[] streamOutput) {
        
        if (streamOutput.length > 0) {
            String strData = new String(streamOutput);
            Integer scn22Index = strData.indexOf("scn22");
            Integer lastCompleteIndex = strData.lastIndexOf("complete");
            Boolean completed = scn22Index < lastCompleteIndex;
            
            // logger.debug("ReceiverArinst22 streamOutput buffer length : " + String.valueOf(streamOutput.length));
            // logger.debug("ReceiverArinst22 streamOutput buffer strData : " + String.valueOf(strData));
            // logger.debug("ReceiverArinst22 streamOutput buffer strData scn22Index : " + String.valueOf(scn22Index));
            // logger.debug("ReceiverArinst22 streamOutput buffer strData lastComplete : " + String.valueOf(lastCompleteIndex));
            logger.debug("ReceiverArinst22 streamOutput buffer completed : " + String.valueOf(completed));
            
            if (scn22Index > 0) {
                String strBuffer = strData.substring(scn22Index);
                Integer newlineIndex = strBuffer.indexOf(NEW_LINE);
                logger.debug("ReceiverArinst22 streamOutput buffer newlineIndex : " + String.valueOf(newlineIndex));
                
                if (completed) {
                    Integer completeIndex = strBuffer.lastIndexOf("complete");
                    strBuffer = strBuffer.substring(newlineIndex + 2, completeIndex - 2);
                    char[] chrBuffer = strBuffer.toCharArray();

                    // logger.debug("ReceiverArinst22 streamOutput buffer chrBuffer : " + String.valueOf(chrBuffer));
                    
                    int listFrom = chrBuffer.length - 10 > 0 ? chrBuffer.length - 10 : 0;
                    int listTo = chrBuffer.length - 1 > 700 ? 700 : chrBuffer.length - 1;
                  
//                    for (int i = 0 ; i < listTo ; i += 1) {
//                        logger.debug("ReceiverArinst22 streamOutput strBuffer Start   (" + i +") : " + 
//                                    String.valueOf(chrBuffer[i]) + "    " +  String.valueOf((int)chrBuffer[i]) + "    "  + String.valueOf(Integer.toBinaryString(chrBuffer[i])));
//                    }
//                    for (int i = listFrom ; i < chrBuffer.length - 1 ; i += 1) {
//                        logger.debug("ReceiverArinst22 streamOutput strBuffer Start   (" + i +") : " + 
//                                    String.valueOf(chrBuffer[i]) + "    " +  String.valueOf((int)chrBuffer[i]) + "    "  + String.valueOf(Integer.toBinaryString(chrBuffer[i])));
//                    }
//                    
//                    for (int i = chrBuffer.length - 1  ; i > listTo ; i -= 1) {
//                        logger.debug("ReceiverArinst22 streamOutput strBuffer End   (" + i +") : " + 
//                                    String.valueOf(chrBuffer[i]) + "    " +  String.valueOf((int)chrBuffer[i]) + "    "  + String.valueOf(Integer.toBinaryString(chrBuffer[i])));
//                    }
                    return chrBuffer;
                } else {
                    strBuffer = strBuffer.substring(newlineIndex + 2);
                    char[] chrBuffer = strBuffer.toCharArray();
                    return chrBuffer;
                }
            }
        }
        return new char[0];
    }
    
    private static List <Map<String, String>> processBufferData(char[] chrBuffer,  Long start, Long stop, Long step) {
        List <Map<String, String>> infoList = new ArrayList <Map<String, String>> ();
//        for (int i = 0; i < chrBuffer.length - 1; i += 1) {
//            
//            logger.debug("ReceiverArinst22 scanData buffer strData : " + 
//            String.valueOf(Byte.toUnsignedInt((byte)chrBuffer[i])) + "   " + String.valueOf(chrBuffer[i]));
//        }     
        
        for (int i = 0; i < chrBuffer.length - 1 ; i += 2) {
            // logger.debug("ReceiverArinst22 scanData strBuffer    (" + i +") : " + String.valueOf(Integer.toBinaryString(chrBuffer[i])));
            // logger.debug("ReceiverArinst22 scanData strBuffer    (" + (int)(i + 1) + ") : " + String.valueOf(Integer.toBinaryString(chrBuffer[i+1])));
            
            short value = (short) ((short)(Byte.toUnsignedInt((byte)chrBuffer[i]) << 8) | ((short) Byte.toUnsignedInt((byte)chrBuffer[i+1]) & 0xFF));
            // logger.debug("ReceiverArinst22 scanData strBuffer  value   : " + String.valueOf(Integer.toBinaryString(value)));
            // logger.debug("ReceiverArinst22 scanData strBuffer  value   : " + String.valueOf(value));
            
            short data = (short) (value & VALUE_MASK);
            // logger.debug("ReceiverArinst22 scanData strBuffer  value  VALUE_MASK : " + String.valueOf(Integer.toBinaryString(value)));

            // Integer index = ((value & INDEX_MASK) >> 11);
            // logger.debug("ReceiverArinst22 scanData strBuffer  value  INDEX_MASK : " + String.valueOf(index) + "     " + String.valueOf(Integer.toBinaryString(index)));

            double amplitude =  (double) (((10.0 * 80.0 - (double) (data)) / 10.0));
            long frequency = start + (i * step) / 2;  // result is returned not in a row
            
            if (frequency - stop  > 0) {
                break;
            }
            
            logger.debug("ReceiverArinst22 scanData amplitude : " + String.valueOf(String.format("%.5f", amplitude)));
            // logger.debug("ReceiverArinst22 scanData frequency : " + String.valueOf(frequency));
            // logger.debug("ReceiverArinst22 scanData stop      : " + String.valueOf(stop));
            
            Map<String, String> map = new HashMap<>();
            
            map.put("frequency", String.valueOf(frequency));
            map.put("amplitude", String.valueOf(amplitude));
            
            infoList.add(map);
        }       
        
        return infoList;
    }
}
