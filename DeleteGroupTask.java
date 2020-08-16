package com.whatsweb;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeleteGroupTask extends AsyncTask{

    protected Object doInBackground(Object... args) {//group
        GroupInfo groupObject = (GroupInfo) args[0];
        String deleteGroupLink = MainActivity.groupMeBaseUrl + "/groups/" + groupObject.getGroupID() + "/destroy?" + MainActivity.groupMeApiKey;
        try {
            //delete group
            URL deleteGroupUrl = new URL(deleteGroupLink);
            HttpURLConnection deleteGroupConnection = (HttpURLConnection) deleteGroupUrl.openConnection();
            deleteGroupConnection.setConnectTimeout(5000);
            deleteGroupConnection.setRequestMethod("POST");
            System.out.println("Status: "+deleteGroupConnection.getResponseCode());
            deleteGroupConnection.disconnect();
            //remove from group list
            MainActivity.groupMeChats.remove(groupObject.getName());
        } catch (Exception e) {
            Log.e("DeleteGroupTask", e.getMessage(), e);
        }
        return null;
    }

}

