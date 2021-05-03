package com.elab.webcomm.arinst;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.elab.webcomm.protocol.SerialProtocol;
import com.elab.webcomm.usb.UsbDataProvider;
import com.fazecast.jSerialComm.SerialPort;

@Service
public class IStreamReceiverArinstImpl implements IStreamReceiverArinst {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private static final String commandPattern = "scn %1$d %2$d %3$d %4$d %5$d \r\n";
    
    private static final int BASE_ATTENUATION_CALCULATION_LEVEL     = 100;      // for avoid negative values (100 = 0 dB, 120 = 20 dB, 70 = -30 dB)
    private static final int ATTENUATION_ACCURACY_COEFFICIENT       = 100;      // two decimal places

    private static final int BASE_AMPLITUDE_CALCULATION_LEVEL       = 80;       // for avoid negative values
                                                                                // amplitudeIntValue = 18600 => amplitude = ((80 * 10.0 - 18659)) / 10.0 = -108.59 dB

    private static final double AMPLITUDE_ACCURACY_COEFFICIENT      = 10.0;     // one decimal place

    private static final long intermediateFrequency                 = 500000L;
    
    private UsbDataProvider usbDataProvider;
    
    private List <Map<String, String>> infoList = new ArrayList <Map<String, String>> ();
    
    private boolean complete = false;
    
    private long start;
    private long stop;
    private long step;
    private long attenuation;
    
    private int pointIndex;
    private int pointShift;
    private int lastPointId;
    
    
    @Override
    public void init() {
        complete = false;
        infoList = new ArrayList <Map<String, String>> ();
        
        usbDataProvider = new UsbDataProvider();
        usbDataProvider.initStreamReceiverArinst(this);
    }
    
    
    @Override
    public Boolean startScan(String port, Long start, Long stop, Long step) throws IOException {
        usbDataProvider.disconnect();

        SerialPort serialPort = usbDataProvider.connect(port, SerialProtocol.RS232);
        
        if (serialPort != null) {
            this.start = start; // 2080000000L;   // 2080 MHz
            this.stop  = stop;  // 2180000000L;   // 2180 MHz
            this.step  = step;  //     500000L;   //  500 KHz
            attenuation =   0;  //    0 dB
    
            long _attenuation = (BASE_ATTENUATION_CALCULATION_LEVEL * ATTENUATION_ACCURACY_COEFFICIENT) + (attenuation * ATTENUATION_ACCURACY_COEFFICIENT);
    
            lastPointId = 0;
            pointShift = 0;
    
            String command = String.format(
                commandPattern,
                start,
                stop,
                step,
                _attenuation,
                0               // command id (can be a random integer value)
            );
    
            usbDataProvider.write(command.getBytes());
            return true;
        }
        
        return false;
    }

    
    @Override
    public void processDeviceResponse(byte[] message) {
        int messageLength = message.length;
        
        if (messageLength == 2 || messageLength == 6) {
            processMessage(message);
        } else {
            String readMessage = new String(message, 0, message.length);
            processMessage(readMessage);
        }
    }

    private void processMessage(byte[] message) {
        
        int messageLength = message.length;
        if (messageLength == 6) {
            pointIndex= message[0] << 24 | message[1] << 16 | message[2] << 8 | message[3];
            // pointIndex is calculated, but sometimes send to avoid possible data loss errors
        }

        int pointId           =  (message[messageLength - 2] & 0x000000FF) >> 3;                                                // 1111 1000 0000 0000
        int amplitudeIntValue = ((message[messageLength - 2] & 0x00000007) << 8) | (message[messageLength - 1] & 0x000000FF);   // 0000 0111 1111 1111

        long frequency = start + (pointShift * step) + ((intermediateFrequency * 2) * pointIndex);  // result is returned not in a row

        // logger.debug("messageLength : " + String.valueOf(messageLength));
        // logger.debug("pointId : " + String.valueOf(pointId));
        // logger.debug("amplitudeIntValue : " + String.valueOf(amplitudeIntValue));
        // logger.debug("frequency : " + String.valueOf(frequency));       
        
        // 100000000000000000000000     result return step = intermediate frequency
        // 100000001000000000000000
        // 100000001000000010000000
        // new level
        // 110000001000000010000000
        // 110000001100000010000000
        // 110000001100000011000000
        // new level
        // 111000001100000011000000
        // etc

        double amplitude = (((BASE_AMPLITUDE_CALCULATION_LEVEL * AMPLITUDE_ACCURACY_COEFFICIENT) - amplitudeIntValue) / AMPLITUDE_ACCURACY_COEFFICIENT) - attenuation;
        // amplitudeIntValue = 18600 => amplitude = ((80 * 10.0 - 18659)) / 10.0 = -108.59 dB

        // logger.debug("amplitude : " + String.valueOf(amplitude));
        // logger.debug("=======================================");
        
        if (lastPointId < pointId || pointId == 0) {
            receiveStreamData(frequency, amplitude);

            lastPointId = pointId;
            pointIndex++;
        }
    }

    private void processMessage(String readMessage) {
        if (readMessage.contentEquals("l")) {
            pointShift++;
            pointIndex = 0;
        } else if (readMessage.contentEquals("complete")) {
            complete = true;
            onCommandComplete();
        }
    }


    private void receiveStreamData(long frequency, double amplitude) {
        // logger.debug("Frequency : " + String.valueOf(frequency) + "    " + "Amplitude : " + String.valueOf(amplitude));
        Map<String, String> map = new HashMap<>();
        map.put("frequency", String.valueOf(frequency));
        map.put("amplitude", String.valueOf(amplitude));
        infoList.add(map);
    }

    
    private void onCommandComplete() {
        if (usbDataProvider != null) {
            usbDataProvider.disconnect();
        }
    }
    
    
    public Boolean isComplete() {
        return complete;
    }
    
    
    public List <Map<String, String>> getInfoList() {
        if (complete) {
            return infoList;
        }
        
        return new ArrayList <Map<String, String>> (); 
    }
    
}
