package com.example.kketblecontrol;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.IntStream;

public class ContinualThread extends Thread{

    private int[] chValues = new int[4];
    private String send_packet = "0211FF0000000003";
    private String kket_send_packet = "010200000000";
    //private int setStartLevel = 3500;

    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private int timeInterval, valueInterval, startLvl, endLvl;

    Handler handler = new Handler();

    File saveFile;
    Context context;
    FileOutputStream fileOutputStream;
    BufferedWriter buf;

    public static boolean isEnd = false;

    public ContinualThread(int timeInterval, int valueInterval, int startLvl, int endLvl) {
        this.mBluetoothLeService = ((ControlActivity) ControlActivity.mContext).mBluetoothLeService;
        this.mNotifyCharacteristic = ((ControlActivity) ControlActivity.mContext).mNotifyCharacteristic;
        try {
            this.fileOutputStream = ((MainActivity) MainActivity.mainContext).openFileOutput("KKET_data.txt", Context.MODE_PRIVATE);
            //this.fileOutputStream = ((ContinualActivity) ContinualActivity.mContext).openFileOutput("KKET_data.txt", Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.timeInterval = timeInterval;
        this.valueInterval = valueInterval;
        this.startLvl = startLvl;
        this.endLvl = endLvl;

        this.fileOutputStream = fileOutputStream;

        isEnd = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void run() {
        mBluetoothLeService.setCharacteristicNotification(
                mNotifyCharacteristic, true);

        /////////////////////// 파일 쓰기 ///////////////////////
//// 파일 생성
//        saveFile = new File(context.getFilesDir(), "kketCon_data"); // 저장 경로
//// 폴더 생성
//        if(!saveFile.exists()){ // 폴더 없을 경우
//            saveFile.mkdir(); // 폴더 생성
//        }
//
//        try {
//            buf = new BufferedWriter(new FileWriter(saveFile+"/CarnumData.txt", true));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //sendToTest();
        sendToKKET();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendToTest() {
        int cnt = 0;

        for(int i = 0; i <= 256; i += valueInterval) {
            if(i >= 256) i = 255;
            for(int j = 0; j <= 256; j += valueInterval) {
                if(j >= 256) j = 255;
                for(int n = 0; n <= 256; n += valueInterval) {
                    if(n >= 256) n = 255;
                    for(int m = 0; m <= 256; m += valueInterval) {
                        if(m >= 256) m = 255;
                        chValues[0] = i;
                        chValues[1] = j;
                        chValues[2] = n;
                        chValues[3] = m;

                        int zeroCnt = 0;
                        for(int tmp : chValues) if(tmp == 0) zeroCnt++;
                        if((IntStream.of(chValues).sum() > 240) && (zeroCnt != 3)) continue;

                        send_packet = send_packet.substring(0, 6) + String.format("%02x", chValues[0]) + String.format("%02x", chValues[1]) + String.format("%02x", chValues[2]) + String.format("%02x", chValues[3]) + send_packet.substring(14);

                        byte[] data = hexStringToByteArray(send_packet);
                        mBluetoothLeService.write(mNotifyCharacteristic, data);

                        //Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                        Log.d(null, send_packet);
                        cnt++;
                        Log.d(null, String.valueOf(cnt));
                        Log.d(null, String.valueOf(IntStream.of(chValues).sum()));

                        try {
                            Thread.sleep(timeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(isEnd) return;
                    }
                }
            }
        }

        send_packet = "0211FF0000000003";

        byte[] data = hexStringToByteArray(send_packet);
        mBluetoothLeService.write(mNotifyCharacteristic, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void sendToKKET() {
        int sumOverCnt = 0;
        int cnt = 0;
        for(int i = 0; i <= 256; i += valueInterval) {
            if (i >= 256) i = 255;
            for (int j = 0; j <= 256; j += valueInterval) {
                if (j >= 256) j = 255;
                for (int n = 0; n <= 256; n += valueInterval) {
                    if (n >= 256) n = 255;
                    for (int m = 0; m <= 256; m += valueInterval) {
                        if (m >= 256) m = 255;
                        chValues[0] = i;
                        chValues[1] = j;
                        chValues[2] = n;
                        chValues[3] = m;

                        int zeroCnt = 0;
                        for (int tmp : chValues) if (tmp == 0) zeroCnt++;
                        if ((IntStream.of(chValues).sum() > 240) && (zeroCnt != 3)){
                            sumOverCnt++;
                            continue;
                        }
                    }
                }
            }
        }

        cnt = 0;
        int totalLevelCnt = (int) (Math.pow(((256 / valueInterval) + 1), 4) - sumOverCnt) - 1;
        int totalMinute = (timeInterval * totalLevelCnt / 1000) / 60;

        long timeStart = System.currentTimeMillis();

        done : for(int i = 0; i <= 256; i += valueInterval) {
            if(i >= 256) i = 255;
            for(int j = 0; j <= 256; j += valueInterval) {
                if(j >= 256) j = 255;
                for(int n = 0; n <= 256; n += valueInterval) {
                    if(n >= 256) n = 255;
                    for(int m = 0; m <= 256; m += valueInterval) {
                        if(m >= 256) m = 255;
                        chValues[0] = i;
                        chValues[1] = j;
                        chValues[2] = n;
                        chValues[3] = m;

                        long timeNow = System.currentTimeMillis();

                        int zeroCnt = 0;
                        for(int tmp : chValues) if(tmp == 0) zeroCnt++;
                        if(zeroCnt == 4) continue;
                        if((IntStream.of(chValues).sum() > 240) && (zeroCnt != 3)) {
                            Log.d(null, "over : " + String.valueOf(IntStream.of(chValues).sum()));
                            continue;
                        }

                        cnt++;
                        if(startLvl != 0 && endLvl != 0) {
                            if (cnt < startLvl) continue;
                            if (cnt > endLvl) break done;
                        }
                        //if(chValues[3] != 0) continue;

                        kket_send_packet = kket_send_packet.substring(0, 4) + String.format("%02x", chValues[0]) + String.format("%02x", chValues[1]) + String.format("%02x", chValues[2]) + String.format("%02x", chValues[3]);

                        byte[] data = hexStringToByteArray(kket_send_packet);
                        mBluetoothLeService.write(mNotifyCharacteristic, data);

                        long now = System.currentTimeMillis();
                        Date mDate = new Date(now);

                        SimpleDateFormat sdfNow = new SimpleDateFormat("HH:mm:ss");
                        String time = sdfNow.format(mDate);

                        //Toast.makeText(getApplicationContext(), send_packet, Toast.LENGTH_SHORT).show();
                        Log.d(null, "spend time : " + String.valueOf((timeNow - timeStart) / 1000));
                        Log.d(null, "local time : " + time);
                        Log.d(null, kket_send_packet);
                        Log.d(null, "level : " + String.valueOf(cnt));
                        Log.d(null, "sum : " + String.valueOf(IntStream.of(chValues).sum()));

                        final String currentCh01 = String.valueOf(chValues[0]);
                        final String currentCh02 = String.valueOf(chValues[1]);
                        final String currentCh03 = String.valueOf(chValues[2]);
                        final String currentCh04 = String.valueOf(chValues[3]);
                        final String currentLevel = String.valueOf(cnt);
                        final String totalTime = String.valueOf(totalMinute);
                        final String spenTime = String.valueOf((timeNow - timeStart) / 1000 / 60);
                        final String totalLevel = String.valueOf(totalLevelCnt);
                        final String localTime = time;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((ContinualActivity) ContinualActivity.mContext).stat_ch01.setText(currentCh01);
                                ((ContinualActivity) ContinualActivity.mContext).stat_ch02.setText(currentCh02);
                                ((ContinualActivity) ContinualActivity.mContext).stat_ch03.setText(currentCh03);
                                ((ContinualActivity) ContinualActivity.mContext).stat_ch04.setText(currentCh04);
                                ((ContinualActivity) ContinualActivity.mContext).stat_time.setText("TIME(m) : " + spenTime + " / " + totalTime + " | LocalTime : " + localTime);
                                ((ContinualActivity) ContinualActivity.mContext).stat_level.setText("LEVEL : " + currentLevel + " / " + totalLevel);
                            }
                        });

//                        try {
//                            //BufferedWriter buf = new BufferedWriter(new FileWriter(saveFile+"/CarnumData.txt", true));
//                            buf.append(localTime + ","); // 날짜 쓰기
//                            buf.append(spenTime + ","); // 파일 쓰기
//                            buf.append(currentLevel + ",");
//                            buf.append(currentCh01 + ",");
//                            buf.append(currentCh02 + ",");
//                            buf.append(currentCh03 + ",");
//                            buf.append(currentCh04 + ",");
//                            buf.newLine(); // 개행
//                            //buf.close();
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }

                        try {
                            String seperatorData = ",";
                            String seperatorLine = "\n";
                            fileOutputStream.write(localTime.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(currentLevel.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(String.valueOf((timeNow - timeStart) / 1000).getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(currentCh01.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(currentCh02.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(currentCh03.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorData.getBytes(StandardCharsets.UTF_8));
                            fileOutputStream.write(currentCh04.getBytes(StandardCharsets.UTF_8)); // 날짜 쓰기
                            fileOutputStream.write(seperatorLine.getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        try {
                            Thread.sleep(timeInterval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(isEnd) return;
                    }
                }
            }
        }

        try {
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        kket_send_packet = "010200000000";

        byte[] data = hexStringToByteArray(kket_send_packet);
        mBluetoothLeService.write(mNotifyCharacteristic, data);
    }

    private byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }
}
