package com.whatsweb;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Random;

public class SendCaseTask extends AsyncTask{

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected Object doInBackground(Object... args) {
        try {
            ArrayList<Integer> caseIndexes = new ArrayList<Integer>();
            String query = ((String) args[0]).toLowerCase();
            //Log.v("HERE","\""+query+"\"");
            boolean isYear = true;
            for(int i = 0; i<query.length(); i++) {
                if(!"0123456789 ".contains(Character.toString(query.charAt(i)))) {
                    isYear = false;
                }
            }
            ArrayList<Integer> yearIndexes = new ArrayList<Integer>();

            InputStream jsonInputStream = NotificationInfo.activity.getAssets().open("cases.json");
            int size = jsonInputStream.available();
            byte[] buffer = new byte[size];
            jsonInputStream.read(buffer);
            jsonInputStream.close();
            String jsonString = new String(buffer, "UTF-8");

            JSONObject casesObject = new JSONObject(jsonString);
            JSONArray caseList = casesObject.getJSONArray("cases");
            ArrayList<String[]> parties = new ArrayList<String[]>();
            for(int i = 0; i < caseList.length(); i++) {
                JSONObject currCase = caseList.getJSONObject(i);
                if(isYear) {
                    String caseDate = currCase.getString("date");
                    if(query.contains(caseDate.substring(caseDate.indexOf(",")+2))) {
                        yearIndexes.add(i);
                    }
                }
                String partyString = currCase.getString("name");
                String[] partyArray;
                if(partyString.contains(" v. ")) {
                    String firstParty = partyString.substring(0, partyString.indexOf(" v. "));
                    if(firstParty.substring(firstParty.length()-1).equals(",")) {
                        firstParty = firstParty.substring(0,firstParty.length()-1);
                    }
                    partyArray = new String[]{firstParty, partyString.substring(partyString.indexOf(" v. ") + 4)};
                } else {
                    partyArray = new String[]{partyString, partyString};
                }

                //System.out.println(partyArray[0]+", "+partyArray[1]);
                parties.add(partyArray);
            }

            if(query.contains(" v. ") || query.contains(" v ")) {
                String queryOne = query.substring(0, Math.max(query.indexOf(" v. "),query.indexOf(" v "))).toLowerCase();
                String queryTwo = query.substring(Math.max(query.indexOf(" v. ")+4,query.indexOf(" v ")+3)).toLowerCase();
                for(int i = 0; i<parties.size(); i++) {
                    String[] litigants = parties.get(i);
                    if((litigants[0].toLowerCase().contains(queryOne) && litigants[1].toLowerCase().contains(queryTwo)) ||
                        litigants[0].toLowerCase().contains(queryTwo) && litigants[1].toLowerCase().contains(queryOne)) {
                        caseIndexes.add(i);
                        System.out.println("case found");
                    }
                }
            }
            if(caseIndexes.size()==0) {
                if(isYear) {
                    for(int i = 0; i<3; i++) {
                        caseIndexes.add(yearIndexes.get(new Random().nextInt(yearIndexes.size())));
                    }
                } else {
                    for (int i = 0; i < 3; i++) {
                        caseIndexes.add(new Random().nextInt(parties.size()));
                    }
                }
            }

            int numFilesSent = 0;
            for(int caseIndex : caseIndexes) {
                if(numFilesSent>=10) {
                    break;
                }
                try {
                    JSONObject caseToSend = caseList.getJSONObject(caseIndex);
                    String caseLink = caseToSend.getString("link");
                    URL caseLinkURL = new URL(caseLink);
                    BufferedInputStream caseInputStream = new BufferedInputStream(caseLinkURL.openStream());
                    Log.v("HERE", "STREAM OPEN");
                    Log.v("HERE", String.valueOf(NotificationInfo.activity.getFilesDir()) + "/Oral_Argument_" + caseIndex + ".mp3");
                    File argFile = new File(String.valueOf(NotificationInfo.activity.getFilesDir()) + "/Oral_Argument_" + caseIndex + ".mp3");
                    Log.v("HERE","CREATED FILE");
                    //Files.copy(caseInputStream, argFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    FileOutputStream fileOutputStream = new FileOutputStream(argFile);
                    Log.v("HERE","CREATED OUTPUT STREAM");
                    byte dataBuffer[] = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = caseInputStream.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }

                    Log.v("HERE", "FILE COPIED");
                    ArrayList<File> arguments = new ArrayList<File>();
                    arguments.add(argFile);

                    GMail androidEmail = new GMail("whatswebtext@gmail.com", "Supreme Court",
                            "ChoneinHadaas425", "nomsg419@gmail.com",
                            "(" + caseToSend.getString("date") + ") " + caseToSend.getString("name"),
                            "This is the case.", arguments, true);
                    androidEmail.createEmailMessage();
                    androidEmail.sendEmail();
                    Log.v("HERE", "FILE SENT");
                    argFile.delete();
                    numFilesSent++;
                }
                catch (Exception e) {
                    Log.e("SendCaseTask", e.getMessage(), e);
                }
            }
            Log.v("HERE", "DONE! SENT " + numFilesSent + " FILES");
        }
        catch (Exception e) {
                Log.e("SendCaseTask", e.getMessage(), e);
        }
        return null;
    }
}

