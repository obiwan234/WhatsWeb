package com.whatsweb;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GMail {

    final String emailPort = "587";// gmail's smtp port
    final String smtpAuth = "true";
    final String starttls = "true";
    final String emailHost = "smtp.gmail.com";
    final float idealImageSize = 10.0F;

    String fromEmail;
    String fromName;
    String fromPassword;
    String toEmail;
    String emailSubject;
    String emailBody;
    ArrayList<File> attachmentList;
    boolean useOriginalName;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;

    public GMail(String fromEmail, String fromName, String fromPassword, String toEmail, String emailSubject, String emailBody, ArrayList<File> attachmentList, boolean useOriginalName) {
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.fromPassword = fromPassword;
        this.toEmail = toEmail;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.attachmentList = attachmentList;
        this.useOriginalName = useOriginalName;

        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", smtpAuth);
        emailProperties.put("mail.smtp.starttls.enable", starttls);
    }

    public GMail(String fromEmail, String fromName, String fromPassword, String toEmail, String emailSubject, String emailBody, ArrayList<File> attachmentList) {
        new GMail(fromEmail,fromName,fromPassword,toEmail,emailSubject,emailBody,attachmentList,false);
    }

    public MimeMessage createEmailMessage() throws MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);

        emailMessage.setFrom(new InternetAddress(fromEmail,fromName));
        emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

        emailMessage.setSubject(emailSubject);
        if(this.attachmentList==null || this.attachmentList.size()==0) {
            emailMessage.setContent(emailBody, "text/html");// for a html email
        } else {
            Multipart multipart = new MimeMultipart();
            int counter = 1;
            for(File attachment : this.attachmentList) {
//                //compress
//                float originalSize = attachment.length()/1000;
//                float compressQuality = (originalSize + idealImageSize)/2;
//                System.out.println("Converting from " + originalSize + "KB to " + originalSize*compressQuality + "KB.");
                

                //attach
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                DataSource fileSource = new FileDataSource(attachment);
                attachmentBodyPart.setDataHandler(new DataHandler(fileSource));
                if (this.useOriginalName) {
                    attachmentBodyPart.setFileName(attachment.getName());
                    Log.v("File Name", attachment.getName());
                } else {
                    attachmentBodyPart.setFileName("WhatsApp Media " + counter);
                }
                multipart.addBodyPart(attachmentBodyPart);
                counter++;
            }
            emailMessage.setContent(multipart);
        }
        return emailMessage;
    }

    public void sendEmail() throws MessagingException {
        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromEmail, fromPassword);
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
    }

}