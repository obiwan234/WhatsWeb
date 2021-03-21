package com.whatsweb;

import android.app.Activity;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.CheckBox;

import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class NotificationInfo {

    private String title;
    private String text;
    private String app;
    private long when;
    private Notification.Action replyAction;
    private Notification notificationObject;

    private String to;
    private String from;
    private GroupInfo groupObject;
    public boolean wasSent = false;
    public boolean forProgramOnly = false;
    public boolean isPicture = false;
    public boolean isVoiceNote = false;
    public boolean sendConfirmation = false;
    private String caption;

    public static Activity activity;

    public  NotificationInfo(String app, String title, String text, long when, Notification notificationObject) {
        try {
            this.title = fixString(title);
            this.text = fixString(text);
            this.app = app;
            this.when = when;
            this.notificationObject = notificationObject;
            if (app.equals("com.whatsapp")) {//handle phone num not in contacts
                //save reply object
                if (notificationObject.actions != null) {
                    Notification.Action reply = notificationObject.actions[0];
                    this.setReplyAction(reply);
                }
                this.isPicture = this.text.indexOf(String.valueOf("\uD83D\uDCF7")) == 0;
                this.isVoiceNote = this.text.indexOf(String.valueOf("\uD83C\uDFA4")) == 0;
                //parse group chat title
                if (title.contains(":")) {
                    int colonIndex = this.title.indexOf(":");
                    String sender = this.title.substring(colonIndex + 2);
                    this.text = sender + ": " + this.text;
                    this.title = this.title.substring(0, colonIndex);
                    if (this.title.contains("messages)")) {
                        this.title = this.title.substring(0, this.title.lastIndexOf("("));
                    }
                    //sometimes group messages register with last char as " "
                    if (this.title.charAt(this.title.length() - 1) == ' ') {
                        this.title = this.title.substring(0, this.title.length() - 1);
                    }
                }
                String contactsSms = getContactsNumber(this.getTitle());
                this.groupObject = getGroup(this.getTitle(), contactsSms);
                if (groupObject == null) {
                    if (contactsSms == null) {
                        contactsSms = this.title;
                    }
                    createNewGroup(this.title, contactsSms);
                    Thread.sleep(2500);
                    this.groupObject = getGroup(this.getTitle(), contactsSms);
                    String firstMessage = "This group connects to the WhatsApp chat, \""
                            + this.getTitle() + "\". The first text you send on this chat will not be sent to WhatsApp.";
                    sendGroupMeMessage(firstMessage, this.groupObject);
                }
                //handle media: (📷 is "\uD83D\uDCF7")
                if (this.isPicture) {
                    this.caption = this.text.substring(0, this.text.indexOf(String.valueOf("\uD83D\uDCF7")))
                            + this.text.substring(this.text.indexOf(String.valueOf("\uD83D\uDCF7")) + 3);//emoji is 2 and space that follows is one more
                    System.out.println(this.caption);
                    this.text = "(Picture) " + this.caption;//add message here
                }
                //(🎤 is "\uD83C\uDFA4")
                if (this.isVoiceNote) {
                    String voiceNoteLength = this.text.substring(this.text.indexOf("(")+1,this.text.indexOf(")"));
                    this.text = "(Voice Note) " + voiceNoteLength;
                    this.caption = this.text;
                }
            } else if (app.equals("com.groupme.android")) {
                if (!this.getTitle().contains(MainActivity.userName + " WhatsWeb with")) {
                    this.wasSent = true;//message not intended for this client
                }
                this.text = this.getText().substring(this.getText().indexOf(':') + 2);//their name in chat isn't important
                /////////////////parse////////////////////////
                int uselessLengthName = (MainActivity.userName + " WhatsWeb with ").length();
                this.setTitle(this.getTitle().substring(uselessLengthName));
                this.groupObject = getGroup(this.getTitle());
                String smsNumber = null;
                if (this.groupObject != null) {
                    smsNumber = groupObject.getPhoneNumber();
                }
                if (smsNumber != null && smsNumber.length() == 10) {
                    smsNumber = "1" + smsNumber;
                }
                String message = this.getText();
                if (this.getText().length() > 1 && this.getText().charAt(0) == '@') {
                    String possibleMessage = isMeantForProgram(this.getText().substring(1));
                    if (possibleMessage != null) {
                        this.setText(possibleMessage);
                        this.forProgramOnly = true;
                        handleAsProgramMessage();
                    }
                }
                if(this.getTitle().toLowerCase().contains("supreme court archive")) {
                    this.forProgramOnly=true;
                    new SendCaseTask().execute(this.getText());
                    this.setText("Processing Request \""+this.getText()+"\"");
                    this.sendToUser();
                }
                if(this.getTitle().toLowerCase().contains("word search")) {
                    this.groupObject = getGroup("Word Search");
                    this.forProgramOnly=true;
                    new SendDefinitionTask().execute(this.getText(),this.groupObject,Long.toString(this.getWhen()));
                }
                if (this.getText().length() > 1 && this.getText().charAt(0) == '@' && !forProgramOnly) {
                    String[] textAsArray = this.getText().substring(1).split(" ", -1);
                    String name = "";
                    for (String wordOfText : textAsArray) {
                        if (isPhoneNumber(wordOfText)) {
                            smsNumber = wordOfText;
                            break;
                        } else {
                            name += wordOfText + " ";
                        }
                    }
                    if (name.length() >= 1) {
                        //get rid of extra space added at end
                        name = name.substring(0, name.length() - 1);
                    }
                    //exceptions
                    if (Arrays.equals(name.split(" ", -1), textAsArray)) {
                        //if no number
                        if (name.contains("(") && name.contains(")")) {
                            name = name.substring(name.indexOf("("), name.indexOf(")") + 1);
                        }
                        if (name.contains("[") && name.contains("]")) {
                            name = name.substring(name.indexOf("["), name.indexOf("]") + 1);
                        }
                    }
                    if (name.equals("")) {
                        //if no name before number
                        name = "Unknown " + (MainActivity.numGroupsUnknownName + 1);
                    }
                    message = message.substring(message.indexOf(smsNumber) + smsNumber.length());
                    this.setText(message);
                    if ((name.charAt(0) == '(' && name.charAt(name.length() - 1) == ')') || (name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']')) {
                        //take ()[] out of name (kept out of message earlier)
                        name = name.substring(1, name.length() - 1);
                        String potentialSMS = getContactsNumber(name);
                        if (potentialSMS != null) {
                            smsNumber = potentialSMS;
                        } else {
                            smsNumber = name;
                        }
                    }
                    //take out dashes
                    if (smsNumber.length() == 10) {
                        smsNumber = "1" + smsNumber;
                    }
                    GroupInfo possibleGroup = getGroup(name, smsNumber);
                    if (possibleGroup != null) {
                        this.groupObject = possibleGroup;
                    } else {
                        createNewGroup(name, smsNumber);
                        Thread.sleep(2500);
                        this.groupObject = getGroup(name, smsNumber);
                        String firstMessage = "This group connects to the WhatsApp chat, \""
                                + name + "\". The first text you send on this chat will not be sent to WhatsApp.";
                        sendGroupMeMessage(firstMessage, this.groupObject);
                    }
                }
                if(this.getText().contains("(*)") && this.getText().trim().indexOf("(*)") == 0) {
                    this.sendConfirmation = true;
                    System.out.println(this.text.replace("(*)",""));
                    this.setText(this.text.replace("(*)",""));
                    System.out.println(this.text);
                }
                if (!forProgramOnly) {
                    this.setTo(smsNumber);
                    if(!this.sendConfirmation) {
                        this.setText(message); //does this do anything?
                    }
                }
            } else if (app.equals("com.foxnews.android")) {
                this.text = this.title + "\n" + this.text;
                this.groupObject = getGroup("Fox News");
                if (groupObject == null && ((CheckBox) activity.findViewById(R.id.foxAlerts)).isChecked()) {
                    createNewGroup("Fox News", "Fox News");
                    Thread.sleep(3500);
                    this.groupObject = getGroup("Fox News");
                    String firstMessage = "This group is for Fox News Alerts";
                    sendGroupMeMessage(firstMessage, this.groupObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAsWhatsAppImage() throws InterruptedException {
        String fromName = this.title;
        String toEmail = MainActivity.phoneEmail;
        String emailSubject = this.caption;
        String emailBody = "";

        Thread.sleep(2000);//wait for download
        File directory = new File(Environment.getExternalStorageDirectory().getPath()+"/WhatsApp/Media/WhatsApp Images");
        ArrayList<File> files = getAllFiles(directory);
        System.out.println("Num Files: " + files.size());
        ArrayList<File> imageBatch = new ArrayList<File>();
        if (files!=null && files.size()!=0) {
            for (File file : files) {
                if(!MainActivity.sentImageList.contains(file.getPath())) {
                    imageBatch.add(file);
                    MainActivity.sentImageList.add(file.getPath());
                }
            }
        }
        if(imageBatch.size()!=0) {
            System.out.println("Image Batch Size: " + imageBatch.size());
            new SendMailTask().execute(fromName, toEmail, emailSubject, emailBody, imageBatch, false);
        }
    }

    private void handleAsWhatsAppVoiceNote() throws InterruptedException {
        String fromName = this.title;
        String toEmail = MainActivity.phoneEmail;
        String emailSubject = this.caption;
        String emailBody = "";

        Thread.sleep(3500);//wait for download
        File directory = new File(Environment.getExternalStorageDirectory().getPath()+"/WhatsApp/Media/WhatsApp Voice Notes");
        ArrayList<File> files = getAllFiles(directory);
        System.out.println("Num Files: " + files.size());
        ArrayList<File> voiceNoteBatch = new ArrayList<File>();
        if (files!=null && files.size()!=0) {
            for (File file : files) {
                if(!MainActivity.sentVoiceNoteList.contains(file.getPath())) {
                    System.out.println(file.getPath());
                    voiceNoteBatch.add(file);
                    MainActivity.sentVoiceNoteList.add(file.getPath());
                }
            }
        }
        //convert
        System.out.println("start conversion");
        ArrayList<File> convertedVoiceNoteBatch = new ArrayList<File>();
        int counter=0;
        for(File opusVoiceNote : voiceNoteBatch) {
            System.out.println("Loop Number: " + counter);
            try {
                //////////////////////////////

                //////////////////////////////
                 counter++;
            } catch (Exception e) {
                Log.e("Conversion Failure", "Error: failure attempting to read " + opusVoiceNote.getPath() + "!");
                return;
            }

            if(voiceNoteBatch.size()!=0) {
                if(convertedVoiceNoteBatch.size()>0) {//conversion successful
                    new SendMailTask().execute(fromName, toEmail, emailSubject, emailBody, convertedVoiceNoteBatch, true);
                } else {
                    new SendMailTask().execute(fromName, toEmail, emailSubject, emailBody, voiceNoteBatch, true);
                }
            }

        }
    }

//    //conversion helper methods//////////////////

//    /////////////////////////////////////////////



    private void handleAsProgramMessage() throws JSONException, InterruptedException {
        String queryMessage = this.getText();
        switch (queryMessage) {
            case "stay": case "here":
                this.setText("Your messages will now be sent to this chat.");
                break;
            case "test":
                this.setText("WhatsWeb is working.");
                break;
            case "diagnostics": case "info":
                //include more info
                this.setText("Program is running. You have " + MainActivity.groupMeChats.size() + " WhatsWeb chats.");
                break;
            case "awake":
                this.setText("Wide awake, in fact. Have a good morning (or whatever time of day it is).");
                break;
            case "respond":
                this.setText("response.");
                break;
            case "ping":
                this.setText("Pong!");
                break;
            case "getgroup": case "getgroupname": case "groupname": case "getname":
                this.setText("This group connects you with the WhatsApp chat, \"" + this.groupObject.getName() + "\".");
                break;
            case "refresh": case "refreshchats": case "refreshgroups":
                new GroupMeTask().execute("query");
                this.setText("Refreshing...");
                break;
            case "deletegroup": case "delete": case "deletechat": case "destroy":
                System.out.println("Deleting chat");
                new GroupMeTask().execute("delete", this.groupObject);
                Thread.sleep(5000);
                new GroupMeTask().execute("query");
                break;
        }
        if(!queryMessage.equals("deletegroup") && !queryMessage.equals("delete") && !queryMessage.equals("deletechat")) {
            this.sendToUser();
        }
    }

    private String isMeantForProgram(String message) {
        message = message.toLowerCase();
        String tempMessage = "";
        for(int i = 0; i<message.length(); i++) {
            if("abcdefghijklmnopqrstuvwxyz1234567890".contains(Character.toString(message.charAt(i)))) {
                tempMessage+=message.charAt(i);
            }
        }
        message=tempMessage;
        //add change name and number
        if(message.equals("deletegroup") || message.equals("deletechat") || message.equals("delete") || message.equals("test")
                || message.equals("diagnostics") || message.equals("info") || message.equals("destroy")
                || message.equals("awake") || message.equals("respond") || message.equals("ping")
                || message.equals("refresh") || message.equals("refreshchats") || message.equals("refreshgroups")
                || message.equals("getgroup") || message.equals("getgroupname") || message.equals("groupname") || message.equals("getname")
                || message.equals("stay") || message.equals("here")) {
            return message;
        }
        return null;
    }

    private String getContactsNumber(String title) {
        ContentResolver contentResolver = activity.getContentResolver();
        //String[] projection = new String[] {ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = activity.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            try{
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (Integer.parseInt(hasPhone) > 0 && name.equalsIgnoreCase(title)) {
                    Cursor phones = contentResolver.query( ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId, null, null);
                    while (phones.moveToNext()) {
                        String phoneNumber = phones.getString(phones.getColumnIndex( ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if(phoneNumber==null) {
                            continue;
                        }
                        //strip +-() that may be in number
                        String tempNum = "";
                        for(int i=0; i<phoneNumber.length(); i++) {
                            if("0123456789".contains(Character.toString(phoneNumber.charAt(i)))) {
                                tempNum+=Character.toString(phoneNumber.charAt(i));
                            }
                        }
                        phoneNumber = tempNum;
                        if(phoneNumber.length()==10) {
                            phoneNumber = "1" + phoneNumber;
                        }
                        Log.v("Found phone in contacts",phoneNumber);
                        return  phoneNumber;
                    }
                    phones.close();
                }
            }catch(Exception e){
                return null;
            }
        }

        return null;
    }

    private GroupInfo getGroup(String name, String sms) {
        if(sms==null) {
            return getGroup(name);
        }
        GroupInfo groupBySMS = getSmsInChat(sms);
        if(groupBySMS!=null) {
            return groupBySMS;
        }
        return getGroup(name);
    }

    private GroupInfo getGroup(String name) {
        //find CLOSEST if more than one match contain criteria!!!
        for(GroupInfo group : MainActivity.groupMeChats.values()) {
            if(name.toLowerCase().contains(group.getName().toLowerCase())) {
                //assume contacts (WhatsApp) has fuller name
                return group;
            }
        }
        //test out:
        if (name.indexOf("+")==0 && name.indexOf(" ")==2 && name.indexOf("(")==3 && name.indexOf(")")==7) {//could add more tests if necessary
            String tempSMS = "";
            //just get numbers
            for(int i = 0; i<name.length(); i++) {
                String index = Character.toString(name.charAt(i));
                if("0123456789".contains(index)) {
                    tempSMS += index;
                }
            }
            for(GroupInfo group : MainActivity.groupMeChats.values()) {
                if(group.getPhoneNumber().equals(tempSMS)) {
                    return group;
                }
            }
        }
        return null;
    }

    private GroupInfo getSmsInChat(String sms) {
        for(String name : MainActivity.groupMeChats.keySet()) {
            GroupInfo tempGroup = MainActivity.groupMeChats.get(name);
            if(tempGroup.getPhoneNumber().equals(sms)) {
                return tempGroup;
            }
        }
        return null;
    }

    public void sendToWhatsApp() throws JSONException, InterruptedException {
        //add catch for number to existing (and no avail reply) to notify user that msg did not go through
        if(this.groupObject!=null&&this.groupObject.getNotificationReplyObject()!=null) {
            this.wasSent=this.groupObject.reply(this.getText());
        } else if (isPhoneNumber(this.getTo())) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, this.getText());
            sendIntent.putExtra("jid", this.getTo() + "@s.whatsapp.net"); //phone number without "+" prefix
            sendIntent.setPackage("com.whatsapp");
            activity.startActivity(sendIntent);
        }
        if(this.sendConfirmation) {
            Thread.sleep(500);
            sendGroupMeMessage("Message Sent.", this.groupObject);
        }
    }

    public void sendToUser() throws JSONException, InterruptedException {
        GroupInfo groupWith = this.groupObject;
        String message = this.getText();
        if(groupWith==null) {
            System.out.println("no group found");
            groupWith = getGroup(MainActivity.userName);
            message = this.getTitle() + ": " + this.getText();//only if cant create new chat
        }
        if(groupWith!=null) {
            sendGroupMeMessage(message,groupWith);
            //if no group created doesn't add to reply action of default group
            if(message.equals(this.getText())) {
                groupWith.setNotificationReplyObject(this);
            }
        } else {//if no username chat instantiated, use email (if uses this, api key may have changed)
            String fromName = this.title;
            String toEmail = MainActivity.phoneEmail;
            String emailSubject = "";
            String emailBody = this.text;
            new SendMailTask().execute(fromName, toEmail, emailSubject, emailBody, null);
        }
        this.wasSent=true;
        if(this.isPicture) {
            handleAsWhatsAppImage();
        } else if (this.isVoiceNote) {
            handleAsWhatsAppVoiceNote();
        }
    }

    private boolean isPhoneNumber(String potentialPhoneNumber) {
        //take out dashes
        if(potentialPhoneNumber.length()<10||potentialPhoneNumber.length()>11) {
            return false;
        }
        for(int i = 0; i<potentialPhoneNumber.length(); i++) {
            if(!"0123456789".contains(Character.toString(potentialPhoneNumber.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    private String fixString(String string) {
        string=string.replace("‘","'");
        string=string.replace("’","'");
        string=string.replace("“","\"");
        string=string.replace("”","\"");
        return string;
    }

    public ArrayList<File> getAllFiles(File filePath) {
        ArrayList<File> allFiles = new ArrayList<File>();
        if(filePath==null || filePath.isFile()) {
            allFiles.add(filePath);
        } else if(filePath.isDirectory()){
            for (File subFilePath : filePath.listFiles()) {
                allFiles.addAll(getAllFiles(subFilePath));
            }
        }
        return  allFiles;
    }

    private void createNewGroup(String name, String smsNumber) throws JSONException {
        new GroupMeTask().execute("createGroup", name, smsNumber);
    }

    private void sendGroupMeMessage(String text, GroupInfo group) throws JSONException {
        new GroupMeTask().execute("sendMessage", group.getGroupID(), Long.toString(this.getWhen()), text);
        //to allow new source_guid for potential subsequent messages
        this.when+=1;
    }

    // Generic Methods

    public void print() {
        Log.v("title", this.title);
        Log.v("text", this.text);
        Log.v("app", this.app);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationInfo that = (NotificationInfo) o;
        if(this.getApp().equals(that.getApp())&&this.getTitle().equals(that.getTitle())
                &&this.getText().equals(that.getText())&&Math.abs(this.when-that.when) < (long) 10000) {
            return true;
        } else if(this.getWhen()==that.getWhen()) {
            return true;
        } else {
            return false;
        }
    }

    public Notification.Action getReplyAction() {
        return replyAction;
    }

    public void setReplyAction(Notification.Action replyAction) {
        this.replyAction = replyAction;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getWhen() {
        return when;
    }

    public Notification getNotificationObject() {
        return notificationObject;
    }

    public void setNotificationObject(Notification notificationObject) {
        this.notificationObject = notificationObject;
    }
}