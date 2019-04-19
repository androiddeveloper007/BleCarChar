package com.saiyi.carbluetooth;

import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

public class DeviceAdapter extends BaseQuickAdapter<Device> {

    public DeviceAdapter(Context cxt, @Nullable List<Device> data) {
        super(cxt);
    }

    @Override
    protected int setItemLayout() {
        return R.layout.item_device;
    }

    @Override
    protected void convert(BaseViewHolder helper, Device item, int position) {
        helper.setText(R.id.TextView_device, item.getName());
    }

}
