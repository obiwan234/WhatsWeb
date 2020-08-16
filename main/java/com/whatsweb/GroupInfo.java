package com.whatsweb;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class GroupInfo {

    private String name;
    private String phoneNumber;
    private String groupID;

    private Notification notificationReplyObject;

    public GroupInfo(String name, String phoneNumber, String groupID) {
        this.name=name;
        this.phoneNumber=phoneNumber;
        this.groupID=groupID;
        notificationReplyObject=null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public void addToHistory(NotificationInfo notification) {
        Notification.Action replyAction = notification.getReplyAction();
        if(replyAction!=null) {
            this.notificationReplyObject = notification.getNotificationObject();
        }
    }

    public boolean reply(String text) {
        Notification replyNotification = this.getReplyNotification();
        RemoteInput[] remoteInputs = replyNotification.actions[0].getRemoteInputs();
        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = replyNotification.extras;
        int i = 0;
        for(RemoteInput remoteIn : remoteInputs){
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), text);
            i++;
        }
        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            replyNotification.actions[0].actionIntent.send(NotificationInfo.activity.getApplicationContext(), 0, localIntent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            Log.e("error", "replyToLastNotification error: " + e.getLocalizedMessage());
            return false;
        }
    }

    public Notification getReplyNotification() {
        return this.notificationReplyObject;
    }

    public String toString() {
        return "Group Name: " + this.getName();
    }

}
