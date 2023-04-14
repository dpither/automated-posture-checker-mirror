package com.example.automatedposturechecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class SessionItem {
    private String name;
    private String length;
    private String date;
    private String user_id;
    private String session_id;

    public SessionItem(String name, String length, String date, String user_id, String session_id) {
        this.name = name;
        this.length = length;
        this.date = date;
        this.user_id = user_id;
        this.session_id = session_id;
    }

    public String getName() {
        return name;
    }

    public String getLength() {
        return length;
    }

    public String getDate() {
        return date;
    }

    public String getSessionId() {
        return session_id;
    }

    public String getUserId() {
        return user_id;
    }

    public static ArrayList<SessionItem> JSONArraytoList(JSONArray array) {
        ArrayList<SessionItem> sessionList = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            String name;
            String session_id;
            String user_id;
            String length;
            String date;
            try {
                JSONObject session = array.getJSONObject(i);
                if (session.getBoolean("complete")) {
                    session_id = session.getString("session_id");
                    user_id = session.getString("user_id");
                    name = session.getString("name");
                    date = session.getString("start_time");
                    int duration_int = session.getInt("duration");
                    int hours = duration_int / 3600;
                    int minutes = (duration_int % 3600) / 60;
                    int seconds = duration_int % 60;
                    length = String.format(Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds);
                    sessionList.add(new SessionItem(name, length, date, user_id, session_id));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return sessionList;
    }
}
