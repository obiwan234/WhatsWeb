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

public class SendGroupMeTask extends  AsyncTask{

    protected Object doInBackground(Object... args) {
        JSONObject messageObject = (JSONObject) args[0];
        GroupInfo group = (GroupInfo) args[1];
        String sendMessageLink;
        try {
            sendMessageLink = MainActivity.groupMeBaseUrl + "/groups/" + group.getGroupID() + "/messages?" + MainActivity.groupMeApiKey;
            URL sendMessageUrl = new URL(sendMessageLink);
            HttpURLConnection sendMessageConnection = (HttpURLConnection) sendMessageUrl.openConnection();
            sendMessageConnection.setConnectTimeout(5000);
            sendMessageConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            sendMessageConnection.setDoOutput(true);
            sendMessageConnection.setDoInput(true);
            sendMessageConnection.setRequestMethod("POST");
            OutputStream sendMessageOutputStream = sendMessageConnection.getOutputStream();
            sendMessageOutputStream.write(messageObject.toString().getBytes("UTF-8"));
            sendMessageOutputStream.close();

            //response
            System.out.println("Status: "+sendMessageConnection.getResponseCode());
            InputStream in = sendMessageConnection.getInputStream();
            InputStream sendMessageInputStream = new BufferedInputStream(in);
            BufferedReader sendMessageStreamReader = new BufferedReader(new InputStreamReader(sendMessageInputStream, "UTF-8"));
            StringBuilder sendMessageStrBuilder = new StringBuilder();
            String sendMessageInputStr;
            while ((sendMessageInputStr = sendMessageStreamReader.readLine()) != null) {
                sendMessageStrBuilder.append(sendMessageInputStr);
            }
            JSONObject sendMessageResponse = new JSONObject(sendMessageStrBuilder.toString());
            System.out.println(sendMessageResponse.toString());

            sendMessageConnection.disconnect();
        } catch (Exception e) {
            Log.e("SendGroupMeTask", e.getMessage(), e);
        }
        return null;
    }

}

