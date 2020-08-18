package com.whatsweb;

import android.app.Notification;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;

public class GroupQueryTask extends AsyncTask{

    protected Object doInBackground(Object... args) {
        try {
            Log.v("Username",MainActivity.userName);
            Log.v("Phone Number", MainActivity.phoneNumber);
            Log.v("Phone Email", MainActivity.phoneEmail);
            String oldUser = MainActivity.userName;
            HashMap<String,GroupInfo> oldGroups= (HashMap<String,GroupInfo>) MainActivity.groupMeChats.clone();
            MainActivity.groupMeChats.clear();
            String groupMeGroupQuery = MainActivity.groupMeBaseUrl + "/groups?" + MainActivity.groupMeApiKey;
            URL groupQueryUrl = new URL(groupMeGroupQuery);
            HttpURLConnection groupQueryConnection = (HttpURLConnection) groupQueryUrl.openConnection();
            JSONObject groupQueryJSON;
            try {
                System.out.println("Status: "+groupQueryConnection.getResponseCode());
                InputStream groupQueryInputStream = new BufferedInputStream(groupQueryConnection.getInputStream());
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(groupQueryInputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                groupQueryJSON = new JSONObject(responseStrBuilder.toString());
            } finally {
                groupQueryConnection.disconnect();
            }
            JSONArray groupArray=groupQueryJSON.getJSONArray("response");
            for(int i = 0; i<groupArray.length(); i++) {
                JSONObject group = (JSONObject) groupArray.get(i);
                String groupName = group.getString("name");
                String groupDescription = group.getString("description");
                int numMembers = group.getJSONArray("members").length();
                int uselessLengthDescription = ("Communicate with ").length();
                String phoneNumber = groupDescription.substring(uselessLengthDescription);
                if(groupName.contains(MainActivity.userName + " WhatsWeb with ") && numMembers>1) {
                    int uselessLengthName = (MainActivity.userName + " WhatsWeb with ").length();
                    String groupWith = groupName.substring(uselessLengthName);
                    MainActivity.groupMeChats.put(groupWith,new GroupInfo(groupWith,phoneNumber,group.getString("group_id")));
                }
            }
            for(String groupWithName : MainActivity.groupMeChats.keySet()) {
                if(groupWithName.contains("Unknown")) {
                    MainActivity.numGroupsUnknownName++;
                }
            }
            //if no groups found, create one here (hardcoded because can't call AsyncTask from another AsyncTask)
            if (MainActivity.groupMeChats.size() == 0) {
                //new group objects
                JSONObject newGroup = new JSONObject();
                newGroup.put("name", MainActivity.userName + " WhatsWeb with " + MainActivity.userName);
                newGroup.put("description", "Communicate with " + MainActivity.phoneNumber);
                JSONObject clientMember = new JSONObject();
                clientMember.put("nickname", MainActivity.userName);
                clientMember.put("phone_number", "+1 " + MainActivity.phoneNumber.substring(1));
                JSONObject newMember = new JSONObject();
                JSONArray membersArray = new JSONArray();
                membersArray.put(clientMember);
                newMember.put("members", membersArray);

                //create group
                String name = MainActivity.userName;
                String smsNumber = MainActivity.phoneNumber;
                String createGroupLink = MainActivity.groupMeBaseUrl + "/groups?" + MainActivity.groupMeApiKey;
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
                String addMemberLink = MainActivity.groupMeBaseUrl + "/groups/" + groupID + "/members/add?" + MainActivity.groupMeApiKey;
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
            }
            //copy over replies
            if(oldGroups.size()>0 && MainActivity.userName.equals(oldUser)) { //if same groupMe user
                for(String groupName : MainActivity.groupMeChats.keySet()) {
                    if(oldGroups.get(groupName)!=null) {
                        Notification replyNotification = oldGroups.get(groupName).getNotificationReplyObject();
                        MainActivity.groupMeChats.get(groupName).setNotificationReplyObject(replyNotification);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("GroupQueryTask", e.getMessage(), e);
        }
        return null;
    }

}

