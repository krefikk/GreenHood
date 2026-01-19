package app;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import database.Variables;

public class EmailSender {
	
	public static void sendNewPassword(String recipientEmail, String newPassword) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Thread.currentThread().setContextClassLoader(EmailSender.class.getClassLoader());
        
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Variables.SENDER_EMAIL, Variables.SENDER_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(Variables.SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(Localization.get("mailpasssubject"));
        
        String htmlContent = "<html><body>"
                + "<p>"  + Localization.get("hello") + ",<br>"
                + Localization.get("passresetrequest") + "<br>"
                + Localization.get("newtemppass", newPassword) + "</p>"
                + "<p>" + Localization.get("changetemppass") + "<br>"
                + "<span style='color: red;'>" + Localization.get("mailpasswarning") + "</span></p>"
                + "</body></html>";
        
        message.setContent(htmlContent, "text/html; charset=utf-8");
        Transport.send(message);
    }
}
