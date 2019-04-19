package com.saiyi.carbluetooth;

public interface DevicesView extends BaseView {
    void showData(String data);
    void showData(String data, String address);
    void setConnectStatus(boolean isConncected, String address);
    void setupSuccess();
    void setupSuccess(boolean fromStartStop);
    void showConnectDialog(boolean isConnect);
}