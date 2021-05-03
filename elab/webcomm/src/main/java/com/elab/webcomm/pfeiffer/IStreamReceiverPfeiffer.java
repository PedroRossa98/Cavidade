package com.elab.webcomm.pfeiffer;

import org.springframework.stereotype.Service;

@Service
public interface IStreamReceiverPfeiffer {

    public void init();
    
    public void execReadPressure();
    
    public void readPressure(String port, Integer addr);
    
    public void setPressureValue(Double value);
    
    public Double getPressureValue();
    
    public Boolean getReadingValue();
    
    public void processDeviceResponse(byte[] message);
    
}