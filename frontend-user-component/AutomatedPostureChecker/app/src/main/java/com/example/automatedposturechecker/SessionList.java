package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;

public class SessionList extends AppCompatActivity {

    private static final String TAG = "SessionList";

    private ArrayList<SessionItem> sessionList = new ArrayList<>();
    private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_session_list);

        //SETUP SESSION LIST RECYCLER VIEW
        RecyclerView rvSessions = findViewById(R.id.session_list);
        DividerItemDecoration verticalDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        Drawable divider = ContextCompat.getDrawable(this, R.drawable.divider);
        verticalDecoration.setDrawable(divider);
        rvSessions.addItemDecoration(verticalDecoration);
        SessionListAdapter adapter = new SessionListAdapter(sessionList);
        rvSessions.setAdapter(adapter);
        rvSessions.setLayoutManager(new LinearLayoutManager(SessionList.this));

        //GET SESSION LIST REQUEST
        user_id = getIntent().getStringExtra(EXTRA_USER_ID);
        Log.d(TAG, "user_id: " + user_id);

        String url = getString(R.string.base_url) + getString(R.string.sessions_url) + user_id;
        JsonArrayRequest session_list_req = new JsonArrayRequest(
                Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                sessionList = SessionItem.JSONArraytoList(response);
                Collections.reverse(sessionList);
                SessionListAdapter adapter = new SessionListAdapter(sessionList);
                rvSessions.setAdapter(adapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        SingletonRequestQueue.getInstance(SessionList.this).addToRequestQueue(session_list_req);

        //CREATE SESSION BUTTON
        FloatingActionButton add_session = findViewById(R.id.add_session);
        add_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoCreateSession();
            }
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SessionList.this);
        builder.setTitle("Confirm Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                gotoSignIn();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog close_alert = builder.create();
        close_alert.show();
    }

    private void gotoSignIn() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SessionList.this, R.anim.slide_in_top, R.anim.slide_out_bot);
        Intent intent = new Intent(SessionList.this, SignIn.class);
        startActivity(intent, options.toBundle());
    }

    private void gotoCreateSession() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SessionList.this, R.anim.slide_in_bot, R.anim.fade_out);
        Intent intent = new Intent(SessionList.this, CreateSession.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }
}