package com.saiyi.carbluetooth;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.text.TextUtils.isEmpty;

/**
 * Created by YGP on 2017/12/20.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleModelImpl implements BleModel {
    private DevicesView view;
    private Activity activity;
    private WeakReference<Activity> wr;
    private static BluetoothGattCharacteristic gattCharacteristic_write = null;
    private static volatile BleModelImpl bleModel;
    private String targetDeviceMac = "";

    private BleModelImpl() {

    }

    public static BleModelImpl getInstance() {
        return synchrorizeProduce();
    }

    private static BleModelImpl synchrorizeProduce() {
        if (bleModel == null) {
            synchronized (BleModelImpl.class) {
                if (bleModel == null) {
                    bleModel = new BleModelImpl();
                }
            }
        }
        return bleModel;
    }

    public void setView(DevicesView view) {
        this.view = view;
        if (view instanceof Activity) {
            wr = new WeakReference<>((Activity) view);
            activity = (Activity) view;
        } else if (view instanceof Fragment) {
            try {
                wr = new WeakReference<Activity>(((Fragment) view).getActivity());
                activity = ((Fragment) view).getActivity();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (wr != null && wr.get() != null)
            wr.get().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        else
            activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

//    private BleModelImpl(DevicesView view) {
//        this.view = view;
//        if (view instanceof Activity) {
//            activity = (Activity) view;
//        } else if (view instanceof Fragment) {
//            try {
//                activity = ((Fragment) view).getActivity();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        activity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//    }

    /*
    // Code to manage Service lifecycle.
    public final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.

            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    */

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;
            final String address = intent.getStringExtra("address");
            //    private boolean mScanning;
            // 10秒后停止查找搜索.
            //    private static final long SCAN_PERIOD = 15000;
            //    private static final int SCAN_DEVICES = 1;
            //    private static final int CONTROL_DEVICES = 2;
            //    private static final int CONTROL_RES = 3;
            //    private BluetoothLeService mBluetoothLeService;
            boolean mConnected = false;
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
//                ToastUtils.makeText("连接成功！");
                setConnectStatus(mConnected, address);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                setConnectStatus(mConnected, address);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(BluetoothLeService.ME.getSupportedGattServices(), address);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null && ConvertUtils.checkXORData(data)) {
                    view.showData(data, address);
                }
            } else if (BluetoothLeService.ACTION_SCAN_TIMEOUT.equals(action)) {
//                LoggerUtils.i("=========================扫描超时======");
                view.showConnectDialog(false);
            } else if (BluetoothLeService.ACTION_GATT_ERR_DISCONNECTED.equals(action)) {
//                LoggerUtils.i("=========================断开异常======");
                view.showConnectDialog(false);
            } else if (BluetoothLeService.ACTION_SCAN_NEW_DEVICE.equals(action)) {
                final String deviceMac = intent.getStringExtra("address");
                LoggerUtils.i("=========================找到新设备======：" + deviceMac);
                if (deviceMac.equals(targetDeviceMac)) {
                    LoggerUtils.i("=========================哈哈找到目标设备了======");
                    Util.otherOperationNeedStopScan();
                    Util.threadSleep(120);
                    if (!Util.checkBLEServiceIsNull()) {
                        DelayHandler.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothLeService.ME.connect(deviceMac);
                            }
                        });
                    }
                }
            }
        }
    };

    /**
     * 蓝牙连接状态过滤器
     *
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_SCAN_NEW_DEVICE);
        intentFilter.addAction(BluetoothLeService.ACTION_SCAN_TIMEOUT);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_ERR_DISCONNECTED);
        return intentFilter;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices, String address) {
        if (gattServices == null) {
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            if (gattService.getUuid().toString().contains("6e400001")) {
//                Log.e(TAG, "获取服务uuid======>" + gattService.getUuid().toString());
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                        .getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                    List<BluetoothGattDescriptor> descriptors = gattCharacteristic
                            .getDescriptors();
                    for (BluetoothGattDescriptor bluetoothGattDescriptor : descriptors) {
                        bluetoothGattDescriptor.toString();
                    }
                    //通知
                    String UUID_CHAR2 = "6e400003-b5a3-f393-e0a9-e50e24dcca9a";
                    //向设备发送数据
                    String UUID_CHAR3 = "6e400002-b5a3-f393-e0a9-e50e24dcca9a";
                    if (gattCharacteristic.getUuid().toString()
                            .equals(UUID_CHAR2)) {
                        BluetoothLeService.ME.setCharacteristicNotification(
                                gattCharacteristic, true);
                    } else if (gattCharacteristic.getUuid().toString()
                            .equals(UUID_CHAR3)) {
                        gattCharacteristic_write = gattCharacteristic;
                    }
                }
            }
        }
    }


    @Override
    public void init() {
        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (activity == null) return;
        if (!(activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))) {
            ToastUtils.makeText(activity, R.string.ble_not_supported);
            finish();
        }
        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) (activity.getSystemService(Context.BLUETOOTH_SERVICE));
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            ToastUtils.makeText(activity,R.string.error_bluetooth_not_supported);
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {// 读取蓝牙状态
            mBluetoothAdapter.enable();
        }
    }

    @Override
    public void connectDevice(String deviceAddress) {
        if (isEmpty(deviceAddress)) return;
        targetDeviceMac = deviceAddress;
        if (!Util.checkBLEServiceIsNull()) {
            BluetoothLeService.ME.scan(true);
        }
        view.showConnectDialog(true);
    }

    @Override
    public void disconnectDevice(String deviceAddress) {
        if (!Util.checkBLEServiceIsNull()) {
            BluetoothLeService.ME.disconnect();
        }
        view.showConnectDialog(false);
    }

    @Override
    public void finish() {
        if (wr != null) {// && mGattUpdateReceiver != null
            try {
                wr.get().unregisterReceiver(mGattUpdateReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                activity.unregisterReceiver(mGattUpdateReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setConnectStatus(boolean isConncected, String address) {
        if (view != null)
            view.setConnectStatus(isConncected, address);
    }

    @Override
    public void writeData(byte[] data) {
        if (gattCharacteristic_write != null) {
            gattCharacteristic_write
                    .setValue(data);
        }
        if (!Util.checkBLEServiceIsNull()) {
            boolean result = BluetoothLeService.ME.wirteCharacteristic(gattCharacteristic_write);
        }
    }

    @Override
    public void writeData(byte[] data, String address) {
        if (gattCharacteristic_write != null) {
            gattCharacteristic_write
                    .setValue(data);
        }
        if (!Util.checkBLEServiceIsNull()) {
            boolean result = BluetoothLeService.ME.wirteCharacteristic(gattCharacteristic_write);
            if (!result) {
                ToastUtils.makeText(activity,R.string.failure_setup);
            } else {
                view.setupSuccess();
            }
        }
    }

    @Override
    public void writeData(byte[] data, String address, boolean fromStartStop) {
        if (gattCharacteristic_write != null) {
            gattCharacteristic_write
                    .setValue(data);
        }
        if (!Util.checkBLEServiceIsNull()) {
            boolean result = BluetoothLeService.ME.wirteCharacteristic(gattCharacteristic_write);
            if (!result) {
                ToastUtils.makeText(activity,R.string.failure_setup);
            } else {
                view.setupSuccess(fromStartStop);
            }
        }
    }

    @Override
    public void releaseActivity() {
        try {
            if (wr != null) {// && mGattUpdateReceiver!=null
                try {
                    wr.get().unregisterReceiver(mGattUpdateReceiver);
                    wr = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    activity.unregisterReceiver(mGattUpdateReceiver);
                    activity = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (view != null) view = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
