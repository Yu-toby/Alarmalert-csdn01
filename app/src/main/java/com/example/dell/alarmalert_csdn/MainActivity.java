package com.example.dell.alarmalert_csdn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.LitePal;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    /**鬧鐘*/
    public static List<Clock> list = new ArrayList<>();
    public static TimeAdapter timeAdapter;
    RecyclerView recyclerView;
    NavigationView navigationView;
    DrawerLayout drawerLayout;
    private ImageView more;
    TextView title;
    Context context = MainActivity.this;

    /**Beacon*/
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning = false;
    ArrayList<ScannedData> findDevice = new ArrayList<>();
    private String mDeviceAddress = "54:6C:0E:9B:5C:79";
    TextView tvRssi;
    private int[] array;
    private int rssiAvg,count=0,seat=0;


//    public static AlarmAlert.Leave notice = new AlarmAlert.Leave();

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**鬧鐘*/
        title = findViewById(R.id.title);
        recyclerView = findViewById(R.id.clock_list);
        navigationView = findViewById(R.id.nav);
        more = findViewById(R.id.open_nav);
        drawerLayout = findViewById(R.id.layout1);
        initTitle();
        LitePal.getDatabase();
        initRecyclerView();

        /**Beacon*/
        tvRssi = findViewById(R.id.tvRssi);
        array = new int[6];

        /**權限相關認證*/
        checkPermission();
        /**初始藍牙掃描及掃描開關之相關功能*/
        bluetoothScan();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initRecyclerView();
    }

    private void initTitle() {
        more.setImageResource(R.drawable.ic_more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });
        title.setText("鬧鐘");
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.add:
                        Intent intent1 = new Intent(MainActivity.this, AddClock.class);
                        startActivity(intent1);
                        break;
                }
                return true;
            }
        });

    }

    private void initRecyclerView() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        timeAdapter = new TimeAdapter(list, context);
        recyclerView.setAdapter(timeAdapter);
        list.clear();
        List<Clock> list1 = DataSupport.findAll(Clock.class);
        for (Clock clock : list1) {
            list.add(clock);
        }
        timeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)){
            drawerLayout.closeDrawer(navigationView);
        }
        else {
            MainActivity.this.finish();
        }
    }

//======================================================================================

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
//                SystemClock.sleep(1000);
                if (device.getAddress().equals(mDeviceAddress)) {

                    if (count<6){
                        array[count] = rssi;
                        count++;
                    }
                    else {
                        array[seat] = rssi;
                        seat++;
                        if (seat>=6) {
                            seat = 0;
                        }
                    }

                    if (count>=5) {
                        Log.v("wyc","array : " +array[0] +" " + array[1] +" " + array[2] +" " + array[3] +" " + array[4] +" " + array[5]);
                        /** 15個數的和 */
                        rssiAvg = 0;
                        for (int i = 0; i <6; i++) {
                            rssiAvg += array[i];
                        }
                        Log.v("wyc","array : " +rssiAvg/6);
                    }

                    runOnUiThread(()->{
                        tvRssi.setText(String.valueOf(rssiAvg/6));
                    });
                }
            }).start();
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


}
