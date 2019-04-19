package com.saiyi.carbluetooth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 设备列表，选择设备后，点击连接，连接蓝牙
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class FoundDeviceActivity extends AppCompatActivity implements DevicesView {

    @BindView(R.id.RecyclerView_device)
    RecyclerView RecyclerView_device;
    private ArrayList<BluDevice> list;
    private BleModel bleModel;
    private int count = 0;
    private Bundle bundle;
    private BlePresenter blePresenter;
    private static MyUIHandler mUIHandler;
    private int selectedPosition = 0;

    private static class MyUIHandler extends UIHandler<FoundDeviceActivity> {
        MyUIHandler(FoundDeviceActivity cls) {
            super(cls);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FoundDeviceActivity activity = ref.get();
            if (activity != null) {
                if (activity.isFinishing()) return;
                switch (msg.what) {
                    case 0:
                        //12秒后，未搜索到设备，刷新ui，提示用户重试
                        if (!LinkState.getInstance().getState()) {
                            Toast.makeText(activity, "无法连接设备，请检查设备是否被占用，或者重启设备。", Toast.LENGTH_SHORT).show();
                            BluetoothLeService.ME.close();
                        }
                        break;
                    case 1:
                        //6秒后，未搜索到设备，重试
                        if (!LinkState.getInstance().getState()) {
                            BluetoothLeService.ME.close();
                            activity.bleModel.connectDevice(activity.list.get(0).getAddress());
                        }
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_device);
        ButterKnife.bind(this);
        mUIHandler = new MyUIHandler(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //获取getIntent中列表，展示在此页面中
        if (getIntent() != null && getIntent().hasExtra("data")) {
            Intent i = getIntent();
            list = (ArrayList<BluDevice>) i.getSerializableExtra("data");
            if (list != null && list.size() > 0) {
                List<Device> deviceList = new ArrayList<>();
                for (BluDevice d : list) {
                    deviceList.add(new Device(d.getName(), false));
                }
                LinearLayoutManager manager = new LinearLayoutManager(this);
                manager.setOrientation(LinearLayoutManager.VERTICAL);
                RecyclerView_device.setLayoutManager(manager);
                DeviceAdapter deviceAdapter = new DeviceAdapter(this, deviceList);
                deviceAdapter.setOnRecyclerViewItemClickListener(new BaseQuickAdapter.OnRecyclerViewItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        view.setSelected(true);
                        selectedPosition = position;
                    }
                });
                RecyclerView_device.setAdapter(deviceAdapter);
                deviceAdapter.setNewData(deviceList);
            }
        }
        bleModel = BleModelImpl.getInstance();
        bleModel.setView(this);
        blePresenter = BlePresenterImpl.getInstance();
        blePresenter.setView(this);
        blePresenter.setModel(bleModel);
        blePresenter.init();
    }

    //点击链接时，正式连接蓝牙
    @OnClick({R.id.ImageButton_back, R.id.TextView_enter})
    public void onViewClicked(View v) {
        switch (v.getId()) {
            case R.id.ImageButton_back:
                onBackPressed();
                break;
            case R.id.TextView_enter:
                //连接蓝牙并传值给下一个页面
                bleModel.connectDevice(list.get(0).getAddress());
                mUIHandler.sendEmptyMessageDelayed(1, 6 * 1000);//6秒后提醒用户重试
                mUIHandler.sendEmptyMessageDelayed(0, 12 * 1000);//12秒后提醒用户重试
                break;
        }
    }

    @Override
    public void showData(String data, String address) {
        count++;//接收次数
        //连接成功，在此回调中处理进入下一个页面的逻辑,这里会得到多个回调值，所以要分别处理
        if (bundle == null) bundle = new Bundle();
        if (count == 1) {
            //将设备名称缓存到sp
            SharePerferenceHelper sp = SharePerferenceHelper.createSharePerference(this, "private");
            sp.putString("deviceName", list.get(selectedPosition).getName());
            bundle.putBoolean("status", true);
            bundle.putString("data", data);
            bundle.putString("address", address);
            sp.putString("address", address);
        } else if (count == 2) {
            //第二次
            bundle.putString("data1", data);
        } else if (count == 3) {
            bundle.putString("data2", data);
        } else {
            LinkState.getInstance().setState(true);
            bundle.putString("data3", data);
            Intent intent = new Intent(this, DeviceStatusActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
            bleModel.finish();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (bleModel != null) {
            bleModel.releaseActivity();
            bleModel.finish();//解决报广播接收器未注销的error
            bleModel = null;
        }
        if (blePresenter != null) {
            blePresenter.setView(null);
            blePresenter.setModel(null);
            blePresenter = null;
        }
        super.onDestroy();
    }

    @Override
    public void setConnectStatus(boolean isConncected, String address) {

    }

    @Override
    public void setupSuccess() {

    }

    @Override
    public void setupSuccess(boolean fromStartStop) {

    }

    @Override
    public void showConnectDialog(boolean isConnect) {
//        toast(isConnect?"连接上了":"未连接");
    }

    @Override
    public void showSuccess() {

    }

    @Override
    public void showEmpty() {

    }

    @Override
    public void showError() {

    }

    @Override
    public void after() {

    }

    @Override
    public void showData(String data) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //释放资源，在返回搜索页时，避免搜索不到设备
        BluetoothLeService.ME.close();
        bleModel.releaseActivity();
        bleModel.finish();
    }
}
