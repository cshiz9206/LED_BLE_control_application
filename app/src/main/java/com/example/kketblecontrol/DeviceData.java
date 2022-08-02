package com.example.kketblecontrol;

public class DeviceData {
    private String deviceName;
    private String deviceAddr;

    public DeviceData(String deviceName, String deviceAddr){
        this.deviceName = deviceName;
        this.deviceAddr = deviceAddr;
    }

    public String getAddr()
    {
        return this.deviceAddr;
    }

    public String getName()
    {
        return this.deviceName;
    }
}