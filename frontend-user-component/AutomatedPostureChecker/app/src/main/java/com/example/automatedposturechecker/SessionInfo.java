package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_SESSION_ID;
import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class SessionInfo extends AppCompatActivity {

    private static final String TAG = "SessionInfo";

    private String session_id;
    private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_info);

        TextView name_text = findViewById(R.id.name_text);
        TextView date_text = findViewById(R.id.date_text);
        TextView duration_text = findViewById(R.id.duration_text);
        TextView sitting_text = findViewById(R.id.sitting_text);
        TextView posture_text = findViewById(R.id.posture_text);
        ScrollView session_stats = findViewById(R.id.session_stats);


        //GET EXTRAS
        user_id = getIntent().getStringExtra(EXTRA_USER_ID);
        session_id = getIntent().getStringExtra(EXTRA_SESSION_ID);

        //GET SESSION REQUEST

        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id;
        JsonObjectRequest session_req = new JsonObjectRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String name = "";
                String date = "";
                String duration = "";
                String sitting = "";
                String posture = "";
                try {
                    name = response.getString("name");
                    date = response.getString("start_time");
                    int duration_int = response.getInt("duration");
                    int hours = duration_int / 3600;
                    int minutes = (duration_int % 3600) / 60;
                    int seconds = duration_int % 60;
                    duration = String.format(Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds);
                    double total_good_posture = response.getDouble("total_good_posture");
                    double total_sitting = response.getDouble("total_sitting");
                    sitting = String.format(Locale.getDefault(), "%.2f",total_sitting*100) + "%";
                    posture = String.format(Locale.getDefault(), "%.2f",total_good_posture*100) + "%";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                name_text.setText(name);
                date_text.setText(date);
                duration_text.setText(duration);
                sitting_text.setText(sitting);
                posture_text.setText(posture);
                session_stats.setVisibility(View.VISIBLE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        SingletonRequestQueue.getInstance(SessionInfo.this).addToRequestQueue(session_req);

        //BACK
        ImageButton back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoSessionList();
            }
        });

        //DELETE
        Button delete_btn = findViewById(R.id.delete_btn);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSession();
            }
        });
    }

    @Override
    public void onBackPressed() {
        gotoSessionList();
    }

    private void gotoSessionList() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SessionInfo.this, R.anim.fade_in, R.anim.slide_out_bot);
        Intent intent = new Intent(SessionInfo.this, SessionList.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }

    private void deleteSession() {
        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id + "/" + session_id;
        StringRequest delete_session_req = new StringRequest(
                Request.Method.DELETE, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                gotoSessionList();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.toString());
            }
        });
        SingletonRequestQueue.getInstance(SessionInfo.this).addToRequestQueue(delete_session_req);
    }
}