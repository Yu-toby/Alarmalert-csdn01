package com.example.dell.alarmalert_csdn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

//import static com.example.dell.alarmalert_csdn.MainActivity.notice;

public class AlarmAlert extends Activity {
    private MediaPlayer mediaPlayer;
    private boolean state = false;
    int count = 0,checkleave = 0;


    /**Beacon*/
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning = false;
    ArrayList<ScannedData> findDevice = new ArrayList<>();
    private String mDeviceAddress = "54:6C:0E:9B:5C:79";

    Leave notice = new Leave();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**Beacon*/

        /**權限相關認證*/
        checkPermission();
        /**初始藍牙掃描及掃描開關之相關功能*/
        bluetoothScan();

//        Alert();
    }

    public static class Leave {

        Boolean isleave;

        public void Isleave (Boolean leave){
            this.isleave = leave;
//            Log.v("wyc","Isleave : " + isleave);
        }

        public Boolean getLeave(){
//            Log.v("wyc","getleave : " + isleave);
            return isleave;
        }

    }

    /**Beacon*/

    /**權限相關認證*/
    private void checkPermission() {
        /**確認手機版本是否在API18以上，否則退出程式*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            /**確認是否已開啟取得手機位置功能以及權限*/
            int hasGone = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasGone != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            /**確認手機是否支援藍牙BLE*/
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this,"Not support Bluetooth", Toast.LENGTH_SHORT).show();
                finish();
            }
            /**開啟藍芽適配器*/
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
            }
        }else finish();
    }

    /**初始藍牙掃描及掃描開關之相關功能*/
    private void bluetoothScan() {
        /**啟用藍牙適配器*/
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        /**開始掃描*/
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        isScanning = true;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onStart() {
        super.onStart();
//        final Button btScan = findViewById(R.id.button_Scan);
        isScanning = true;
//        btScan.setText("停止掃描");
        findDevice.clear();
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**避免跳轉後掃描程序係續浪費效能，因此離開頁面後即停止掃描*/
    @SuppressLint("NewApi")
    @Override
    protected void onStop() {
        super.onStop();
//        final Button btScan = findViewById(R.id.button_Scan);
        /**關閉掃描*/
        isScanning = false;
//        btScan.setText("開始掃描");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            new Thread(()->{
//                SystemClock.sleep(500);
                if (device.getAddress().equals(mDeviceAddress)) {
//                    Log.v("wyc","rssi" + String.valueOf(rssi));

                    /**判斷是否離開*/
                    if (Math.abs(rssi)>=80){        //設定離開範圍
                        notice.Isleave(true);
                        count++;
                        checkleave++;
                        Log.v("wyc","Isleave : " + notice.getLeave());
                        Log.v("wyc","count : " + count);
                    }
                    else{
                        notice.Isleave(false);
                        count++;
                        Log.v("wyc","Isleave : " + notice.getLeave());
                        Log.v("wyc","count : " + count);
                    }

                    runOnUiThread(()->{
                    });
                }
            }).start();
            if ((notice.getLeave()!=null)&&(!state)&&(count>=8)){
                Log.v("wyc","check : " + checkleave);
                Alert();
                state = true;
                count = 0;
            }
        }
    };

    /**Byte轉16進字串工具*/
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        return gethex;
    }

    /**濾除重複的藍牙裝置(以Address判定)*/
    private ArrayList getSingle(ArrayList list) {
        ArrayList tempList = new ArrayList<>();
        try {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                if (!tempList.contains(obj)) {
                    tempList.add(obj);
                } else {
                    tempList.set(getIndex(tempList, obj), obj);
                }
            }
            return tempList;
        } catch (ConcurrentModificationException e) {
            return tempList;
        }
    }

    /**以Address篩選陣列->抓出該值在陣列的哪處*/
    private int getIndex(ArrayList temp, Object obj) {
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).toString().contains(obj.toString())) {
                return i;
            }
        }
        return -1;
    }

    private void Alert () {

        int position = getIntent().getIntExtra("position",-1);

//        do{
//            Log.v("wyc","isleave : " + notice.getLeave());
//            try{
//                // delay 0.5 second
//                Thread.sleep(500);
//            } catch(InterruptedException e){
//                e.printStackTrace();
//            }
//            if (!notice.getLeave()){
//                checkleave = false;
//            }
//            count ++;
//        }while (count<6);

        Log.v("wyc","check : " + checkleave);

        if (checkleave<8) {
            /**貪睡部分*/
            mediaPlayer = MediaPlayer.create(this,R.raw.wantouy);
            mediaPlayer.start();
            AlertDialog.Builder alert = new AlertDialog.Builder(AlarmAlert.this);
            alert.setIcon(R.drawable.alarm);
            alert.setTitle("時間到~");
            alert.setCancelable(false);
            alert.setMessage("起床了!!!");
            alert.setPositiveButton("貪 睡",((dialog,which) -> {}));
            AlertDialog dialog = alert.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v ->{     //按鈕按下後的動作
//                if (!notice.getLeave()){
//                    Log.v("wyc","turn-leave : " + notice.getLeave());
//                    Toast.makeText(this,"還沒離開",Toast.LENGTH_LONG).show();
//                }
//                else {
//                    Log.v("wyc","turn-leave : " + notice.getLeave());
//                    dialog.dismiss();
//                    AlarmAlert.this.finish();
//                    mediaPlayer.stop();
//                }
                /**再設定一個鬧鐘，但不再列表上顯示*/
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.add(Calendar.MINUTE, 1);

                Clock clock = new Clock();
                String hourformat = format(calendar.get(Calendar.HOUR_OF_DAY));
                String minuteformat = format(calendar.get(Calendar.MINUTE));

                Intent intent = new Intent(getApplicationContext(), CallAlarm.class);
                PendingIntent sender = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (System.currentTimeMillis()>calendar.getTimeInMillis()+40000){
                        //加24小时
                        am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis()+86400000, sender);
                    }else {
                        am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
                    }
                }

                clock.setHour(hourformat);
                clock.setMinute(minuteformat);
                clock.setContent("snooze");
                clock.setClockType(Clock.clock_open);

                getApplicationContext().stopService(intent);

                Toast.makeText(this,"再次響起時間 " + hourformat + " : " + minuteformat,Toast.LENGTH_LONG).show();

                finish();

                mediaPlayer.stop();
                dialog.dismiss();
                AlarmAlert.this.finish();
            }));
            dialog.setCancelable(false);
        }
        else {
            /**已離開*/
            AlertDialog.Builder alert = new AlertDialog.Builder(AlarmAlert.this);
            alert.setIcon(R.drawable.alarm);
            alert.setTitle("時間到~");
            alert.setCancelable(false);
            alert.setMessage("你很棒  起床了");
            alert.setPositiveButton("關 閉",((dialog,which) -> {}));
            AlertDialog dialog = alert.create();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v ->{
                Log.v("wyc","turn-leave : " + notice.getLeave());
                dialog.dismiss();
                AlarmAlert.this.finish();
            }));
            dialog.setCancelable(false);
        }

    }

    private String format(int x) {
        String s = "" + x;
        if (s.length() == 1) {
            s = "0" + s;
        }
        return s;
    }

}
