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

public class CreateGroupTask extends AsyncTask{

    protected Object doInBackground(Object... args) {//group json, member json, name, smsnumber
        JSONObject newGroup = (JSONObject) args[0];
        JSONObject newMember = (JSONObject) args[1];
        String name = (String) args[2];
        String smsNumber = (String) args[3];
        String createGroupLink = MainActivity.groupMeBaseUrl + "/groups?" + MainActivity.groupMeApiKey;
        String addMemberLink;

        try {
            //create group
            URL newGroupUrl = new URL(createGroupLink);
            HttpURLConnection createGroupConnection = (HttpURLConnection) newGroupUrl.openConnection();
            createGroupConnection.setConnectTimeout(5000);
            createGroupConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            createGroupConnection.setDoOutput(true);
            createGroupConnection.setDoInput(true);
            createGroupConnection.setRequestMethod("POST");
            OutputStream newGroupOutputStream = createGroupConnection.getOutputStream();
            newGroupOutputStream.write(newGroup.toString().getBytes("UTF-8"));
            newGroupOutputStream.close();

            //response
            System.out.println("Status: "+createGroupConnection.getResponseCode());
            InputStream createChatInputStream = new BufferedInputStream(createGroupConnection.getInputStream());
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(createChatInputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            JSONObject newGroupResponse = new JSONObject(responseStrBuilder.toString());
            JSONObject groupObject=newGroupResponse.getJSONObject("response");
            String groupID = groupObject.getString("group_id");
            createGroupConnection.disconnect();

            //add member
            addMemberLink = MainActivity.groupMeBaseUrl + "/groups/" + groupID + "/members/add?" + MainActivity.groupMeApiKey;
            URL addMemberUrl = new URL(addMemberLink);
            HttpURLConnection addMemberConnection = (HttpURLConnection) addMemberUrl.openConnection();
            addMemberConnection.setConnectTimeout(5000);
            addMemberConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            addMemberConnection.setDoOutput(true);
            addMemberConnection.setDoInput(true);
            addMemberConnection.setRequestMethod("POST");
            OutputStream addMemberOutputStream = addMemberConnection.getOutputStream();
            addMemberOutputStream.write(newMember.toString().getBytes("UTF-8"));
            addMemberOutputStream.close();

            System.out.println("Status: "+addMemberConnection.getResponseCode());
            InputStream addMemberInputStream = new BufferedInputStream(addMemberConnection.getInputStream());
            BufferedReader addMemberStreamReader = new BufferedReader(new InputStreamReader(addMemberInputStream, "UTF-8"));
            StringBuilder addMemberStrBuilder = new StringBuilder();
            String addMemberInputStr;
            while ((addMemberInputStr = addMemberStreamReader.readLine()) != null) {
                addMemberStrBuilder.append(addMemberInputStr);
            }

            addMemberConnection.disconnect();

            MainActivity.groupMeChats.put(name,new GroupInfo(name,smsNumber,groupID));

        } catch (Exception e) {
            Log.e("CreateGroupTask", e.getMessage(), e);
        }
        return null;
    }

}

