package com.whatsweb;

import android.app.Notification;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.widget.CheckBox;

import java.util.ArrayList;

public class NotificationListener extends NotificationListenerService {

    Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        MainActivity.receivedMessagesList = new ArrayList<NotificationInfo>();
        MainActivity.sentMessagesList = new ArrayList<NotificationInfo>();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Notification notificationObject = sbn.getNotification();
            String title = notificationObject.extras.getCharSequence(Notification.EXTRA_TITLE).toString();
            String text = notificationObject.extras.getCharSequence(Notification.EXTRA_TEXT).toString();
            String package_name = sbn.getPackageName();
            if(!isUselessMessage(text) && !title.contains("You") && !isUselessMessage(title)) {
                NotificationInfo newNotification = new NotificationInfo(package_name, title, text, notificationObject.when, notificationObject);
                //Log.v("title", newNotification.getTitle());
//                Log.v("text", newNotification.getText());
                if(newNotification.getApp()!=null && newNotification.getApp().equals("com.foxnews.android") &&
                        ((CheckBox)NotificationInfo.activity.findViewById(R.id.foxAlerts)).isChecked() &&
                        (MainActivity.receivedMessagesList.size() == 0 || !notificationWasSent(newNotification, MainActivity.receivedMessagesList) )) {
                    newNotification.sendToUser();
                    MainActivity.receivedMessagesList.add(newNotification);
                    if(MainActivity.receivedMessagesList.size()>=20) {
                        MainActivity.receivedMessagesList.remove(0);
                    }
                }
                if(newNotification.getApp()!=null && newNotification.getApp().equals("com.whatsapp") &&
                        (MainActivity.receivedMessagesList.size() == 0 || !notificationWasSent(newNotification, MainActivity.receivedMessagesList) )) {
                    newNotification.sendToUser();
                    MainActivity.receivedMessagesList.add(newNotification);
                    if(MainActivity.receivedMessagesList.size()>=20) {
                        MainActivity.receivedMessagesList.remove(0);
                    }
                } else if(newNotification.getApp()!=null && newNotification.getApp().equals("com.groupme.android") && !newNotification.forProgramOnly) {
                    if((MainActivity.sentMessagesList.size() == 0 || !notificationWasSent(newNotification, MainActivity.sentMessagesList))) {
                        newNotification.sendToWhatsApp();
                        MainActivity.sentMessagesList.add(newNotification);
                        if(MainActivity.sentMessagesList.size()>=20) {
                            MainActivity.sentMessagesList.remove(0);
                        }
                    }
                }
            }
            cancelAllNotifications();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isUselessMessage(String message) {
        if(message.toLowerCase().contains("backup in progress") || message.contains("new messages") ||
                message.toLowerCase().contains("logged out of whatsapp") ||
                message.contains("Ongoing voice call") || message.toLowerCase().contains("finished backup") ||
                (message.contains("messages from")&&message.contains("chats")) ||
                (message.length()>17&&message.substring(0,17).equals("Deleting messages"))) {
            return true;
        }
        return false;
    }

    public boolean notificationWasSent(NotificationInfo newNotification, ArrayList<NotificationInfo> notificationList) {//handle 2nd is replyable (replace original)
        //change to check against individual groups messages
        for(NotificationInfo notification : notificationList) {
            if(newNotification.equals(notification) && notification.wasSent) {
                return true;
            }
        }
        return false;
    }
}

