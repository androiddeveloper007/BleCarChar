package com.saiyi.carbluetooth;

public class BlePresenterImpl implements BlePresenter {
    private DevicesView view;
    private BleModel model;
    private static volatile BlePresenterImpl blePresenter;
    private BlePresenterImpl() {

    }

    public static BlePresenterImpl getInstance() {
        return synchrorizeProduce();
    }

    private static BlePresenterImpl synchrorizeProduce() {
        if (blePresenter == null) {
            synchronized(BlePresenterImpl.class) {
                if (blePresenter == null) {
                    blePresenter =  new BlePresenterImpl();
                }
            }
        }

        return blePresenter;
    }

    public void setView(DevicesView view) {
        this.view = view;
    }

    public DevicesView getView() {
        return view;
    }

    public void setModel(BleModel model) {
        this.model = model;
    }

    private BlePresenterImpl(DevicesView view, BleModel model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void connectDevice(String deviceAddress) {
        model.connectDevice(deviceAddress);
    }

    @Override
    public void disconnectDevice(String deviceAddress) {
        model.disconnectDevice(deviceAddress);
    }

    @Override
    public void init() {
        model.init();
    }

    @Override
    public void writeData(byte[] data) {
        model.writeData(data);
    }

    @Override
    public void writeData(byte[] data, String address) {
        model.writeData(data,address);
    }

    @Override
    public void writeData(byte[] data, String address, boolean fromStartStop) {
        model.writeData(data,address,fromStartStop);
    }

    @Override
    public void finish() {
        model.finish();
    }
}
