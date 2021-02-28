package com.whatsweb;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Random;

public class SendDefinitionTask extends AsyncTask{

    public String currQuery;

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected Object doInBackground(Object... args) {
            try {
                String query = ((String) args[0]).toLowerCase();
                String definition = getDefinition(query);
                GroupInfo group = (GroupInfo) args[1];
                String when = (String) args[2];
                String replyString = currQuery+"\n"+definition;
                Log.v("HERE",replyString);
                sendMessage(replyString,group,when);
            }
            catch (Exception e) {
                Log.e("SendDefinitionTask", e.getMessage(), e);
            }
        return null;
    }

    public String getDefinition(String query) throws JSONException {
        currQuery=query;
        JSONArray definitionJSON = new JSONArray();
        try {
            String definitionLink = "https://www.dictionaryapi.com/api/v3/references/collegiate/json/" +
                    query.replace(" ", "%20") +
                    "?key=2c6908e7-265b-45f6-8a51-9dc12c020087";
            URL definitionLinkURL = new URL(definitionLink);
            BufferedInputStream definitionInputStream = new BufferedInputStream(definitionLinkURL.openStream());
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(definitionInputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            definitionJSON = new JSONArray(responseStrBuilder.toString());
            Log.v("HERE", definitionJSON.toString());
            if (definitionJSON.get(0) instanceof JSONObject) { //defined
                return definitionJSON.getJSONObject(0).getJSONArray("shortdef").getString(0);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return getDefinition(definitionJSON.getString(0));
    }

    public void sendMessage(String message, GroupInfo group, String when) {
        try {
            JSONObject messageObjectNested = new JSONObject();
            messageObjectNested.put("source_guid", when);
            messageObjectNested.put("text", message);
            JSONObject messageObject = new JSONObject();
            messageObject.put("message", messageObjectNested);

            String sendMessageLink;
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
    }

}

