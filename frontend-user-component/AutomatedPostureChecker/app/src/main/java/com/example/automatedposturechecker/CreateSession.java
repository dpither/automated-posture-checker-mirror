package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_BAD_POSTURE_THRESHOLD;
import static com.example.automatedposturechecker.Utils.EXTRA_NOTIFICATION_TYPE;
import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_NAME;
import static com.example.automatedposturechecker.Utils.EXTRA_STRETCH_REMINDER;
import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_MUTE;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_SOUND;
import static com.example.automatedposturechecker.Utils.NOTIFICATION_VIBRATE;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CreateSession extends AppCompatActivity {
    private static final String TAG = "CreateSession";

    private String session_id;
    private String user_id;
    private String session_name;
    private int notification_type;
    private int stretch_reminder;
    private int bad_posture_threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_create_session);

        user_id = getIntent().getStringExtra(EXTRA_USER_ID);

        //SETUP
        Button begin_setup_btn = findViewById(R.id.begin_setup_btn);
        EditText session_name_input = findViewById(R.id.session_name_input);
        EditText stretch_reminder_input = findViewById(R.id.stretch_reminder_input);
        EditText bad_posture_threshold_input = findViewById(R.id.bad_posture_threshold_input);

        session_name_input.setError(null);
        stretch_reminder_input.setError(null);
        bad_posture_threshold_input.setError(null);

        begin_setup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK FIELDS
                boolean invalidFields = false;
                //Session name
                session_name = session_name_input.getText().toString();
                if (session_name.trim().equalsIgnoreCase("")) {
                    session_name_input.setError("This field cannot be blank");
                    invalidFields = true;
                }

                //Notification type
                notification_type = getNotificationType();

                //Stretch reminder
                String stretch_reminder_text = stretch_reminder_input.getText().toString();
                if (stretch_reminder_text.trim().equalsIgnoreCase("")) {
                    stretch_reminder_input.setError("This field cannot be blank");
                    invalidFields = true;
                } else {
                    stretch_reminder = Integer.parseInt(stretch_reminder_text);
                    if (stretch_reminder <= 0) {
                        stretch_reminder_input.setError("Enter a number greater than 0");
                        invalidFields = true;
                    }
                }

                //Bad posture threshold
                String bad_posture_threshold_text = bad_posture_threshold_input.getText().toString();
                if (bad_posture_threshold_text.trim().equalsIgnoreCase("")) {
                    bad_posture_threshold_input.setError("This field cannot be blank");
                    invalidFields = true;
                } else {
                    bad_posture_threshold = Integer.parseInt(bad_posture_threshold_text);
                    if (bad_posture_threshold <= 0) {
                        bad_posture_threshold_input.setError("Enter a number greater than 0");
                        invalidFields = true;
                    }
                }

                if (invalidFields) {
                    return;
                }

                //CREATE SESSION REQUEST
                String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id;
                JSONObject create_session_data = new JSONObject();
                try {
                    create_session_data.put("name", session_name);
                    create_session_data.put("start_time", getTime());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JsonObjectRequest create_session_req = new JsonObjectRequest(
                        Request.Method.POST, url, create_session_data, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            session_id = response.getString("session_id");
                            gotoSetupSession(session_id);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
                SingletonRequestQueue.getInstance(CreateSession.this).addToRequestQueue(create_session_req);
            }
        });

        //CANCEL
        ImageButton cancel_btn = findViewById(R.id.cancel_create_session_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoSessionList();
            }
        });
    }

    @Override
    public void onBackPressed() {
        gotoSessionList();
    }

    private void gotoSetupSession(String session_id) {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(CreateSession.this, R.anim.slide_in_right, R.anim.slide_out_left);
        Intent intent = new Intent(CreateSession.this, SetupSession.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        intent.putExtra(EXTRA_SESSION_ID, session_id);
        intent.putExtra(EXTRA_SESSION_NAME, session_name);
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, notification_type);
        intent.putExtra(EXTRA_STRETCH_REMINDER, stretch_reminder);
        intent.putExtra(EXTRA_BAD_POSTURE_THRESHOLD, bad_posture_threshold);
        startActivity(intent, options.toBundle());
    }

    private void gotoSessionList() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(CreateSession.this, R.anim.fade_in, R.anim.slide_out_bot);
        Intent intent = new Intent(CreateSession.this, SessionList.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }

    private int getNotificationType() {
        RadioGroup notification_group = findViewById(R.id.notification_group);
        int selected = notification_group.getCheckedRadioButtonId();
        if (selected == R.id.select_sound) {
            return NOTIFICATION_SOUND;
        } else if (selected == R.id.select_vibration) {
            return NOTIFICATION_VIBRATE;
        } else if (selected == R.id.select_mute) {
            return NOTIFICATION_MUTE;
        } else {
            return -1;
        }
    }

    private String getTime() {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy h:mm a");
        String time_string = sdf.format(date);
        time_string = time_string.replace("a.m.", "AM").replace("p.m.", "PM");
        Log.d(TAG, time_string);
        return time_string;
    }
}

