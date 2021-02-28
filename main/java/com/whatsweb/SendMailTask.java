package com.whatsweb;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class SendMailTask extends AsyncTask {

    protected Object doInBackground(Object... args) {
        try {
            GMail androidEmail = new GMail(args[0].toString(),
                    args[1].toString(), args[2].toString(), args[3].toString(),
                    args[4].toString(), args[5].toString() ,(ArrayList<File>) args[6]);
            androidEmail.createEmailMessage();
            androidEmail.sendEmail();
        } catch (Exception e) {
            Log.e("SendMailTask", e.getMessage(), e);
        }
        return null;
    }

}

