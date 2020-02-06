package com.example.mybleapp;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;


// A service that interacts with the BLE device via the Android BLE API.
public class BluetoothLeService extends Service {

    private static final String TAG = "BluetoothLeService";


    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private Handler handler;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";


    public final static  UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_HEART_RATE_CONTROL_POINT = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("000033f3-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("000033f4-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_JYOU = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb");
    public static  final UUID UUID_DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        connectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" + bluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BluetoothGattDescriptor descriptor = gatt.getService(HEART_RATE_SERVICE_UUID)
                                .getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"))
                                .getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                        Log.d(TAG, "Notification Enabled");
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        Log.w(TAG, "Broadcast update called");
                    }
                }


                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                                  int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }


                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                }


                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    super.onReadRemoteRssi(gatt, rssi, status);
                }



            };


            private void broadcastUpdate(final String action) {
                final Intent intent = new Intent(action);
                sendBroadcast(intent);
            }

            private void broadcastUpdate(final String action,
                                         final BluetoothGattCharacteristic characteristic) {
                final Intent intent = new Intent(action);
                // This is special handling for the Heart Rate Measurement profile. Data
                // parsing is carried out as per profile specifications.
                if(UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())){
                    byte[] data = new byte[1];
                    data[0] =  (byte) (59 & 0xFF);
                    boolean a =  characteristic.setValue(data);
                    String d = " data write false";
                    if(a){
                        d = " data write true";
                    }
                    Log.d(TAG,  d);
                    intent.putExtra(EXTRA_DATA, "hello");
                    }




                if (UUID_HEART_RATE_CONTROL_POINT.equals(characteristic.getUuid())) {
                    Log.d(TAG, "heart rate data read called");
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "Heart rate format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "Heart rate format UINT8.");
                    }
                    final int heartRate = characteristic.getIntValue(format, 1);
                    Log.d(TAG, String.format("Received heart rate: %s", String.valueOf(heartRate)));
                    intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
                } else {
                    // For all other profiles, writes the data formatted in HEX.
                    final byte[] data = characteristic.getValue();
                    if (data != null && data.length > 0) {
                        final StringBuilder stringBuilder = new StringBuilder(data.length);
                        for(byte byteChar : data)
                            stringBuilder.append(String.format("%02X ", byteChar));
                        intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                                stringBuilder.toString());
                    }
                }
                sendBroadcast(intent);
            }


        public boolean initialize() {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "initialize: Unable to initialize a bluetooth adapter");
                return false;
            }
            return true;
        }


        @RequiresApi(api = Build.VERSION_CODES.M)
        public boolean connect(String address) {
            Log.d(TAG, "connect called");
            if(bluetoothAdapter == null || address == null){
                Log.w(TAG, "Bluetooth adapter not initialized");
                return false;
            }

            if(bluetoothDeviceAddress!=null && address.equals(bluetoothDeviceAddress)
                    && bluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if(bluetoothGatt.connect()){
                    connectionState = STATE_CONNECTING;
                    return true;
                } else {
                     return false;
                }
            }

            final  BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if(device == null){
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }

            bluetoothGatt = device.connectGatt(this, true, gattCallback, TRANSPORT_LE);
            Log.d(TAG, "Trying to create a new connection.");

            bluetoothDeviceAddress = address;
            Log.d(TAG, bluetoothDeviceAddress   + "    bluetoothDeviceAddress");
            connectionState = STATE_CONNECTING;
            return  true;
        }



        public  void disconnect(){
            if(bluetoothAdapter == null || bluetoothGatt==null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            bluetoothGatt.disconnect();
            Log.d(TAG, "disconnected");
        }


        public class LocalBinder extends Binder {
            BluetoothLeService getService() {
                return BluetoothLeService.this;
            }
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return mBinder;
        }

        private final IBinder mBinder = new LocalBinder();


        @Override
        public boolean onUnbind(Intent intent) {
            // After using a given device, you should make sure that BluetoothGatt.close() is called
            // such that resources are cleaned up properly.  In this particular example, close() is
            // invoked when the UI is disconnected from the Service.
            close();
            return super.onUnbind(intent);
        }



        public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            bluetoothGatt.readCharacteristic(characteristic);
        }


        public String  writeCharacteristics(BluetoothGattCharacteristic characteristic){
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return null;
            }
                            byte[] value = new byte[1];
                            value[0] =  (byte) (21 & 0xFF);
                            String data = "00";

                            boolean a =  characteristic.setValue(data);
//                            characteristic.setWriteType(de);
                               String d = " data write false";
                               if(a){
                                    d = " data write true";
                               }
                            Log.d(TAG,  d );
                            bluetoothGatt.writeCharacteristic(characteristic);
              return data;
        }


        public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                  boolean enabled) {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }

            bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            // This is specific to Heart Rate Measurement.
            if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }


        public List<BluetoothGattService> getSupportedGattServices(){
            if(bluetoothGatt == null ) return null;
            return  bluetoothGatt.getServices();
        }


        public void close() {
            if (bluetoothGatt == null) {
                return;
            }
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

}