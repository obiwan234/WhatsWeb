package com.whatsweb;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity {

    /*
       BUGS:
        1)Crashes when Fox Notification is picture
        2)Pictures are sent multiple times (often by wrong sender)

       NOTES:
        !)use date to track images that client has received and not received
        *)compress images before sending
        0)Convert all names to Proper Case
        1)add OAuth?
        2)save notification object (1 per group for reply) somewhere to reload on startup (still important?)
        3)create diagnostics that checks ALL api functionality (with test account, message user where fail in catch)
        4)detect and handle media (first from whatsapp, then from client)
            a)detect voice message from whatsapp; convert opus to mp3; send via mms or phone call;
        5)add was sent to client, every time send another loop through missed ones
        6)save username and info after first login

       OPTIMIZATION:
        1)extend NotificationInfo into 2 separate classes (WhatsApp and GroupMe)
        2)replace group me app/notifications with url callback
        3)put api actions in regular classes to be called by AsyncTasks
    */

    public static final int overestimateNumGroups = 500;

    public static String phoneEmail;//instead use phone number, either detected or input
    public static String phoneNumber;
    public static String userName;//group me
    public static String groupMeApiKey;
    public static String groupMeBaseUrl="https://api.groupme.com/v3";
    public static ArrayList<NotificationInfo> receivedMessagesList;
    public static ArrayList<NotificationInfo> sentMessagesList;//mark as sent and when send new, loop through and send any unsent
    public static ArrayList<String> sentImageList;
    public static ArrayList<String> sentVoiceNoteList;
    public static HashMap<String, GroupInfo> groupMeChats;
    public static int numGroupsUnknownName;
    NotificationListener notificationListener;
    WhatsappAccessibilityService whatsappAccessibilityService;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor infoEditor;


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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);

        }
        while(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //don't go to next step until read files permission
        }
        Context context=getApplicationContext();
        if (!isAccessibilityOn (context, WhatsappAccessibilityService.class)) {
            startActivity(new Intent (Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        NotificationInfo.activity=this;
        sharedPreferences = getPreferences(MODE_PRIVATE);
        infoEditor = sharedPreferences.edit();

        //userName="Moshe Goldberg";
        userName=sharedPreferences.getString("userName","Moshe Goldberg");
        System.out.println("username: "+userName);
        //phoneEmail="7187875275@pm.sprint.com";
        phoneEmail=sharedPreferences.getString("phoneEmail","7187875275@pm.sprint.com");
        System.out.println("phone email: "+phoneEmail);
        //phoneNumber="17187875275";
        phoneNumber=sharedPreferences.getString("phoneNumber","17187875275");
        System.out.println("phone number: "+phoneNumber);
        //groupMeApiKey="token=fs1IgMLDI3e93WKOD4sIxR8RCSrGFTaKy6Sf7Sno";//m6rfsk8wVk2ZPa10w36GL1LYcSz7bDhGIzzec470 (old)
                        // NT7QDhcg7zXwTgdTlzqDKW4uy2TAuRkhHztn6KUP //iAepUpNi0hE3bR6sBS0gfrceTnXVNBODWUDDo5Bk
                        //nNyeyflMOvU1jMwrYyMeigVEQcCcguRddBTYicQG //FKJ9maSTIPFN9x3orpiFB8lYmeKiHCBs2gte7Xvg
                        //zA7ZgvZ3kfUMBQKWyH2M9bsQ99i7Jsn1Uk45KDCr //vRQrMNNg6PBoaSbUB9Fe0Vij9DqrceTDBoEpVXdd
        groupMeApiKey="token="+sharedPreferences.getString("groupMeApiKey","fs1IgMLDI3e93WKOD4sIxR8RCSrGFTaKy6Sf7Sno");
        System.out.println("api key: "+groupMeApiKey);


        groupMeChats = new  HashMap<String, GroupInfo>();
        numGroupsUnknownName=0;
        sentImageList = new ArrayList<String>();
        sentVoiceNoteList = new ArrayList<String>();
        File imageDirectory = new File(Environment.getExternalStorageDirectory().getPath()+"/WhatsApp/Media/WhatsApp Images");
        ArrayList<File> imageFiles = getAllFiles(imageDirectory);
        if (imageFiles!=null && imageFiles.size()!=0) {
            for (File imageFile : imageFiles) {
                sentImageList.add(imageFile.getPath());
            }
        }
        File voiceNoteDirectory = new File(Environment.getExternalStorageDirectory().getPath()+"/WhatsApp/Media/WhatsApp Voice Notes");
        ArrayList<File> voiceNoteFiles = getAllFiles(voiceNoteDirectory);
        if (voiceNoteFiles!=null && voiceNoteFiles.size()!=0) {
            for (File voiceNoteFile : voiceNoteFiles) {
                sentVoiceNoteList.add(voiceNoteFile.getPath());
            }
        }
        //new GroupQueryTask().execute();
        new GroupMeTask().execute("query");
    }

    public void getUserFields() {
        boolean isNewInfo = false;
        EditText inputName = findViewById(R.id.editTextTextPersonName2);
        String name = inputName.getText().toString();
        EditText inputPhone = findViewById(R.id.editTextPhone2);
        String phone = inputPhone.getText().toString();
        EditText inputEmail = findViewById(R.id.editTextTextEmailAddress2);
        String email = inputEmail.getText().toString();
        EditText inputApiKey = findViewById(R.id.editTextApiKey);
        String apiKey = inputApiKey.getText().toString();
        if(name!=null && !name.equals(getString(R.string.name)) && name.trim().length()>0 && !name.equals(userName)) {
            isNewInfo=true;
            userName=name;
            infoEditor.putString("userName", userName);
            infoEditor.commit();
        }
        if(phone!=null && !phone.equals(getString(R.string.phone)) && phone.trim().length()>0 && !phone.equals(phoneNumber)) {
            isNewInfo=true;
            phoneNumber=phone;
            if(phoneNumber.length()==10) {
                phoneNumber = "1" + phoneNumber;
            }
            infoEditor.putString("phoneNumber", phoneNumber);
            infoEditor.commit();
        }
        if(email!=null && !email.equals(getString(R.string.phone_mms_email)) && email.trim().length()>0 && !email.equals(phoneEmail)) {

            isNewInfo=true;
            phoneEmail=email;
            infoEditor.putString("phoneEmail", phoneEmail);
            infoEditor.commit();
        }
        if(apiKey!=null && !apiKey.equals(getString(R.string.api_key)) && apiKey.trim().length()>0 && !apiKey.equals("token="+groupMeApiKey)) {
            isNewInfo=true;
            groupMeApiKey="token="+apiKey;
            infoEditor.putString("groupMeApiKey", apiKey);
            infoEditor.commit();
        }
        if(isNewInfo) {
            new GroupMeTask().execute("query");

            System.out.println("username: "+userName);
            System.out.println("phone email: "+phoneEmail);
            System.out.println("phone number: "+phoneNumber);
            System.out.println("api key: "+groupMeApiKey);
        }
        inputName.getText().clear();
        inputPhone.getText().clear();
        inputEmail.getText().clear();
        inputApiKey.getText().clear();
    }

    public void getUserFields(View view) {
        getUserFields();
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

    public ArrayList<File> getAllFiles(File filePath) {
        ArrayList<File> allFiles = new ArrayList<File>();
        if(filePath==null || filePath.isFile()) {
            allFiles.add(filePath);
        } else if(filePath.isDirectory()){
            for (File subFilePath : filePath.listFiles()) {
                allFiles.addAll(getAllFiles(subFilePath));
            }
        }
        return  allFiles;
    }
}