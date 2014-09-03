package cz.svoboda.telecatch;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
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

public class SendMail {
    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;
    String emailHost;
    String fromUser;
    String fromUserEmailPassword;


    public void setMailServerProperties(String emailPort, String host, String user, String pass) {
        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.starttls.enable", "true");

        emailHost = host;
        fromUser = user;
        fromUserEmailPassword = pass;
    }

    public void createEmailMessage(String fromEmail, String[] toEmails, String emailSubject, String emailBody) throws AddressException, MessagingException {
        mailSession = Session.getDefaultInstance(emailProperties, null);
        emailMessage = new MimeMessage(mailSession);


        for (int i = 0; i < toEmails.length; i++) {
            emailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmails[i]));
        }

        emailMessage.setFrom( new InternetAddress(fromEmail));
        emailMessage.setSubject(emailSubject);
        MimeMultipart multipart = new MimeMultipart();
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(emailBody);
        multipart.addBodyPart(messageBodyPart);
        // Put parts in message
        emailMessage.setContent(multipart);
        //emailMessage.setText("AHOJ");// for a text email

    }
    public void sendEmail() throws AddressException, MessagingException {
        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromUser, fromUserEmailPassword);
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
        System.out.println("Email sent successfully.");
    }
}