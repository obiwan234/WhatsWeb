package com.whatsweb;

import android.os.Environment;

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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class GMail {

    final String emailPort = "587";// gmail's smtp port
    final String smtpAuth = "true";
    final String starttls = "true";
    final String emailHost = "smtp.gmail.com";

    String fromEmail;
    String fromName;
    String fromPassword;
    String toEmail;
    String emailSubject;
    String emailBody;
    File attachment;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;

    public GMail(String fromEmail, String fromName, String fromPassword, String toEmail, String emailSubject, String emailBody, File attachment) {
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.fromPassword = fromPassword;
        this.toEmail = toEmail;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.attachment = attachment;

        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", smtpAuth);
        emailProperties.put("mail.smtp.starttls.enable", starttls);
    }

    public MimeMessage createEmailMessage() throws MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);

        emailMessage.setFrom(new InternetAddress(fromEmail,fromName));
        emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));

        emailMessage.setSubject(emailSubject);
        if(this.attachment==null) {
            emailMessage.setContent(emailBody, "text/html");// for a html email
        } else {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource fileSource = new FileDataSource(this.attachment);
            attachmentBodyPart.setDataHandler(new DataHandler(fileSource));
            attachmentBodyPart.setFileName("WhatsApp Image");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(attachmentBodyPart);
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