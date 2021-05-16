package com.elab.webcomm.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elab.webcomm.arinst.IStreamReceiverArinst;
import com.elab.webcomm.arinst.ReceiverArinst22;
import com.elab.webcomm.entity.ArinstParamScan;
import com.elab.webcomm.gpio.RPiGPIO;
import com.elab.webcomm.pfeiffer.IStreamReceiverPfeiffer;
import com.elab.webcomm.pfeiffer.ReceiverPfeifferPressure;
import com.elab.webcomm.usb.UsbDataProvider;
import com.elab.webcomm.utils.CSVWriterUtils;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;



// https://www.youtube.com/watch?v=6Oo-9Can3H8 - Threads
// https://www.youtube.com/watch?v=NEZ2ASoP_nY
// https://developers.exlibrisgroup.com/blog/calling-apis-in-parallel-with-java-code/


@RestController
@PropertySource("classpath:upload.properties")
public class SerialController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private static final int      RETRY_TIMES         = 3;
    
    private static final String   WIN_ARINST_PORT     = "COM9";
    private static final String   WIN_PFEIFFER_PORT   = "COM10";
    private static final String   RPI_ARINST_PORT     = "/dev/ttyACM0";
    private static final String   RPI_PFEIFFER_PORT   = "/dev/ttyUSB0";    
    
    Boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    
    
    @Value("${upload.path}")
    private String EXTERNAL_FILE_PATH;
    
    @Autowired
    private IStreamReceiverArinst streamReceiverArinst;
    
    @Autowired
    private IStreamReceiverPfeiffer streamReceiverPfeiffer;
    
    @Autowired
    private UsbDataProvider usbDataProvider;
    
    // https://fazecast.github.io/jSerialComm/
    // https://github.com/westside/spring-boot-arduino/blob/master/src/main/java/com/bhaptics/demo/SerialExample.java
    // https://github.com/floringavrila/serialtools/tree/master/src/main/java/ro/paha/serialtools
    // https://stackoverflow.com/questions/4436733/how-to-write-java-code-that-return-a-line-of-string-into-a-string-variable-from
    
   
    @RequestMapping(value = "/port", method = RequestMethod.GET)
    public List<String> getComPortData(HttpServletRequest request,
                               HttpServletResponse response) {

        logger.debug("getComPortData : ");
        ArrayList<String> portList = usbDataProvider.getUsbDevices();
        List<String> commList = new ArrayList<>();
        
        for (String port : portList) {
            logger.debug("getComPortData port : " + String.valueOf(port));
            commList.add(String.valueOf(port));
        }
        
        return commList;
    }
    
    
    @RequestMapping(value = "/pressure", method = RequestMethod.GET)
    public String getDevicePressureData(HttpServletRequest request,
                                        HttpServletResponse response,
                                        @RequestParam(required = false, name="port") String port,
                                        @RequestParam(required = false, name="addr") Integer addr) {

        DecimalFormat df2 = new DecimalFormat("0.000");
        
        Double invalidPressure = -1.0;
        logger.debug("getDevicePressureData : ");
        
        if (port == null) {
            port = isWindows ? WIN_PFEIFFER_PORT : RPI_PFEIFFER_PORT;
        }
        
        if (addr == null) {
            addr = 1;
        }
        
        Boolean python = false;
        Boolean quick = true;
        
        try {
            if (python) {
                if (quick) {
                    Double pressureValue = ReceiverPfeifferPressure.execReadPressure();
                    return df2.format(pressureValue);
                } else {
                    streamReceiverPfeiffer.execReadPressure();
                    Thread.sleep(1000);
                }
            } else {
                if (quick) {
                    Double pressureValue = ReceiverPfeifferPressure.readPressure(port, addr);
                    return df2.format(pressureValue);
                } else {
                    streamReceiverPfeiffer.init();
                    streamReceiverPfeiffer.readPressure(port, addr);
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Double delta = 0.000001d;
        
        for (int i = 0; i < RETRY_TIMES; i++) {
            logger.debug("getDevicePressureData RETRY_TIMES : " + i);
            Double pressureValue = streamReceiverPfeiffer.getPressureValue();
            
            // logger.debug("SerialController getDevicePressureData pressureValue : " + String.valueOf(pressureValue));
            // logger.debug("SerialController getDevicePressureData invalidPressure : " + String.valueOf(invalidPressure));
            // logger.debug("SerialController getDevicePressureData compare : " + String.valueOf(pressureValue != invalidPressure));
            logger.debug("SerialController getDevicePressureData compare 2 : " + String.valueOf(Math.abs(pressureValue - invalidPressure) > delta));
            
            if (Math.abs(pressureValue - invalidPressure) > delta) {
                logger.debug("SerialController getDevicePressureData pressureValue : " + df2.format(pressureValue));
                return df2.format(pressureValue);
            }            
            
            try {
                // wait 1-5 sec. and try again
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.debug("SerialController getDevicePressureData pressureValue : " + df2.format(invalidPressure));
        
        return df2.format(invalidPressure);
    }

    
    @RequestMapping(value = "/pressuremock", method = RequestMethod.GET)
    public Double getMockDevicePressureData(HttpServletRequest request,
                                            HttpServletResponse response) {

        Double invalidPressure = -1.0;
        logger.debug("getMockDevicePressureData : ");
        streamReceiverPfeiffer.setPressureValue(invalidPressure);
        // streamReceiverPfeiffer.setPressureValue(345.3);
        
        for (int i = 0; i < RETRY_TIMES; i++) {
            Double pressureValue = streamReceiverPfeiffer.getPressureValue();
            
            if (pressureValue != invalidPressure) {
                return pressureValue;
            }
            
            try {
                // wait 1-5 sec. and try again
                Thread.sleep((new Random().nextInt(5) + 1) * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return streamReceiverPfeiffer.getPressureValue();
    }

    
    // http://localhost:8091/elab/arinst?port=COM9&start=3386000000&stop=3891000000&step=500000
    @RequestMapping(value = "/arinst", method = RequestMethod.GET)
    public List <Map<String, String>> getArinstData(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    @RequestParam(required = false, name="port") String port,
                                                    @RequestParam(required = true, name="start") String start,
                                                    @RequestParam(required = true, name="stop") String stop,
                                                    @RequestParam(required = true, name="step") String step) {
        
        
        if (port == null) {
            port = isWindows ? WIN_ARINST_PORT : RPI_ARINST_PORT;
        }
        
        Boolean mathlab = true;
        
        try {
            if (mathlab) {
                return ReceiverArinst22.startScan(port, Long.valueOf(start), Long.valueOf(stop), Long.valueOf(step));
            } else {
                streamReceiverArinst.init();
                Boolean scan = streamReceiverArinst.startScan(port, Long.valueOf(start), Long.valueOf(stop), Long.valueOf(step));
                
                if (!scan) {
                    return new ArrayList <Map<String, String>> ();
                }
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return new ArrayList <Map<String, String>> ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return new ArrayList <Map<String, String>> ();
        }
        
        for (int i = 0; i < RETRY_TIMES; i++) {
            
            try {
                // wait 1-5 sec. and try again
                Thread.sleep((new Random().nextInt(5) + 1) * 1000);
            } catch (Exception e) {
                // e.printStackTrace();
                return new ArrayList <Map<String, String>> ();
            }
            
            logger.debug("getArinstData retry : " + String.valueOf(i));
            
            if (streamReceiverArinst.isComplete()) {
                return streamReceiverArinst.getInfoList();
            }
        }
        
        return new ArrayList <Map<String, String>> ();
    }

    
    @RequestMapping(value = "/arinst/csv", method = RequestMethod.GET)
    public void getArinstCSV(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(required = false, name="port") String port,
                             @RequestParam(required = true, name="start") String start,
                             @RequestParam(required = true, name="stop") String stop,
                             @RequestParam(required = true, name="step") String step) {
        
        
        if (port == null) {
            port = isWindows ? WIN_ARINST_PORT : RPI_ARINST_PORT;
        }
        
        streamReceiverArinst.init();
        
        try {
            Boolean scan = streamReceiverArinst.startScan(port, Long.valueOf(start), Long.valueOf(stop), Long.valueOf(step));
            
            if (!scan) {
                return;
            }
            
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return;
        }
        
        for (int i = 0; i < RETRY_TIMES; i++) {
            
            try {
                // wait 1-5 sec. and try again
                Thread.sleep((new Random().nextInt(5) + 1) * 1000);
            } catch (Exception e) {
                // e.printStackTrace();
                return;
            }
            
            // logger.debug("getArinstData retry : " + String.valueOf(i));
            
            if (streamReceiverArinst.isComplete()) {
                List<Map<String, String>> data = streamReceiverArinst.getInfoList();
                
                String strData = toCSV(data);
                // logger.debug("getArinstData strData : " + String.valueOf(strData));
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
                Date date = new Date();
                String downloadName = "arinst" + "_" + sdf.format(date) + ".csv";
                
                String csvFileName = EXTERNAL_FILE_PATH + downloadName;
                
                Writer file = null;
                
                try {
                    file = new OutputStreamWriter(new FileOutputStream(csvFileName), StandardCharsets.UTF_8);
                    
                    file.write(strData);
                    
                    file.flush();
                    file.close();
                    
                    downloadFile(response, downloadName, downloadName);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        

    }

    
    // http://localhost:8091/elab/arinst
    //    {
    //        "port": "COM9",
    //        "start": "3386000000",
    //        "stop": "3891000000",
    //        "step": "500000"
    //    }
    @RequestMapping(value = "/arinst", method = RequestMethod.POST)
    public List <Map<String, String>> getArinstDataByPost(HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          @RequestBody ArinstParamScan arinstParamScan) {
        
        
        if (arinstParamScan.getPort() == null) {
            arinstParamScan.setPort(isWindows ? WIN_ARINST_PORT : RPI_ARINST_PORT);
        }
        
        streamReceiverArinst.init();
        
        try {
            Boolean scan = streamReceiverArinst.startScan(arinstParamScan.getPort(), 
                    arinstParamScan.getStart(), arinstParamScan.getStop(), arinstParamScan.getStep());
            
            if (!scan) {
                return new ArrayList <Map<String, String>> ();
            }
            
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return new ArrayList <Map<String, String>> ();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            return new ArrayList <Map<String, String>> ();
        }
        
        for (int i = 0; i < RETRY_TIMES; i++) {
            
            try {
                // wait 1-5 sec. and try again
                Thread.sleep((new Random().nextInt(5) + 1) * 1000);
            } catch (Exception e) {
                // e.printStackTrace();
                return new ArrayList <Map<String, String>> ();
            }
            
            logger.debug("getArinstData retry : " + String.valueOf(i));
            
            if (streamReceiverArinst.isComplete()) {
                return streamReceiverArinst.getInfoList();
            }
        }
        
        return new ArrayList <Map<String, String>> ();
    }

 
    
    // https://pi4j.com/1.2/example/control.html
    @RequestMapping(method = RequestMethod.GET, value = "/gpio")
    public void setGPIO(HttpServletRequest request,
                        HttpServletResponse response) throws InterruptedException {
        
         RPiGPIO.GPIOTest();
    }

    
    @RequestMapping(method = RequestMethod.GET, value = "/arinst/data")
    public void getArinstScanData(HttpServletRequest request,
                                  HttpServletResponse response) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        Date date = new Date();
        String downloadName = "arinst" + "_" + sdf.format(date) + ".csv";
 
        // logger.debug("getArinstScanCSV downloadName : " + String.valueOf(downloadName));

        // logger.debug("getArinstScanCSV InfoList : " + String.valueOf(streamReceiverArinst.getInfoList()) + "  " + String.valueOf(streamReceiverArinst.isComplete()));

        String csvFileName = EXTERNAL_FILE_PATH + downloadName;
        
        Writer file = null;
        
        List <Map<String, String>> infoList = new ArrayList <Map<String, String>> ();
        
        if (streamReceiverArinst.isComplete()) {
            infoList = streamReceiverArinst.getInfoList();
        } else {
            return;
        }

        try {
            file = new OutputStreamWriter(new FileOutputStream(csvFileName), StandardCharsets.UTF_8);
            
            CSVWriterUtils.writeLine(file, Arrays.asList("frequency", "amplitude"), ';');

            for (Map<String, String> info : infoList) {
                String frequency = info.get("frequency");
                String amplitude = info.get("amplitude");
                
                List<String> list = new ArrayList<>();
                list.add(frequency);
                list.add(amplitude.replace(".", ","));
                
                CSVWriterUtils.writeLine(file, list, ';');
            }
            
            file.flush();
            file.close();
            
            downloadFile(response, downloadName, downloadName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    @RequestMapping(method = RequestMethod.GET, value = "/arinst/list")
    public ArrayList<String> getArinstScanList(HttpServletRequest request,
                                 HttpServletResponse response) {
        // Creates an array in which we will store the names of files and directories
        String[] pathnames;
        ArrayList<String> fileName = new ArrayList<String>();
        // Creates a new File instance by converting the given pathname string
        // into an abstract pathname
        File f = new File(EXTERNAL_FILE_PATH);

        // Populates the array with names of files and directories
        pathnames = f.list();

        // For each pathname in the pathnames array
        for (String pathname : pathnames) {
            // Print the names of files and directories
            System.out.println(pathname);
            fileName.add(pathname);
        }
        
        return fileName;
    }
    
    
    @RequestMapping(method = RequestMethod.GET, value = "/arinst/csv/{filename}")
    public void getArinstScanCSVFile(HttpServletRequest request,
                                     HttpServletResponse response,
                                     @PathVariable("filename") String filename) {
        
        String downloadName = filename;
 
        logger.debug("getArinstScanCSV downloadName : " + String.valueOf(downloadName));

        try {
            
            downloadFile(response, downloadName, downloadName);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
    private void downloadFile(HttpServletResponse response, 
                              String fileName, String downloadName) throws IOException {

        File file = new File(EXTERNAL_FILE_PATH + fileName);
        
        if (file.exists()) {
            if (downloadName.equals("")) {
                downloadName = file.getName();
            }
            
            //get the mimetype
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());
            if (mimeType == null) {
                //unknown mimetype so set the mimetype to application/octet-stream
                mimeType = "application/octet-stream";
                // mimeType = "application/vnd.ms-excel";
            }
            
            response.setContentType(mimeType);
            
            /**
            * In a regular HTTP response, the Content-Disposition response header is a
            * header indicating if the content is expected to be displayed inline in the
            * browser, that is, as a Web page or as part of a Web page, or as an
            * attachment, that is downloaded and saved locally.
            * 
            */
            
            /**
            * Here we have mentioned it to show inline
            */
            response.setHeader("Content-Disposition", String.format("inline; filename=\"" + downloadName + "\""));
            
            // Here we have mentioned it to show as attachment
            // response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + downloadName + "\""));
            
            response.setContentLength((int) file.length());
            
            InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            
            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }
    
    
    private String toCSV(List<Map<String, String>> list) {
        List<String> headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().collect(Collectors.toList());
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(headers.get(i));
            sb.append(i == headers.size()-1 ? "\n" : ",");
        }
        for (Map<String, String> map : list) {
            for (int i = 0; i < headers.size(); i++) {
                sb.append(map.get(headers.get(i)));
                sb.append(i == headers.size()-1 ? "\n" : ",");
            }
        }
        return sb.toString();
    }
}
