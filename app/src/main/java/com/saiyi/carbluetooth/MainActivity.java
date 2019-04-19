package com.saiyi.carbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;

public class MainActivity extends AppCompatActivity {

    private static BluetoothAdapter mBluetoothAdapter;
    private static int count;//接收到设备的个数
    private static ArrayList<BluDevice> bluDeviceList;
    private static MyUIHandler mUIHandler;

    private static class MyUIHandler extends UIHandler<MainActivity> {
        MyUIHandler(MainActivity cls) {
            super(cls);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = ref.get();
            if (activity != null) {
                if (activity.isFinishing()) return;
                switch (msg.what) {
                    case 0:
                        Intent i = new Intent(activity, FoundDeviceActivity.class);
                        i.putExtra("data", bluDeviceList);
                        activity.startActivity(i);
                        activity.finish();
                        count=0;
                        break;
                    case 1:
                        //15秒种，未搜索到设备，刷新ui，提示用户重试
                        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        break;
                    case 2:
                        //8秒种，未搜索到设备，自动重试
                        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        activity.bindDevice();
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mUIHandler = new MyUIHandler(this);

        openService();

        bindDevice();
    }

    private void bindDevice() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mUIHandler.sendEmptyMessageDelayed(2, 8 * 1000);//8秒后自动重试
            mUIHandler.sendEmptyMessageDelayed(1, 15 * 1000);//15秒后提醒用户重试
        }
    }

    private void openService() {
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(this, BluetoothLeService.class);
        startService(serviceIntent);
    }

    static BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public synchronized void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            bluDeviceList = new ArrayList<>();
            BluDevice blu = new BluDevice();
            String name = device.getName();
            String address = device.getAddress();
            if (isEmpty(name)) return;
            if (isEmpty(address)) return;
            if (!name.startsWith("car_seat")) return;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            blu.setName(name);
            blu.setAddress(address);
            bluDeviceList.add(blu);
            count++;
            if(count==1){
                mUIHandler.sendEmptyMessageDelayed(0,1500);
            }else if(count==2){
                mUIHandler.removeMessages(0);
                mUIHandler.sendEmptyMessage(0);
            }
        }
    };

    @Override
    protected void onDestroy() {
        //关闭扫描
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter = null;
        }
        super.onDestroy();
    }
}
