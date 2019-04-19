package com.saiyi.carbluetooth;

public class LinkState {
    private static LinkState instance;

    public static LinkState getInstance() {
        if (instance == null) {
            instance = new LinkState();
        }
        return instance;
    }

    private static boolean state;

    public boolean getState() {
        return state;
    }

    public void setState(boolean b) {
        state = b;
    }
}
