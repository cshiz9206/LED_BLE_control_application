package com.example.kketblecontrol;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ControlActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_SERVICE_UUID = "SERVICE_UUID";
    public static final String EXTRAS_CHAR_UUID = "CHAR_UUID";
    private final static String TAG = ControlActivity.class.getSimpleName();

    //private String send_packet_on = "0211FFFFFFFFFF03";
    //private String send_packet_off = "0211FF0000000003";
    private String send_packet_off = "010200000000";
    private String send_packet_on = "010216161616";
    private String send_packet = "010200000000";
    //private String send_packet = "0211FF0000000003";

    //service
    protected BluetoothLeService mBluetoothLeService;
    private ExpandableListView mGattServicesList;
    private TextView mDataField;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final String SERVICE_UUID = "0000ff00-0000-1000-8000-00805f9b34fb";
    private final String READ_WRITE_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
    private final String KKET_WRITE_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB";
    private final String CHARAC_UUID = KKET_WRITE_UUID;

    //connect
    private boolean mConnected = false;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    //action
    private Button btnOn;
    private Button btnOff;
    private Button btnSet;
    private SeekBar ch_1, ch_2, ch_3, ch_4;
    private TextView ch1_val, ch2_val, ch3_val, ch4_val;
    private EditText ch1_edit, ch2_edit, ch3_edit, ch4_edit;
    protected BluetoothGattCharacteristic mNotifyCharacteristic;

    //comm activity
    public static Context mContext;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // 시동 초기화에 성공하면 자동으로 장치에 연결합니다.
            mBluetoothLeService.connect(Device.mDeviceAddress);
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
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState("connected");
                Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_LONG).show();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState("disconnected");
                Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show();
                invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }
//            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
//            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(Device.mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ControlActivity.this, MainActivity.class); //지금 액티비티에서 다른 액티비티로 이동하는 인텐트 설정
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //인텐트 플래그 설정
        startActivity(intent);  //인텐트 이동
        finish();   //현재 액티비티 종료
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_info:
                final Intent intentInfo = new Intent(this, InfoActivity.class);
                intentInfo.putExtra(ControlActivity.EXTRAS_DEVICE_NAME, Device.mDeviceName);
                intentInfo.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS, Device.mDeviceAddress);
                intentInfo.putExtra(ControlActivity.EXTRAS_SERVICE_UUID, SERVICE_UUID);
                intentInfo.putExtra(ControlActivity.EXTRAS_CHAR_UUID, CHARAC_UUID);
                startActivity(intentInfo);
                return true;
            case R.id.menu_continual:
                for(ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics) {
                    for(BluetoothGattCharacteristic characteristic : service) {
                        if(characteristic.getUuid().equals(UUID.fromString(CHARAC_UUID))){
                            mNotifyCharacteristic = characteristic;
                        }
                    }
                }
                final Intent intent = new Intent(this, ContinualActivity.class);
//                intent.putExtra("mBluetoothLeService", mBluetoothLeService);
//                intent.putExtra("mNotifyCharacteristic", mNotifyCharacteristic);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("KketBLEControl");
        actionBar.setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        if(intent.getStringExtra(EXTRAS_DEVICE_NAME) != null) {
            Device.mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            Device.mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //interface
        btnOn = (Button)findViewById(R.id.btn_on);
        btnOff = (Button)findViewById(R.id.btn_off);
        btnSet = (Button)findViewById(R.id.btn_set);
        ch_1 = (SeekBar)findViewById(R.id.seekBar);
        ch_2 = (SeekBar)findViewById(R.id.seekBar2);
        ch_3 = (SeekBar)findViewById(R.id.seekBar3);
        ch_4 = (SeekBar)findViewById(R.id.seekBar4);;
        ch1_val = (TextView) findViewById(R.id.ch01_val);
        ch2_val = (TextView) findViewById(R.id.ch02_val);
        ch3_val = (TextView) findViewById(R.id.ch03_val);
        ch4_val = (TextView) findViewById(R.id.ch04_val);
        ch1_edit = (EditText) findViewById(R.id.ch01_edit);
        ch2_edit = (EditText) findViewById(R.id.ch02_edit);
        ch3_edit = (EditText) findViewById(R.id.ch03_edit);
        ch4_edit = (EditText) findViewById(R.id.ch04_edit);
        SeekBar[] seekBars = {ch_1, ch_2, ch_3, ch_4};
        TextView[] textNum = {ch1_val, ch2_val, ch3_val, ch4_val};
        EditText[] editTexts = {ch1_edit, ch2_edit, ch3_edit, ch4_edit};

        //comm activity
        mContext = this;


//        //Service List
//        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
//        //mDataField = (TextView) findViewById(R.id.data_value);

        /* 점등 버튼 이벤트 */
        btnOn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onClick(View v) {
                for(ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics) {
                    for(BluetoothGattCharacteristic characteristic : service) {
                        if(characteristic.getUuid().equals(UUID.fromString(CHARAC_UUID))){
                            mNotifyCharacteristic = characteristic;
                        }
                    }
                }
                mBluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, true);
                if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                    Toast.makeText(v.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                //send_packet_on = "0211FFFFFFFFFF03";
                byte[] data = hexStringToByteArray(send_packet_on);
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
                for(ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics) {
                    for(BluetoothGattCharacteristic characteristic : service) {
                        if(characteristic.getUuid().equals(UUID.fromString(CHARAC_UUID))){
                            byte[] tmp2 = characteristic.getValue();
                            List<BluetoothGattDescriptor> tmp3 = characteristic.getDescriptors();
                            mNotifyCharacteristic = characteristic;
                        }
                    }
                }
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                    Toast.makeText(v.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                //send_packet_off = "0211FF0000000003";
                byte[] data = hexStringToByteArray(send_packet_off);
                mBluetoothLeService.write(mNotifyCharacteristic, data);
                Toast.makeText(v.getContext(), "조명을 소등하였습니다.", Toast.LENGTH_SHORT).show();

                for(int i = 0; i < seekBars.length; i++){
                    int index = i;
                    seekBars[index].setProgress(0);
                    textNum[index].setText("0");
                }
            }
        });

        /* set 버튼 이벤트 */
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics) {
                    for(BluetoothGattCharacteristic characteristic : service) {
                        if(characteristic.getUuid().equals(UUID.fromString(CHARAC_UUID))){
                            mNotifyCharacteristic = characteristic;
                        }
                    }
                }
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                if(mBluetoothLeService==null || mNotifyCharacteristic==null){
                    Toast.makeText(view.getContext(), "Characteristic을 선택해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] data = hexStringToByteArray(send_packet);
                mBluetoothLeService.write(mNotifyCharacteristic, data);
                Toast.makeText(view.getContext(), "세팅 완료 (packet : " + send_packet, Toast.LENGTH_SHORT).show();
            }
        });

        /* seekbar 이벤트 */
        for(int i = 0; i < seekBars.length; i++){
            int index = i;
            seekBars[index].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int val, boolean b) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    textNum[index].setText(String.valueOf(seekBar.getProgress()));
                    editTexts[index].setText(String.valueOf(seekBar.getProgress()));
                    int tmp[] = {seekBars[0].getProgress(), seekBars[1].getProgress(), seekBars[2].getProgress(), seekBars[3].getProgress()};
                    int zeroCnt = 0;
                    for(int tmpProgress : tmp) if(tmpProgress == 0) zeroCnt++;
