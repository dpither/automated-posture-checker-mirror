package com.example.automatedposturechecker;

import static com.example.automatedposturechecker.Utils.EXTRA_USER_ID;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class SignIn extends AppCompatActivity {

    private static final String TAG = "SignIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        setContentView(R.layout.activity_sign_in);

        //SIGN IN BUTTON
        Button sign_in_btn = findViewById(R.id.sign_in_btn);
        EditText username_input = findViewById(R.id.username_input);
        EditText password_input = findViewById(R.id.password_input);
        username_input.setError(null);
        password_input.setError(null);

        sign_in_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //CHECK FIELDS
                String username = username_input.getText().toString();
                String password = password_input.getText().toString();
                boolean invalidFields = false;
                if (username.trim().equalsIgnoreCase("")) {
                    username_input.setError("This field cannot be blank");
                    invalidFields = true;
                }
                if (password.trim().equalsIgnoreCase("")) {
                    password_input.setError("This field cannot be blank");
                    invalidFields = true;
                }
                if (invalidFields) {
                    return;
                }

                //SIGN IN REQUEST
                String url = getString(R.string.base_url) + getString(R.string.sign_in_url);
                JSONObject sign_in_data = new JSONObject();
                try {
                    sign_in_data.put("name", username);
                    sign_in_data.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JsonObjectRequest sign_in_req = new JsonObjectRequest(
                        Request.Method.POST, url, sign_in_data, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String user_id = response.getString("user_id");
                            gotoSessionList(user_id);
                            Log.d(TAG, response.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String body;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                body = new String(error.networkResponse.data, "UTF-8");
                                Utils.showToast(SignIn.this, body);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.e(TAG, "Error: " + error);
                    }
                });
                SingletonRequestQueue.getInstance(SignIn.this).addToRequestQueue(sign_in_req);
            }
        });

        //SIGN UP BUTTON
        Button sign_up_btn = findViewById(R.id.sign_up_btn);
        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoSignUp();
            }
        });
    }

    @Override
    public void onBackPressed() {
    }

    private void gotoSignUp() {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SignIn.this, R.anim.slide_in_right, R.anim.slide_out_left);
        Intent intent = new Intent(SignIn.this, SignUp.class);
        startActivity(intent, options.toBundle());
    }

    private void gotoSessionList(String user_id) {
        ActivityOptions options = ActivityOptions.makeCustomAnimation(SignIn.this, R.anim.slide_in_bot, R.anim.slide_out_top);
        Intent intent = new Intent(SignIn.this, SessionList.class);
        intent.putExtra(EXTRA_USER_ID, user_id);
        startActivity(intent, options.toBundle());
    }
}