package com.whatsweb;

import android.os.AsyncTask;
import android.util.Log;

public class GroupMeTask extends AsyncTask {

    protected Object doInBackground(Object... args) {
        try {
            String function = (String)args[0];
            GroupInfo group;
            switch(function) {
                case "createGroup":
                    group = GroupMeWrapper.createGroup((String)args[1], (String)args[2]);
                    GroupMeWrapper.addMember(group.getGroupID(),MainActivity.userName,"+1 "+ MainActivity.phoneNumber.substring(1));
                    break;
                case "addMember":
                    GroupMeWrapper.addMember((String)args[1], (String)args[2], (String)args[3]);
                    break;
                case "delete":
                    GroupMeWrapper.delete((GroupInfo)args[1]);
                    break;
                case "sendMessage":
                    GroupMeWrapper.sendMessage((String)args[1], (String)args[2], (String)args[3]);
                    break;
                case "query":
                    GroupMeWrapper.query();
                    if(MainActivity.groupMeChats.size() == 0) {
                        group = GroupMeWrapper.createGroup(MainActivity.userName,MainActivity.phoneNumber);
                        GroupMeWrapper.addMember(group.getGroupID(),MainActivity.userName,"+1 "+ MainActivity.phoneNumber.substring(1));
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e("GroupMe Task Failure", e.getMessage(), e);
        }
        return null;
    }
}
