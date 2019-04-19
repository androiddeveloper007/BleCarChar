package com.saiyi.carbluetooth;

public interface BleModel {
    void init();
    void connectDevice(String deviceAddress);
    void disconnectDevice(String deviceAddress);
    void finish();
    void setConnectStatus(boolean isConncected, String address);
    void writeData(byte[] data);
    void writeData(byte[] data, String address);
    void writeData(byte[] data, String address, boolean fromStartStop);
    void setView(DevicesView view);
    void releaseActivity();
}
