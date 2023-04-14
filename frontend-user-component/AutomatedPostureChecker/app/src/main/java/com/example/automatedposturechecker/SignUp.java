package com.example.automatedposturechecker;

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

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_sign_up);

        //CREATE ACCOUNT BUTTON
        Button create_account_btn = findViewById(R.id.create_account_btn);
        EditText username_input = findViewById(R.id.username_input);
        EditText password_input = findViewById(R.id.password_input);
        username_input.setError(null);
        password_input.setError(null);

        create_account_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK FIELDS
                String username = username_input.getText().toString();
                String password = password_input.getText().toString();
                boolean emptyFields = false;
                if (username.trim().equalsIgnoreCase("")) {
                    username_input.setError("This field cannot be blank");
                    emptyFields = true;
                }
                if (password.trim().equalsIgnoreCase("")) {
                    password_input.setError("This field cannot be blank");
                    emptyFields = true;
                }
                if (emptyFields) {
                    return;
                }

                //CREATE ACCOUNT REQUEST
                String url = getString(R.string.base_url) + getString(R.string.sign_up_url);
                JSONObject sign_up_data = new JSONObject();
                try {
                    sign_up_data.put("name", username);
                    sign_up_data.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JsonObjectRequest sign_up_req = new JsonObjectRequest(
                        Request.Method.POST, url, sign_up_data, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        goToSignIn();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String body;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                body = new String(error.networkResponse.data, "UTF-8");
                                Utils.showToast(SignUp.this, body);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "Error: " + error);
                    }
                });
                SingletonRequestQueue.getInstance(SignUp.this).addToRequestQueue(sign_up_req);
            }
        });

        //CANCEL BUTTON
        ImageButton cancel_btn = findViewById(R.id.cancel_sign_up_btn);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSignIn();
            }
        });
    }

    @Override
    public void onBackPressed() {
        goToSignIn();
    }

    private void goToSignIn() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SignUp.this, R.anim.slide_in_left, R.anim.slide_out_right);
        Intent intent = new Intent(SignUp.this, SignIn.class);
        startActivity(intent, options.toBundle());
    }
}