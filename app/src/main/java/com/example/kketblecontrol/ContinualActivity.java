package com.example.kketblecontrol;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileOutputStream;

public class ContinualActivity extends AppCompatActivity {

    Button btnStart, btnEnd;
    EditText timeInterval, valueInterval, startLevel, endLevel;
    TextView stat_ch01, stat_ch02, stat_ch03, stat_ch04, stat_level, stat_time;

    private int[] chValues = new int[4];
    private String send_packet = "0211FF0000000003";

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private ContinualThread continualThread;
    public static Context mContext;

    FileOutputStream fileOutputStream;

    Handler handler = new Handler();

//    private final ServiceConnection mServiceConnection = new ServiceConnection() {
//
//        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder service) {
//            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            mBluetoothLeService = null;
//        }
//    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ContinualThread.isEnd = true;
        Intent intent = new Intent(ContinualActivity.this, ControlActivity.class); //지금 액티비티에서 다른 액티비티로 이동하는 인텐트 설정
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //인텐트 플래그 설정
        startActivity(intent);  //인텐트 이동
        finish();   //현재 액티비티 종료
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continual);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Continual Mode");
        actionBar.setDisplayHomeAsUpEnabled(true);

        btnStart = (Button) findViewById(R.id.btn_start);
        btnEnd = (Button) findViewById(R.id.btn_end);
        timeInterval = (EditText) findViewById(R.id.time_interval);
        valueInterval = (EditText) findViewById(R.id.value_interval);
        startLevel = (EditText) findViewById(R.id.level_start);
        endLevel = (EditText) findViewById(R.id.level_end);

        stat_ch01 = (TextView) findViewById(R.id.stat_ch01);
        stat_ch02 = (TextView) findViewById(R.id.stat_ch02);
        stat_ch03 = (TextView) findViewById(R.id.stat_ch03);
        stat_ch04 = (TextView) findViewById(R.id.stat_ch04);
        stat_level = (TextView) findViewById(R.id.stat_level);
        stat_time = (TextView) findViewById(R.id.stat_time);

        final Intent intent = getIntent();
        mBluetoothLeService = (BluetoothLeService) intent.getSerializableExtra("mBluetoothLeService");
        mNotifyCharacteristic = (BluetoothGattCharacteristic) intent.getParcelableExtra("mNotifyCharacteristic");

        mContext = this;

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int timeInt = 2000;
                int valInt = 16;
                int startLvl, endLvl;
                try {
                    timeInt = Integer.parseInt(String.valueOf(timeInterval.getText()));
                    valInt = Integer.parseInt(String.valueOf(valueInterval.getText()));
                    startLvl = Integer.parseInt(String.valueOf(startLevel.getText()));
                    endLvl = Integer.parseInt(String.valueOf(endLevel.getText()));
                } catch(NumberFormatException e) {
                    startLvl = 0;
                    endLvl = 0;
                }
//                try {
//                    fileOutputStream = mContext.openFileOutput("KKET_data.txt", Context.MODE_PRIVATE);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                continualThread = new ContinualThread(timeInt, valInt, startLvl, endLvl);
                continualThread.start();
            }
        });

        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContinualThread.isEnd = true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContinualThread.isEnd = true;
    }
}
