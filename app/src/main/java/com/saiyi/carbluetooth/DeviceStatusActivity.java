package com.saiyi.carbluetooth;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 就是提示连接成功的结果，目前没有处理连接失败的状态。
 */
public class DeviceStatusActivity extends AppCompatActivity {

    @BindView(R.id.LinearLayout_addFalse)
    LinearLayout LinearLayout_addFalse;
    @BindView(R.id.LinearLayout_addSuccess)
    LinearLayout LinearLayout_addSuccess;
    @BindView(R.id.TextView_definite)
    TextView TextView_definite;
    @BindView(R.id.TextView_enter)
    TextView TextView_enter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);
        ButterKnife.bind(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            boolean status = bundle.getBoolean("status");
            if (status) {
                LinearLayout_addFalse.setVisibility(View.GONE);
                LinearLayout_addSuccess.setVisibility(View.VISIBLE);
                TextView_definite.setVisibility(View.GONE);
                TextView_enter.setVisibility(View.VISIBLE);
            }
        }
    }

    @OnClick({R.id.ImageButton_back, R.id.TextView_definite, R.id.TextView_enter})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ImageButton_back:
                onBackPressed();
                break;
            case R.id.TextView_definite:
                onBackPressed();
                break;
            case R.id.TextView_enter:
                Intent intent = new Intent(this, MainActivity.class);
                if (getIntent().getExtras() != null)
                    intent.putExtras(getIntent().getExtras());
                startActivity(intent);
                finish();
                break;
        }
    }
}
