package com.example.mybleapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity  implements MyAdapter.onDeviceListener{
    private final String TAG = MainActivity.class.getSimpleName();
    //private final String macAddress = "2D:02:AD:AE:C6:FC";
    private ArrayList<BluetoothDevice> myDataset = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS =2 ;
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler;

    // Stops scanning after 10 seconds.

    private static final long SCAN_PERIOD = 10000;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        recyclerView =  findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
            recyclerView.setHasFixedSize(true);

        // use a linear layout manager
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            mAdapter = new MyAdapter(myDataset, this);
            recyclerView.setAdapter(mAdapter);



            handler = new Handler();

        //check for ble support
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scan, menu);
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan :
               scanLeDevice(true);
                return true;
            case R.id.menu_stop :
                scanLeDevice(false);
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        //Location access
        int permissionCoarse = Build.VERSION.SDK_INT >= 23 ?
                ContextCompat
                        .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) :
                PackageManager.PERMISSION_GRANTED;

        if (permissionCoarse == PackageManager.PERMISSION_GRANTED) {
            scanLeDevice(true);
        } else {
            askForCoarseLocationPermission();
        }



    }


    private void askForCoarseLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSIONS);
    }






    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_ENABLE_BT && requestCode== Activity.RESULT_CANCELED){
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scanLeDevice(final boolean enable){
        final BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        String[] peripheralAddresses = new String[]{"2D:02:AD:AE:C6:FC"};
        // Build filters list
        List<ScanFilter> filters = null;
        if (peripheralAddresses != null) {
        filters = new ArrayList<>();
        for (String address : peripheralAddresses) {
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceAddress(address)
                    .build();
            filters.add(filter);
        }
        }

         ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build();

            if(enable){
                handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mScanning = false;
                            scanner.stopScan(scanCallback);
//                            mAdapter = new MyAdapter(myDataset, this);
                            recyclerView.setAdapter(mAdapter);
                        }
                    }, SCAN_PERIOD);
                if (scanner != null) {
                    scanner.startScan(filters, scanSettings, scanCallback);
                    Log.d(TAG, "scan started scanner");
                    mScanning=true;
                }  else {
                    Log.e(TAG, "could not get scanner object");
                }
            } else{
                mScanning = false;
                scanner.stopScan(scanCallback);
            }

    }

    private final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // ...do whatever you want with this found device
            if(!myDataset.contains(device)) {
                myDataset.add(device);
            }
            recyclerView.setAdapter(mAdapter);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // Ignore for now
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Ignore for now
        }
    };


    public  void onDeviceClick(int position){
        BluetoothDevice device =  myDataset.get(position);
        Intent intent = new Intent(this,  DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME,   device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        startActivity(intent);
    }



}




