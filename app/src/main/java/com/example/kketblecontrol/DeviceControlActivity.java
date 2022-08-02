package com.example.kketblecontrol;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 특정 BLE 기기의 경우 이 활동은 연결, 데이터 표시, 데이터 표시, 사용자 인터페이스를 제공합니다.
 * 및 장치에서 지원하는 GATT 서비스 및 특성을 표시합니다. 활동
 * 는 {@codeBluatusLeService}과(와) 통신하며, 이는 차례로 다음 항목과 상호 작용합니다.
 * 블루투스 LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Button btnOn;
    private Button btnOff;
    private ImageView imgConnect;
    private TextView textConnect;
    private SeekBar[] seekBars;
    private TextView[] textNum;
    private Button[] btnSet;

    // 서비스 라이프사이클을 관리하는 코드입니다.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((com.example.kketblecontrol.BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // 시동 초기화에 성공하면 자동으로 장치에 연결합니다.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    //서비스에서 발생한 다양한 이벤트를 처리합니다.
    //작업_GATT_CONNECTED: GATT 서버에 연결되었습니다.
    //작업_GATT_DISCONNECT: GATT 서버에서 연결이 끊어졌습니다.
    //작업_GATT_SERVICES_DISCOVERED: GATT 서비스를 검색했습니다.
    //조치_DATA_ABLE: 디바이스에서 데이터를 수신했습니다. 읽기 또는 알림 작업의 결과일 수 있습니다.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState("connected");
                invalidateOptionsMenu();
            } else if (com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState("disconnected");
                invalidateOptionsMenu();
                clearUI();
            } else if (com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (com.example.kketblecontrol.BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(com.example.kketblecontrol.BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    // 지정된 GATT 특성을 선택한 경우 지원되는 기능을 확인합니다. 이 샘플
    // '읽기' 및 '알림' 기능을 시연합니다. 보시오
    // 완료를 위한 http://d.android.com/reference/android/bluetooth/BluetoothGatt.html
    // 지원되는 특성 기능 목록입니다.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // UI 참조를 설정합니다.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        mDataField = (TextView) findViewById(R.id.data_value);
//
//        btnOn = (Button)findViewById(R.id.btn_on);
//        btnOff = (Button)findViewById(R.id.btn_off);
//        imgConnect = (ImageView)findViewById(R.id.img_connect);
//        textConnect = (TextView)findViewById(R.id.text_connect);

        seekBars = new SeekBar[4];
        seekBars[0] = (SeekBar) findViewById(R.id.seekBar);
        seekBars[1] = (SeekBar) findViewById(R.id.seekBar2);
        seekBars[2] = (SeekBar) findViewById(R.id.seekBar3);
        seekBars[3] = (SeekBar) findViewById(R.id.seekBar4);

//        textNum = new TextView[4];
//        textNum[0] = (TextView) findViewById(R.id.text_num1);
//        textNum[1] = (TextView) findViewById(R.id.text_num2);
//        textNum[2] = (TextView) findViewById(R.id.text_num3);
//        textNum[3] = (TextView) findViewById(R.id.text_num4);
//
//        btnSet = new Button[4];
//        btnSet[0] = (Button)findViewById(R.id.btn_set1);
//        btnSet[1] = (Button)findViewById(R.id.btn_set2);
//        btnSet[2] = (Button)findViewById(R.id.btn_set3);
//        btnSet[3] = (Button)findViewById(R.id.btn_set4);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, com.example.kketblecontrol.BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /* 점등 버튼 이벤트 */
        btnOn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View v) {
                if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                    Toast.makeText(v.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String on_packet = "0211FFFFFFFFFF03";
                byte[] data = hexStringToByteArray(on_packet);
                //  byte[] data = {(byte)0x02, (byte)0x11, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03};
                mBluetoothLeService.write(mNotifyCharacteristic, data);
                Toast.makeText(v.getContext(), "조명을 점등하였습니다.", Toast.LENGTH_SHORT).show();

                for(int i = 0; i < seekBars.length; i++){
                    int index = i;
                    seekBars[index].setProgress(255);
                    textNum[index].setText("255");
                }
            }
        });

        /* 소등 버튼 이벤트 */
        btnOff.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View v) {
                if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                    Toast.makeText(v.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String off_packet = "0211FF0000000003";
                byte[] data = hexStringToByteArray(off_packet);
                mBluetoothLeService.write(mNotifyCharacteristic, data);
                Toast.makeText(v.getContext(), "조명을 소등하였습니다.", Toast.LENGTH_SHORT).show();

                for(int i = 0; i < seekBars.length; i++){
                    int index = i;
                    seekBars[index].setProgress(0);
                    textNum[index].setText("000");
                }
            }
        });

        /* 시크바 이벤트 */
        for (int i = 0; i < seekBars.length; i++) {
            int index = i;
            seekBars[index].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textNum[index].setText(String.format("%03d", progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }

        for (int i = 0; i < seekBars.length; i++) {
            int index = i;
            btnSet[index].setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void onClick(View v) {
                    if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                        Toast.makeText(v.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String ch_packet = "0211FF";
                    for(int j = 0; j < textNum.length; j++){
                        int jndex = j;
                        String power = (String) textNum[jndex].getText();
                        String hex = String.format("%02X", Integer.parseInt(power));
                        ch_packet +=hex;
                    }
                    ch_packet +="03";
                    byte[] data = hexStringToByteArray(ch_packet);
                    mBluetoothLeService.write(mNotifyCharacteristic, data);
                    Toast.makeText(v.getContext(), "채널별 제어.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /* String -> 16진수 Byte */
    private byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.gatt_services, menu);
//        if (mConnected) {
//            menu.findItem(R.id.menu_connect).setVisible(false);
//            menu.findItem(R.id.menu_disconnect).setVisible(true);
//        } else {
//            menu.findItem(R.id.menu_connect).setVisible(true);
//            menu.findItem(R.id.menu_disconnect).setVisible(false);
//        }
//        return true;
//    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_connect:
//                mBluetoothLeService.connect(mDeviceAddress);
//                return true;
//            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

//    private void updateConnectionState(String state) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if(state.equals("connected")){
//                    imgConnect.setImageResource(R.drawable.on);
//                }else if(state.equals("disconnected")){
//                    imgConnect.setImageResource(R.drawable.fail);
//                }
//                textConnect.setText("Bluetooth "+state);
//            }
//        });
//    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    // 지원되는 GATT 서비스/특성을 통해 반복하는 방법을 시연합니다.
    // 이 샘플에서는 확장 가능한 목록 보기에 바인딩된 데이터 구조를 채웁니다.
    // UI에서.
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // 사용 가능한 GATT 서비스를 루프합니다.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, com.example.kketblecontrol.SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            // 사용 가능한 특성을 루프합니다.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, com.example.kketblecontrol.SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(com.example.kketblecontrol.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(com.example.kketblecontrol.BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }




}
