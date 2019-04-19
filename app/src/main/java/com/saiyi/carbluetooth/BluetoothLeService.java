package com.saiyi.carbluetooth;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_ERR_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_ERR_DISCONNECTED";
    public final static String ACTION_SCAN_TIMEOUT = "com.example.bluetooth.le.ACTION_GATT_SCAN_TIMEOUT";
    public final static String ACTION_SCAN_NEW_DEVICE = "com.example.bluetooth.le.ACTION_GATT_SCAN_NEW_DEVICE";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
/*	public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID
			.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);*/

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.

    public static BluetoothLeService ME = null;
    public boolean isScanning = false;
    public static final int AUTO_STOP_SCAN_MSG_WHAT = 0X15011501;
    private Object newScanCallback = null;
    protected HashSet<String> scanDeviceMacAddress = new HashSet<>();
    static final int SCAN_BLE_TIME = 15 * 1000;

    @Override
    public void onCreate() {
        ME = this;
        initialize();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        ME = null;
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    public synchronized void scan(boolean flag) {
        if (flag == isScanning) {
            return;
        }

        isScanning = flag;

        if (flag) {
            scanDeviceMacAddress.clear();

            Message msg = DelayHandler.getInstance().obtainMessage();
            msg.what = AUTO_STOP_SCAN_MSG_WHAT;
            msg.obj = new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    stopScanBLEDevice();
                    broadcastUpdate(ACTION_SCAN_TIMEOUT, "");
                }
            };
            DelayHandler.getInstance().sendMessageDelayed(msg, SCAN_BLE_TIME);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (newScanCallback == null) {
                    newScanCallback = new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
//                            LoggerUtils.i("android5.0及以上搜索结果" + result.toString());

                            handleSearchResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                        }

                        @Override
                        public void onBatchScanResults(List<ScanResult> results) {
//                            LoggerUtils.i("外围设备->onBatchScanResults:" + results.toString());
                        }

                        @Override
                        public void onScanFailed(int errorCode) {
//                            LoggerUtils.i("外围设备->onScanFailed:" + errorCode);
                        }
                    };
                }

                List<ScanFilter> list = new ArrayList<>();
                ScanFilter.Builder builder = new ScanFilter.Builder();
                builder.setServiceUuid(ParcelUuid.fromString("00000001-0000-1000-8000-00805f9b34fb"));
                list.add(builder.build());

                ScanSettings.Builder builderSS = new ScanSettings.Builder();
                builderSS.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                if (mBluetoothAdapter != null && newScanCallback != null) {
                    try {
                        mBluetoothAdapter.getBluetoothLeScanner().startScan(null, builderSS.build(), (ScanCallback) newScanCallback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (mBluetoothAdapter != null && mLeScanCallback != null) {
                    try {
                        mBluetoothAdapter.startLeScan(mLeScanCallback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

//            LoggerUtils.i("扫描设备...");
        } else {
            DelayHandler.getInstance().removeMessages(AUTO_STOP_SCAN_MSG_WHAT);
            stopScanBLEDevice();
//            LoggerUtils.i("停止扫描...");
        }

    }

    @SuppressWarnings("deprecation")
    private void stopScanBLEDevice() {
//        LoggerUtils.i("停止扫描------------------------------------");
        if (mBluetoothAdapter == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (newScanCallback != null) {
                try {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan((ScanCallback) newScanCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (mLeScanCallback != null) {
                try {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            LoggerUtils.i("android5.0以下搜索结果:" + device.getName()+" mac:"+device.getAddress());
            handleSearchResult(device, rssi, scanRecord);

        }
    };

    private void handleSearchResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        String deviceName = device.getName() == null ? "" : device.getName();
        if (!scanDeviceMacAddress.contains(device.getAddress())) {
            scanDeviceMacAddress.add(device.getAddress());
            broadcastUpdate(ACTION_SCAN_NEW_DEVICE, device.getAddress());
        }
    }

    private void searchService(final BluetoothGatt gatt) {
        new Thread() {
            public void run() {
                gatt.discoverServices();
            }
        }.start();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;

                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction, gatt.getDevice().getAddress());
//                LoggerUtils.i("Connected to GATT server.");
                // Attempts to discover services after successful connection.
//				LoggerUtils.i( "Attempting to start service discovery:"
//						+ mBluetoothGatt.discoverServices());

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                searchService(gatt);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
//                LoggerUtils.i("Disconnected from GATT server.");
                broadcastUpdate(intentAction, gatt.getDevice().getAddress());
                gatt.close();
                mBluetoothGatt = null;

                scanDeviceMacAddress.remove(gatt.getDevice().getAddress());
            } else {
                broadcastUpdate(ACTION_GATT_ERR_DISCONNECTED, gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            LoggerUtils.i("onServicesDiscovered received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
//            LoggerUtils.i("onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {

//            LoggerUtils.i("onDescriptorWriteonDescriptorWrite = " + status
//                    + ", descriptor =" + descriptor.getUuid().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, gatt.getDevice().getAddress());
            if (characteristic.getValue() != null) {
                //String test = characteristic.getStringValue(0);
//                LoggerUtils.i(characteristic.getStringValue(0));
            }
//            LoggerUtils.i("--------onCharacteristicChanged-----");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            LoggerUtils.i("rssi = " + rssi);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
//            LoggerUtils.i("--------write success----- status:" + status);

        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String address) {
//        LoggerUtils.i("-----------------广播action:" + action);
        final Intent intent = new Intent(action);
        intent.putExtra("address", address);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(
                    data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));
            //String test = stringBuilder.toString();
//            LoggerUtils.i("ppp" + new String(data) + "\n"
//                    + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        }
        //}
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic, String address) {
        final Intent intent = new Intent(action);

        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(
                    data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));
            //String test = stringBuilder.toString();
//            LoggerUtils.i("ppp" + new String(data) + "\n"
//                    + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        }
        //}
        intent.putExtra("address", address);
        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
//                LoggerUtils.i( "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
//            LoggerUtils.i( "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
//            LoggerUtils.i(
//                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
//            LoggerUtils.i( "Device not found.  Unable to connect.");
            return false;
        }
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
//            LoggerUtils.i("===================已连接上");
            broadcastUpdate(ACTION_GATT_CONNECTED, address);
            return true;
        }

        // Previously connected device. Try to reconnect
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
//            LoggerUtils.i(
//                    "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }


        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
//		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        device.connectGatt(this, false, mGattCallback);
//        LoggerUtils.i( "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            LoggerUtils.i( "BluetoothAdapter not initialized");
            return;
        }
        int connectionState = mBluetoothManager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
//            LoggerUtils.i("===================已断开");
            broadcastUpdate(ACTION_GATT_DISCONNECTED, mBluetoothGatt.getDevice().getAddress());
            return;
        }
        if (connectionState == BluetoothProfile.STATE_DISCONNECTING) {
//            LoggerUtils.i("===================正在断开");
            return;
        }
        refreshGatt(mBluetoothGatt);
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
//        LoggerUtils.i("gatt--close");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private void refreshGatt(BluetoothGatt gatt) {
        try {
            Method method = gatt.getClass().getDeclaredMethod("refresh");
            method.invoke(gatt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean wirteCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null || characteristic == null) {
//            LoggerUtils.i( "BluetoothAdapter not initialized");
            return false;
        }

        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            LoggerUtils.i( "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            LoggerUtils.i( "BluetoothAdapter not initialized");
            return;
        }
        if (enabled == true) {
//            LoggerUtils.i("Enable Notification");
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
            descriptor
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } else {
//            LoggerUtils.i("Disable Notification");
            mBluetoothGatt.setCharacteristicNotification(characteristic, false);
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
            descriptor
                    .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Read the RSSI for a connected remote device.
     */
    public boolean getRssiVal() {
        if (mBluetoothGatt == null)
            return false;

        return mBluetoothGatt.readRemoteRssi();
    }


    public int getConnectionState() {
        return mConnectionState;
    }
}
