package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_LAST_IMAGE_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SetupSession extends AppCompatActivity {

    private static final String TAG = "SetupSession";

    private boolean connected = false;
    private boolean checkedPosition = false;

    private final int SESSION_LIST_STATE = 0;
    private final int CREATE_SESSION_STATE = 1;

    private static final int REQUEST_ENABLE_BT = 200;

    private String session_id;
    private String user_id;
    private String last_image_id;

    private String wifiPassword = null;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BLE_Service bluetoothLeService;
    private List<ScanFilter> filters = new ArrayList<>();
    private ScanSettings settings;
    private Button scan_btn, start_btn, check_position_btn;
    private ImageView test_image;
    private ImageButton cancel_btn;
    private TextView status_text;

    private static final int BLUETOOTH_PERMISSION_CODE = 300;
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 301;
    private static final int BLUETOOTH_CONNECT_PERMISSION_CODE = 302;
    private static final int FINE_LOCATION_PERMISSION_CODE = 303;
    private static final int COARSE_LOCATION_PERMISSION_CODE = 304;

    private static final int SCAN_PERIOD = 7000;
    private boolean scanning;
    private Handler handler = new Handler();

    private Intent gattServiceIntent;

    private static final String deviceName = "APC_Setup";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLeService = ((BLE_Service.LocalBinder) service).getService();
            if (bluetoothLeService != null) {
                if (!bluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_session);

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

        gattServiceIntent = new Intent(this, BLE_Service.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(gattServiceIntent);

        //GET EXTRAS
        user_id = getIntent().getStringExtra(EXTRA_USER_ID);
        session_id = getIntent().getStringExtra(EXTRA_SESSION_ID);

        Log.d(TAG, "user_id: " + user_id);
        Log.d(TAG, "session_id: " + session_id);

        //SCAN
        scan_btn = findViewById(R.id.scan_btn);
        status_text = findViewById(R.id.status_text);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            status_text.setText("Bluetooth not available");
            status_text.setTextColor(ContextCompat.getColor(SetupSession.this, R.color.app_red));
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        filters.add(new ScanFilter.Builder().setDeviceName(deviceName).build());
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();

        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan_btn.setError(null);
                checkPermissions();
                wifiPassword = null;
                scanBLE();
            }
        });

        //CHECK POSITION
        check_position_btn = findViewById(R.id.check_position_btn);
        test_image = findViewById(R.id.test_image);
        check_position_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    scan_btn.setError("Connect to device to check position");
                    return;
                }
                test_image.setImageDrawable(getResources().getDrawable(R.drawable.ic_camera));
                sendBluetoothTest();
                getTestImage();
            }
        });


        //START
        start_btn = findViewById(R.id.start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK FIELDS
                if (!connected) {
                    scan_btn.setError("Connect to device before starting");
                    return;
                }

                if (!checkedPosition) {
                    check_position_btn.setError("Check camera position before starting");
                    return;
                }

                sendBluetoothStart();
                gotoSessionInProgress();
            }
        });

        //CANCEL
        cancel_btn = findViewById(R.id.cancel_setup_session_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(serviceConnection);
                deleteSession(SESSION_LIST_STATE);
                gattServiceIntent = null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(gattUpdateReceiver);
        SingletonRequestQueue.getInstance(SetupSession.this).cancelAll(TAG);
    }

    @Override
    public void onBackPressed() {
        unbindService(serviceConnection);
        deleteSession(CREATE_SESSION_STATE);
        gattServiceIntent = null;
    }

    private void gotoSessionInProgress() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SetupSession.this, R.anim.slide_in_right, R.anim.slide_out_left);
        Intent intent = new Intent(SetupSession.this, SessionInProgress.class);
        Bundle extras = getIntent().getExtras();
        intent.putExtras(extras);
        intent.putExtra(EXTRA_LAST_IMAGE_ID, last_image_id);
        startActivity(intent, options.toBundle());
    }

    private void gotoCreateSession() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SetupSession.this, R.anim.slide_in_left, R.anim.slide_out_right);
        Intent intent = new Intent(SetupSession.this, CreateSession.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }

    private void gotoSessionList() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SetupSession.this, R.anim.fade_in, R.anim.slide_out_bot);
        Intent intent = new Intent(SetupSession.this, SessionList.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            checkPermission(Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_PERMISSION_CODE);
            checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_PERMISSION_CODE);
        } else {
            checkPermission(Manifest.permission.BLUETOOTH, BLUETOOTH_PERMISSION_CODE);
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_LOCATION_PERMISSION_CODE);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, COARSE_LOCATION_PERMISSION_CODE);
        }
    }

    private void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(SetupSession.this, permission) ==
                PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(SetupSession.this, new String[]{
                    permission
            }, requestCode);
        }
    }

    private void scanBLE() {
        if (!scanning) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                    if (!connected) {
                        status_text.setText("Device not found");
                    }
                    setScanUI(R.color.app_blue, true);
                }
            }, SCAN_PERIOD);

            scanning = true;
            status_text.setText("Scanning...");
            setScanUI(R.color.app_grey, false);
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (connected) {
                return;
            }
            BluetoothDevice device = result.getDevice();
            bluetoothLeService.connect(device.getAddress());
            scanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLE_Service.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                status_text.setText("Device connected");
                setScanUI(R.color.app_blue, true);
            } else if (BLE_Service.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                status_text.setText("Device disconnected");
                scanning = false;
                bluetoothLeScanner.stopScan(scanCallback);
                setScanUI(R.color.app_blue, true);
            } else if (BLE_Service.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Services discovered
                requestWiFiPassword();
            } else if (BLE_Service.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BLE_Service.EXTRA_DATA);
                Utils.showToast(SetupSession.this, "BLE Notification: " + data);
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLE_Service.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLE_Service.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLE_Service.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLE_Service.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void requestWiFiPassword() {
        if (wifiPassword != null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(SetupSession.this);
        builder.setTitle("Requesting WiFi Password For Device");
        builder.setMessage("Please enter the password of your current WiFi connection for the device to use.");
        final View customLayout = getLayoutInflater().inflate(R.layout.alert_password, null);
        builder.setView(customLayout);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EditText password_input = customLayout.findViewById(R.id.password_input);
                wifiPassword = password_input.getText().toString();
                sendBluetoothConfig();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void sendBluetoothConfig() {
        byte[] byte_config_opcode = Utils.hexStringToByteArray(getString(R.string.CONFIG_OPCODE));
        byte[] byte_sessionID = session_id.getBytes(StandardCharsets.UTF_8);
        byte[] byte_userID = user_id.getBytes(StandardCharsets.UTF_8);
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        byte[] byte_ssid = getSSID(ssid);
        if (byte_ssid.length > 32) {
            Log.e(TAG, "SSID length greater than 32");
            return;
        }
        byte[] byte_ssid_length = BigInteger.valueOf(byte_ssid.length).toByteArray();
        byte[] byte_password = wifiPassword.getBytes(StandardCharsets.UTF_8);
        if (byte_password.length > 32) {
            Log.e(TAG, "SSID length greater than 32");
            return;
        }
        byte[] byte_password_length = BigInteger.valueOf(byte_password.length).toByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(byte_config_opcode);
            outputStream.write(byte_sessionID);
            outputStream.write(byte_userID);
            outputStream.write(byte_ssid_length);
            outputStream.write(byte_ssid);
            outputStream.write(byte_password_length);
            outputStream.write(byte_password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] config_bytes = outputStream.toByteArray();
        Log.d(TAG, "Config bytes: " + Utils.byteArrayToHexString(config_bytes));
        bluetoothLeService.writeBytes(config_bytes);
    }

    private void sendBluetoothStart() {
        byte[] byte_start_opcode = Utils.hexStringToByteArray(getString(R.string.START_OPCODE));
        bluetoothLeService.writeBytes(byte_start_opcode);
    }

    private void sendBluetoothTest() {
        byte[] byte_test_opcode = Utils.hexStringToByteArray(getString(R.string.TEST_OPCODE));
        bluetoothLeService.writeBytes(byte_test_opcode);
    }

    private byte[] getSSID(String s) {
        if (s.charAt(0) == '\"') {
            return s.substring(1, s.length() - 1).getBytes(StandardCharsets.UTF_8);
        } else {
            return Utils.hexStringToByteArray(s);
        }
    }

    private void deleteSession(int next_state) {
        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id;
        StringRequest delete_session_req = new StringRequest(
                Request.Method.DELETE, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (next_state == SESSION_LIST_STATE) {
                    gotoSessionList();
                } else if (next_state == CREATE_SESSION_STATE) {
                    gotoCreateSession();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.toString());
            }
        });
        SingletonRequestQueue.getInstance(SetupSession.this).addToRequestQueue(delete_session_req);
    }

    private void setScanUI(int color, boolean enable) {
        scan_btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(SetupSession.this, color)));
        scan_btn.setEnabled(enable);
    }

    private void getTestImage() {
        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id + "/" + getString(R.string.image_url);
        JsonObjectRequest img_req = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String image_id = response.getString("image_id");
                    if (last_image_id != null && image_id.equals(last_image_id)) {
                        getTestImage();
                        return;
                    } else {
                        last_image_id = image_id;
                        String image_base64 = response.getString("content");
                        byte[] image_decoded = Base64.decode(image_base64, Base64.DEFAULT);
                        Bitmap image_bitmap = BitmapFactory.decodeByteArray(image_decoded, 0, image_decoded.length);
                        test_image.setImageBitmap(image_bitmap);
                        checkedPosition = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
                getTestImage();
            }
        });
        img_req.setTag(TAG);
        Log.d(TAG, "Sending request");
        SingletonRequestQueue.getInstance(SetupSession.this).addToRequestQueue(img_req);
    }
}
