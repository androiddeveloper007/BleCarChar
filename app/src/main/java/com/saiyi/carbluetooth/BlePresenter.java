package com.saiyi.carbluetooth;

public interface BlePresenter {
    void connectDevice(String deviceAddress);
    void disconnectDevice(String deviceAddress);
    void init();
    void writeData(byte[] data);
    void writeData(byte[] data, String address);
    void writeData(byte[] data, String address, boolean fromStartStop);
    void finish();
    void setView(DevicesView view);
    void setModel(BleModel model);
    DevicesView getView();
}
