package com.example.kketblecontrol;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_SERVICE_UUID = "SERVICE_UUID";
    public static final String EXTRAS_CHAR_UUID = "CHAR_UUID";

    TextView deviceNameView, deviceAddrView, serviceView, characterView;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(InfoActivity.this, ControlActivity.class); //지금 액티비티에서 다른 액티비티로 이동하는 인텐트 설정
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //인텐트 플래그 설정
        startActivity(intent);  //인텐트 이동
        finish();   //현재 액티비티 종료
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Connected Device Information");
        actionBar.setDisplayHomeAsUpEnabled(true);

        deviceNameView = (TextView) findViewById(R.id.device_name);
        deviceAddrView = (TextView) findViewById(R.id.device_address);
        serviceView = (TextView) findViewById(R.id.service_uuid);
        characterView = (TextView) findViewById(R.id.characteristic_uuid);

        final Intent intent = getIntent();
        String mService = intent.getStringExtra(EXTRAS_SERVICE_UUID);
        String mCharacterisitc = intent.getStringExtra(EXTRAS_CHAR_UUID);
        deviceNameView.setText(Device.mDeviceName);
        deviceAddrView.setText(Device.mDeviceAddress);
        serviceView.setText(mService);
        characterView.setText(mCharacterisitc);
    }
}
