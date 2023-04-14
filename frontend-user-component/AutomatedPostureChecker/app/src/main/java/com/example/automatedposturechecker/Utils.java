package com.example.automatedposturechecker;


import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static final String EXTRA_USER_ID = "com.example.automatedposturechecker.EXTRA_USER_ID";
    public static final String EXTRA_SESSION_ID = "com.example.automatedposturechecker.EXTRA_SESSION_ID";
    public static final String EXTRA_SESSION_NAME = "com.example.automatedposturechecker.EXTRA_SESSION_NAME";
    public static final String EXTRA_NOTIFICATION_TYPE = "com.example.automatedposturechecker.EXTRA_NOTIFICATION_TYPE";
    public static final String EXTRA_STRETCH_REMINDER = "com.example.automatedposturechecker.EXTRA_STRETCH_REMINDER";
    public static final String EXTRA_BAD_POSTURE_THRESHOLD = "com.example.automatedposturechecker.EXTRA_BAD_POSTURE_THRESHOLD";
    public static final String EXTRA_LAST_IMAGE_ID = "com.example.automatedposturechecker.EXTRA_LAST_IMAGE_ID";
    public static final int NOTIFICATION_SOUND = 0;
    public static final int NOTIFICATION_VIBRATE = 1;
    public static final int NOTIFICATION_MUTE = 2;

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[s.length() / 2];
        for (int i = 0; i < data.length; i++) {
            int index = i * 2;
            int val = Integer.parseInt(s.substring(len-index-2, len-index), 16);
            data[i] = (byte) val;
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] byteArray) {
        StringBuffer hexStringBuffer = new StringBuffer();
        for (int i = 0; i < byteArray.length; i++) {
            hexStringBuffer.append(byteToHex(byteArray[i]));
        }
        return hexStringBuffer.toString();
    }

    public static String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);
        return new String(hexDigits);
    }

    public static void showToast(Context context, String string) {
        Toast.makeText(context, string, Toast.LENGTH_LONG).show();
    }
}
