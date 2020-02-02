package com.example.mybleapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends AppCompatActivity {

        private static final String TAG = "DeviceControlActivity";
        public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
        public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
        private String mDeviceName;
        private String mDeviceAddress;
        private boolean connected = false;

        private TextView mConnectionState;
        private  TextView mDataField;
        private BluetoothGattCharacteristic mNotifyCharacteristic;
        private ExpandableListView mGattServicesList;
        private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
                new ArrayList<>();

        private final String LIST_NAME = "NAME";
        private final String LIST_UUID = "UUID";

        BluetoothLeService bluetoothLeService;


        private final ServiceConnection mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "service connected called");
                bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!bluetoothLeService.initialize()) {
                    Log.d(TAG, "Unable to initialize bluetoothLeService");
                    finish();
                }
                bluetoothLeService.connect(mDeviceAddress);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                bluetoothLeService = null;
            }
        };

        // Handles various events fired by the Service.// ACTION_GATT_CONNECTED: connected to a GATT server.
        // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
        // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
        // ACTION_DATA_AVAILABLE: received data from the device. This can be a
        // result of read or notification operations.
        private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    connected = true;
                    updateConnectionState(R.string.connected);
                    invalidateOptionsMenu();
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    connected = false;
                    updateConnectionState(R.string.disconnected);
                    invalidateOptionsMenu();
                    clearUI();
                //clearUI();
                } else if (BluetoothLeService.
                        ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the
                    // user interface.
                    displayGattServices(bluetoothLeService.getSupportedGattServices());
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        };



        private final ExpandableListView.OnChildClickListener servicesListClickListner =
                new ExpandableListView.OnChildClickListener() {
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
                                    bluetoothLeService.setCharacteristicNotification(
                                            mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                bluetoothLeService.readCharacteristic(characteristic);
                            }
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mNotifyCharacteristic = characteristic;
                                bluetoothLeService.setCharacteristicNotification(
                                        characteristic, true);
                            }

                            if((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE)>0){

                            }


                            return true;
                        }
                        return false;
                    }
                };


        private void updateConnectionState(final int resourceId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mConnectionState.setText(resourceId);
                }
            });
        }


        private void displayData(String data) {
            if (data != null) {
                //mDataField.setText(data);
            }
        }


        private void clearUI() {
            mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
            //        mDataField.setText(R.string.no_data);
        }



        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_device_control);


            //UI view
            //mConnectionState = findViewById(R.id.connection_state);
            //mDataField = findViewById(R.id.data_value);
            mGattServicesList = findViewById(R.id.gatt_services_list);
            mGattServicesList.setOnChildClickListener(servicesListClickListner);

            final Intent intent = getIntent();
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
            Log.d(TAG, "on create called");
        }


        @Override
        protected void onResume() {
            super.onResume();
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
            if (bluetoothLeService != null) {
                final boolean result = bluetoothLeService.connect(mDeviceAddress);
                Log.d(TAG, "Connect request result=" + result);
            }
        }


        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(gattUpdateReceiver);
        }



        @Override
        protected void onDestroy() {
            super.onDestroy();
            unbindService(mServiceConnection);
            bluetoothLeService = null;
        }


        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.connect, menu);
            return true;
        }


        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_connect:
                    bluetoothLeService.connect(mDeviceAddress);
                    Toast.makeText(this, "connected  " + mDeviceName, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.menu_disconnect:
                    bluetoothLeService.disconnect();
                    Toast.makeText(this, "disconnected  " + mDeviceAddress, Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.home:
                    onBackPressed();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
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



        private void displayGattServices(List<BluetoothGattService> gattServices) {
            if (gattServices == null) return;
            String uuid ;
            String unknownServiceString = getResources().
                    getString(R.string.unknown_service);
            String unknownCharaString = getResources().
                    getString(R.string.unknown_characteristic);
            ArrayList<HashMap<String, String>> gattServiceData =
                    new ArrayList<>();
            ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                    = new ArrayList<>();
            mGattCharacteristics =
                    new ArrayList<>();

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : gattServices) {
                HashMap<String, String> currentServiceData =
                        new HashMap<>();
                uuid = gattService.getUuid().toString();
                currentServiceData.put(
                        LIST_NAME, GattAttributes.
                                lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                        new ArrayList<>();
                List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas =
                        new ArrayList<>();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic :
                        gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData =
                            new HashMap<>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(
                            LIST_NAME, GattAttributes.lookup(uuid,
                                    unknownCharaString));
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
                    new String[] {LIST_NAME, LIST_UUID},
                    new int[] { android.R.id.text1, android.R.id.text2 },
                    gattCharacteristicData,
                    android.R.layout.simple_expandable_list_item_2,
                    new String[] {LIST_NAME, LIST_UUID},
                    new int[] { android.R.id.text1, android.R.id.text2 }
            );
            mGattServicesList.setAdapter(gattServiceAdapter);
        }

}