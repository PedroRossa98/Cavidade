package com.elab.webcomm.arinst;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public interface IStreamReceiverArinst {

    public void init();
    
    public Boolean startScan(String port, Long start, Long stop, Long step) throws IOException;
    
    public void processDeviceResponse(byte[] message);
    
    public Boolean isComplete();
    
    public List <Map<String, String>> getInfoList();

}