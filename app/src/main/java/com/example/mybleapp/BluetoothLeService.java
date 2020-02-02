package com.example.mybleapp;

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

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

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
                    Handler bleHandler = new Handler();

                    final byte[] value = new byte[characteristic.getValue().length];
                    System.arraycopy(characteristic.getValue(), 0, value, 0, characteristic.getValue().length);


//                   bleHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            myProcessor.
//                        }
//                    })


                }


//                @Override
//                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
//                    super.onDescriptorRead(gatt, descriptor, status);
//                    // Do some checks first
//                    final BluetoothGattCharacteristic parentCharacteristic = descriptor.getCharacteristic();
//                    if (status != GATT_SUCCESS) {
//                        Timber.e("write descriptor failed value <%s>, device: %s, characteristic: %s", bytes2String(currentWriteBytes), getAddress(), parentCharacteristic.getUuid());
//                    }
//
//                    // Check if this was the Client Configuration Descriptor
//                    if (descriptor.getUuid().equals(UUID.fromString(CCC_DESCRIPTOR_UUID))) {
//                        if (status == GATT_SUCCESS) {
//                            // Check if we were turning notify on or off
//                            byte[] value = descriptor.getValue();
//                            if (value != null) {
//                                if (value[0] != 0) {
//                                    // Notify set to on, add it to the set of notifying characteristics
//                                    notifyingCharacteristics.add(parentCharacteristic.getUuid());
//                                    if (notifyingCharacteristics.size() > MAX_NOTIFYING_CHARACTERISTICS) {
//                                        Timber.e("too many (%d) notifying characteristics. The maximum Android can handle is %d", notifyingCharacteristics.size(), MAX_NOTIFYING_CHARACTERISTICS);
//                                    }
//                                } else {
//                                    // Notify was turned off, so remove it from the set of notifying characteristics
//                                    notifyingCharacteristics.remove(parentCharacteristic.getUuid());
//                                }
//                            }
//                        }
//
//                        // Propagate to callback, even when there was an error
//                        bleHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                peripheralCallback.onNotificationStateUpdate(BluetoothPeripheral.this, parentCharacteristic, status);
//                            }
//                        });
//                    } else {
//                        // Propagate to callback, even when there was an error
//                        bleHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                peripheralCallback.onDescriptorWrite(BluetoothPeripheral.this, currentWriteBytes, descriptor, status);
//                            }
//                        });
//                    }
//                    completedCommand();
//                }

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
                if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
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
                    Log.d(TAG, String.format("Received heart rate: %d", heartRate));
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

            bluetoothGatt = device.connectGatt(this, false, gattCallback, TRANSPORT_LE);
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

//            if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0){
//                return false;
//            }
            bluetoothGatt.readCharacteristic(characteristic);
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