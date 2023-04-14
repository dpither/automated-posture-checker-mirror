package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_BAD_POSTURE_THRESHOLD;
import static com.example.automatedposturechecker.Utils.EXTRA_LAST_IMAGE_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_NOTIFICATION_TYPE;
import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_NAME;
import static com.example.automatedposturechecker.Utils.EXTRA_STRETCH_REMINDER;
import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_MUTE;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_SOUND;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_VIBRATE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SessionInProgress extends AppCompatActivity {
    private static final String TAG = "SessionInProgress";

    private int TIMER_DELAY = 1000;

    private String user_id;
    private String session_id;
    private String session_name;
    private int notification_type;
    private int stretch_reminder;
    private int bad_posture_threshold;

    private BLE_Service bluetoothLeService;
    private Button pause_resume_btn;
    private TextView timer_text;

    private boolean timer_running = false;
    private boolean inflight_request = false;
    private boolean notifications_on = true;
    private String last_image_id = null;
    private long minimum_image_date;
    private int time_in_seconds = 0;
    private int stretch_timer = 0;
    private int bad_posture_counter = 0;
    private Timer timer;
    private TimerTask timerTask;

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
        setContentView(R.layout.activity_session_in_progress);

        Intent gattServiceIntent = new Intent(this, BLE_Service.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(gattServiceIntent);

        //GET EXTRAS
        user_id = getIntent().getStringExtra(EXTRA_USER_ID);
        session_id = getIntent().getStringExtra(EXTRA_SESSION_ID);
        session_name = getIntent().getStringExtra(EXTRA_SESSION_NAME);
        notification_type = getIntent().getIntExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_SOUND);
        stretch_reminder = getIntent().getIntExtra(EXTRA_STRETCH_REMINDER, 0);
        bad_posture_threshold = getIntent().getIntExtra(EXTRA_BAD_POSTURE_THRESHOLD, 0);
        last_image_id = getIntent().getStringExtra(EXTRA_LAST_IMAGE_ID);
        TextView session_title = findViewById(R.id.name_text);
        session_title.setText(session_name);
        Log.d(TAG, "user_id: " + user_id);
        Log.d(TAG, "session_id: " + session_id);

        timer_text = findViewById(R.id.timer_text);
        timer = new Timer();
        startTimer();

        //PAUSE/RESUME
        pause_resume_btn = findViewById(R.id.pause_resume_btn);
        pause_resume_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer_running) {
                    cancelTimer();
                    setPauseButtonUI(getString(R.string.resume), R.color.app_green);
                } else {
                    startTimer();
                    setPauseButtonUI(getString(R.string.pause), R.color.app_yellow);
                }
            }
        });

        //STOP
        Button stop_btn = findViewById(R.id.stop_btn);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSession();
            }
        });

        //NOTIFICATION TOGGLE
        ImageButton notification_btn = findViewById(R.id.notification_btn);
        notification_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (notifications_on) {
                    notifications_on = false;
                    notification_btn.setImageResource(R.drawable.ic_bell_off);
                } else {
                    notifications_on = true;
                    notification_btn.setImageResource(R.drawable.ic_bell_on);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void onBackPressed() {
    }

    private void setPauseButtonUI(String s, int color) {
        pause_resume_btn.setText(s);
        pause_resume_btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(SessionInProgress.this, color)));
    }

    private void startTimer() {
        if (timer_running) {
            return;
        }
        timer_running = true;
        minimum_image_date = System.currentTimeMillis();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        time_in_seconds++;
                        int hours = time_in_seconds / 3600;
                        int minutes = (time_in_seconds % 3600) / 60;
                        int seconds = time_in_seconds % 60;
                        String time_text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                        timer_text.setText(time_text);

                        stretch_timer++;
                        if (stretch_timer == stretch_reminder * 60) {
                            alertStretch();
                        }

                        if (!inflight_request) {
                            getLatestImage();
                        }
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, TIMER_DELAY, TIMER_DELAY);
    }

    private void cancelTimer() {
        timer_running = false;
        timerTask.cancel();
    }

    private void stopSession() {
        cancelTimer();

        AlertDialog.Builder builder = new AlertDialog.Builder(SessionInProgress.this);
        builder.setTitle("Confirm Session End");
        builder.setMessage("Are you sure you want to end this session?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendBluetoothStop();
                sendServerStop();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startTimer();
                dialog.cancel();
            }
        });
        AlertDialog close_alert = builder.create();
        close_alert.show();
    }

    private void sendBluetoothStop() {
        byte[] byte_stop_opcode = Utils.hexStringToByteArray(getString(R.string.STOP_OPCODE));
        bluetoothLeService.writeBytes(byte_stop_opcode);
    }

    private void gotoSessionInfo() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SessionInProgress.this, R.anim.slide_in_bot, R.anim.fade_out);
        Intent intent = new Intent(SessionInProgress.this, SessionInfo.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        intent.putExtra(EXTRA_SESSION_ID, session_id);
        startActivity(intent, options.toBundle());
    }

    private void alertStretch() {
        stretch_timer = 0;
        if (!notifications_on) {
            return;
        }
        notifyUser();
        cancelTimer();

        AlertDialog.Builder builder = new AlertDialog.Builder(SessionInProgress.this);
        builder.setTitle("Stretch Reminder");
        if (stretch_reminder == 1) {
            builder.setMessage("You have been sitting for " + stretch_reminder + " minute. \nConsider getting up to stretch.");
        } else {
            builder.setMessage("You have been sitting for " + stretch_reminder + " minutes. \nConsider getting up to stretch.");
        }

        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startTimer();
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void alertBadPosture(String image_base64, String comments) {
        bad_posture_counter = 0;
        if (!notifications_on) {
            return;
        }
        notifyUser();
        cancelTimer();

        byte[] image_decoded = Base64.decode(image_base64, Base64.DEFAULT);
        Bitmap image_bitmap = BitmapFactory.decodeByteArray(image_decoded, 0, image_decoded.length);
        final View customLayout = getLayoutInflater().inflate(R.layout.alert_posture, null);
        ImageView image = customLayout.findViewById(R.id.image);
        image.setImageBitmap(image_bitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(SessionInProgress.this);
        builder.setTitle("Bad Posture Alert");
        builder.setMessage("Here is an image of your current posture to help you improve.\n" + "Try adjusting the following parts:\n" + comments);
        builder.setCancelable(false);
        builder.setView(customLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startTimer();
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void notifyUser() {
        switch (notification_type) {
            case NOTIFICATION_SOUND:
                MediaPlayer mediaPlayer = MediaPlayer.create(SessionInProgress.this, R.raw.notification_sound);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.stop();
                        mp.release();
                    }
                });
                mediaPlayer.start();
                break;
            case NOTIFICATION_VIBRATE:
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 200, 100, 200};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, VibrationEffect.DEFAULT_AMPLITUDE));
                break;
            case NOTIFICATION_MUTE:
                return;
        }
    }

    private void handleNewImage(JSONObject image) {
        try {
            String image_base64 = image.getString("content");
            boolean goodPosture = image.getBoolean("goodPosture");
            boolean isSitting = image.getBoolean("isSitting");
            long date = image.getLong("date");

            if (!isSitting) {
                stretch_timer = 0;
                bad_posture_counter = 0;
                return;
            }
            if (goodPosture) {
                bad_posture_counter = 0;
                return;
            } else {
                bad_posture_counter++;
            }
            if (date < minimum_image_date) {
                return;
            }
            if (bad_posture_counter == bad_posture_threshold) {
                String comments = image.getString("comments");
                alertBadPosture(image_base64, comments);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getLatestImage() {
        inflight_request = true;
        Log.d(TAG, "Sending request");
        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id + "/" + getString(R.string.image_url);
        JsonObjectRequest img_req = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                inflight_request = false;
                if (response.length() == 0) {
                    return;
                }

                try {
                    String image_id = response.getString("image_id");
                    if (last_image_id != null && image_id.equals(last_image_id)) {
                        return;
                    } else {
                        last_image_id = image_id;
                        handleNewImage(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                inflight_request = false;
            }
        });
        SingletonRequestQueue.getInstance(SessionInProgress.this).addToRequestQueue(img_req);
    }

    private void sendServerStop() {
        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id + "/" + getString(R.string.status_url);
        JSONObject stop_data = new JSONObject();
        try {
            stop_data.put("duration", time_in_seconds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest stop_req = new JsonObjectRequest(
                Request.Method.POST, url, stop_data, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                gotoSessionInfo();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        SingletonRequestQueue.getInstance(SessionInProgress.this).addToRequestQueue(stop_req);
    }
}