package com.whatsweb;

import android.app.Notification;
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

public class GroupMeWrapper {
    //to be called from an in-between Async Task

    public GroupMeWrapper() {

    }

    public static GroupInfo createGroup(String groupWith, String smsNumber) {
        try {
            //create objects
            JSONObject newGroup = new JSONObject();
            newGroup.put("name", MainActivity.userName + " WhatsWeb with " + groupWith);
            newGroup.put("description", "Communicate with " + smsNumber);
            String createGroupLink = MainActivity.groupMeBaseUrl + "/groups?" + MainActivity.groupMeApiKey;

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

            GroupInfo group = new GroupInfo(groupWith,smsNumber,groupID);
            MainActivity.groupMeChats.put(groupWith,group);
            return group;

        } catch (Exception e) {
            Log.e("Create Group Failure", e.getMessage(), e);
        }
        return null;
    }

    public static void addMember(String groupID, String nickname, String smsNumber) {
        try {
            JSONObject clientMember = new JSONObject();
            clientMember.put("nickname", nickname);
            clientMember.put("phone_number", smsNumber);
            JSONObject newMember = new JSONObject();
            JSONArray membersArray = new JSONArray();
            membersArray.put(clientMember);
            newMember.put("members", membersArray);

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
        } catch (Exception e) {
            Log.e("Add Member Failure",e.getMessage(),e);
        }
    }

    public static void delete(GroupInfo group) {
        try{
            String deleteGroupLink = MainActivity.groupMeBaseUrl + "/groups/" + group.getGroupID() + "/destroy?" + MainActivity.groupMeApiKey;
            //delete group
            URL deleteGroupUrl = new URL(deleteGroupLink);
            HttpURLConnection deleteGroupConnection = (HttpURLConnection) deleteGroupUrl.openConnection();
            deleteGroupConnection.setConnectTimeout(5000);
            deleteGroupConnection.setRequestMethod("POST");
            System.out.println("Status: "+deleteGroupConnection.getResponseCode());
            deleteGroupConnection.disconnect();
            //remove from group list
            MainActivity.groupMeChats.remove(group.getName());
        } catch (Exception e) {
            Log.e("Delete Group Failure",e.getMessage(),e);
        }
    }

    public static void query() {
        try {
            String oldUser = MainActivity.userName;
            HashMap<String,GroupInfo> oldGroups= (HashMap<String,GroupInfo>) MainActivity.groupMeChats.clone();
            MainActivity.groupMeChats.clear();
            String groupMeGroupQuery = MainActivity.groupMeBaseUrl + "/groups?per_page=" + MainActivity.overestimateNumGroups + "&"
                    + MainActivity.groupMeApiKey;
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

            //copy over replies
            if(oldGroups.size()>0 && MainActivity.userName.equals(oldUser)) { //if same groupMe user
                for(String groupName : MainActivity.groupMeChats.keySet()) {
                    if(oldGroups.get(groupName)!=null) {
                        Notification replyNotification = oldGroups.get(groupName).getNotificationReplyObject();
                        MainActivity.groupMeChats.get(groupName).setNotificationReplyObject(replyNotification);
                    }
                }
            }
            Log.v("NUM GROUPS",""+MainActivity.groupMeChats.size());
        } catch (Exception e) {
            Log.e("Query Failure", e.getMessage(), e);
        }
    }

    public static void sendMessage(String groupID, String source_guid, String message) {
        try {
            JSONObject tempMessageObject = new JSONObject();
            tempMessageObject.put("source_guid", source_guid);
            tempMessageObject.put("text", message);
            JSONObject messageObject = new JSONObject();
            messageObject.put("message", tempMessageObject);

            String sendMessageLink = MainActivity.groupMeBaseUrl + "/groups/" + groupID + "/messages?" + MainActivity.groupMeApiKey;
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
            Log.e("Send GroupMe Failure", e.getMessage(), e);
        }
    }

}
