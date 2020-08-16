package com.whatsweb;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity {

    /* NOTES:
        1)add OAuth?
        2)save notification object (1 per group for reply) somewhere to reload on startup (still important?)
        3)fix delete group; create diagnostics that checks ALL api functionality
        4)detect and handle media (first from whatsapp, then from client)
            a)detect picture notification from whatsapp (with emoji); send via mms?
            b)detect voice message from whatsapp; convert opus to mp3; send via mms or phone call;
        5)add was sent to client, every time send another loop through missed ones
        6)on first launch query for username (use as gm identifier), or input on screen

       OPTIMIZATION:
        1)extend NotificationInfo into 2 separate classes (WhatsApp and GroupMe)
        2)replace group me app/notifications with url callback
    */

    public static String phoneEmail;//instead use phone number, either detected or input
    public static String phoneNumber;
    public static String userName;//group me
    public static String groupMeApiKey;
    public static String groupMeBaseUrl;
    public static ArrayList<NotificationInfo> receivedMessagesList;
    public static ArrayList<NotificationInfo> sentMessagesList;//mark as sent and when send new, loop through and send any unsent
    public static HashMap<String, GroupInfo> groupMeChats;
    public static int numGroupsUnknownName;
    NotificationListener notificationListener;
    WhatsappAccessibilityService whatsappAccessibilityService;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notificationListener = new NotificationListener();
        whatsappAccessibilityService = new WhatsappAccessibilityService();
        if (!Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
        while (!Settings.Secure.getString(this.getContentResolver(),"enabled_notification_listeners").contains(getApplicationContext().getPackageName())) {

        }
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_CONTACTS},
                        1);

        }
        while(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            //don't go to next step until contacts permission
        }
        Context context=getApplicationContext();
        if (!isAccessibilityOn (context, WhatsappAccessibilityService.class)) {
            startActivity(new Intent (Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        NotificationInfo.activity=this;
        userName="Moshe Goldberg";
        phoneEmail="7187875275@messaging.sprintpcs.com";
        phoneNumber="17187875275";
        groupMeApiKey="token=IB3lgCtQxHbXfNftJ5lS8MJemwyEKgonLDQq6uDu";//m6rfsk8wVk2ZPa10w36GL1LYcSz7bDhGIzzec470 (old)
        groupMeBaseUrl="https://api.groupme.com/v3";
        groupMeChats = new  HashMap<String, GroupInfo>();
        numGroupsUnknownName=0;
        new GroupQueryTask().execute();
    }

    public boolean isAccessibilityOn (Context context, Class<? extends AccessibilityService> clazz) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName () + "/" + clazz.getCanonicalName ();
        try {
            accessibilityEnabled = Settings.Secure.getInt (context.getApplicationContext ().getContentResolver (), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {  }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString (context.getApplicationContext().getContentResolver (), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString (settingValue);
                while (colonSplitter.hasNext ()) {
                    String accessibilityService = colonSplitter.next ();

                    if (accessibilityService.equalsIgnoreCase (service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}