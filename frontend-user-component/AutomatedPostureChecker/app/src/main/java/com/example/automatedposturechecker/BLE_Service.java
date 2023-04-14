package com.example.automatedposturechecker;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.UUID;

public class BLE_Service extends Service {

    public final static String TAG = "BluetoothLeService";

    private Binder binder = new LocalBinder();
    private LinkedList<BluetoothGatt> gattInstances = new LinkedList<>();

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt bluetoothGatt;

    public static final String ACTION_GATT_CONNECTED =
            "com.example.automatedposturechecker.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED =
            "com.example.automatedposturechecker.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.automatedposturechecker.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE =
            "com.example.automatedposturechecker.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.example.automatedposturechecker.EXTRA_DATA";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;
    private int connectionState = STATE_DISCONNECTED;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    class LocalBinder extends Binder {
        public BLE_Service getService() {
            return BLE_Service.this;
        }
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (connectionState == STATE_CONNECTED) {
            return true;
        }
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            gattInstances.add(bluetoothGatt);
            return true;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }
    }

    public int writeString(String s) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED;
        }
        UUID service_UUID = UUID.fromString(getString(R.string.service_UUID_string));
        BluetoothGattService service = bluetoothGatt.getService(service_UUID);
        if (service == null) {
            Log.e(TAG, "Service not found");
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }

        UUID characteristic_UUID = UUID.fromString(getString(R.string.characteristic_UUID_string));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristic_UUID);
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found");
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        byte[] value = s.getBytes(StandardCharsets.UTF_8);
        characteristic.setValue(value);
        if (bluetoothGatt.writeCharacteristic(characteristic)) {
            return BluetoothStatusCodes.SUCCESS;
        } else {
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }
    }

    public int writeBytes(byte[] bytes) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt not initialized");
            return BluetoothStatusCodes.ERROR_DEVICE_NOT_BONDED;
        }
        UUID service_UUID = UUID.fromString(getString(R.string.service_UUID_string));
        BluetoothGattService service = bluetoothGatt.getService(service_UUID);
        if (service == null) {
            Log.e(TAG, "Service not found");
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }

        UUID characteristic_UUID = UUID.fromString(getString(R.string.characteristic_UUID_string));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristic_UUID);
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found");
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        characteristic.setValue(bytes);
        if (bluetoothGatt.writeCharacteristic(characteristic)) {
            return BluetoothStatusCodes.SUCCESS;
        } else {
            return BluetoothStatusCodes.ERROR_UNKNOWN;
        }
    }

    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        while (!gattInstances.isEmpty()){
            Log.d(TAG, "Disconnecting instance");
            BluetoothGatt instance = gattInstances.pop();
            instance.disconnect();
            instance.close();
        }
        Log.d(TAG, "closing");
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        Log.i(TAG, "Notification received: " + new String(data, StandardCharsets.UTF_8));
        intent.putExtra(EXTRA_DATA, Utils.byteArrayToHexString(data));
        sendBroadcast(intent);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.i(TAG, "Connected to GATT server");
                //Discover on connection
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                bluetoothGatt.requestMtu(100);
                Log.i(TAG, "GATT server discovered services");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
}