//                    if((IntStream.of(tmp).sum() > 240) && (zeroCnt != 3)) {
//                        Toast.makeText(getApplicationContext(), "총합 240 초과", Toast.LENGTH_SHORT).show();
//                    }
//                    else {
                        send_packet = send_packet.substring(0, 4 + (index * 2)) + String.format("%02x", seekBar.getProgress()) + send_packet.substring(6 + (index * 2));
                        //send_packet = send_packet.substring(0, 6 + (index * 2)) + String.format("%02x", seekBar.getProgress()) + send_packet.substring(8 + (index * 2));

//                    }
                }
            });
        }

        editTexts[0].addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")) {
                    return;
                }
                else {
                    textNum[0].setText(String.valueOf(Integer.parseInt(String.valueOf(s))));
                    seekBars[0].setProgress(Integer.parseInt(String.valueOf(s)));
                    send_packet = send_packet.substring(0, 4 + (0 * 2)) + String.format("%02x", Integer.parseInt(String.valueOf(s))) + send_packet.substring(6 + (0 * 2));
                    Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        editTexts[1].addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")) {
                    return;
                }
                else {
                    textNum[1].setText(String.valueOf(Integer.parseInt(String.valueOf(editTexts[1].getText()))));
                    seekBars[1].setProgress(Integer.parseInt(String.valueOf(editTexts[1].getText())));
                    send_packet = send_packet.substring(0, 4 + (1 * 2)) + String.format("%02x", Integer.parseInt(String.valueOf(editTexts[1].getText()))) + send_packet.substring(6 + (1 * 2));
                    Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        editTexts[2].addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")) {
                    return;
                }
                else {
                    textNum[2].setText(String.valueOf(Integer.parseInt(String.valueOf(editTexts[2].getText()))));
                    seekBars[2].setProgress(Integer.parseInt(String.valueOf(editTexts[2].getText())));
                    send_packet = send_packet.substring(0, 4 + (2 * 2)) + String.format("%02x", Integer.parseInt(String.valueOf(editTexts[2].getText()))) + send_packet.substring(6 + (2 * 2));
                    Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        editTexts[3].addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")) {
                    return;
                }
                else {
                    textNum[3].setText(String.valueOf(Integer.parseInt(String.valueOf(editTexts[3].getText()))));
                    seekBars[3].setProgress(Integer.parseInt(String.valueOf(editTexts[3].getText())));
                    send_packet = send_packet.substring(0, 4 + (3 * 2)) + String.format("%02x", Integer.parseInt(String.valueOf(editTexts[3].getText()))) + send_packet.substring(6 + (3 * 2));
                    Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                }
            }
        });
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

//    private final ExpandableListView.OnChildClickListener servicesListClickListner =
//            new ExpandableListView.OnChildClickListener() {
//                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
//                @Override
//                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
//                                            int childPosition, long id) {
//                    if (mGattCharacteristics != null) {
//                        final BluetoothGattCharacteristic characteristic =
//                                mGattCharacteristics.get(groupPosition).get(childPosition);
//                        final int charaProp = characteristic.getProperties();
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
//                        return true;
//                    }
//                    return false;
//                }
//            };
//
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
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
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
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

//        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
//                this,
//                gattServiceData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[]{LIST_NAME, LIST_UUID},
//                new int[]{android.R.id.text1, android.R.id.text2},
//                gattCharacteristicData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[]{LIST_NAME, LIST_UUID},
//                new int[]{android.R.id.text1, android.R.id.text2}
//        );
//        mGattServicesList.setAdapter(gattServiceAdapter);
    }
}
