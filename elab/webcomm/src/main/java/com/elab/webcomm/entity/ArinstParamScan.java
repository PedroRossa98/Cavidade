package com.elab.webcomm.entity;

public class ArinstParamScan {

    String port;
    Long   start;
    Long   stop;
    Long   step;
    
    
    public String getPort() {
        return port;
    }
    
    public void setPort(String port) {
        this.port = port;
    }
    
    public Long getStart() {
        return start;
    }
    
    public void setStart(Long start) {
        this.start = start;
    }
    
    public Long getStop() {
        return stop;
    }
    
    public void setStop(Long stop) {
        this.stop = stop;
    }
    
    public Long getStep() {
        return step;
    }
    
    public void setStep(Long step) {
        this.step = step;
    }
    
}
