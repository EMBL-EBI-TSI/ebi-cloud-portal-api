package uk.ac.ebi.tsc.portal.api.utils;

import java.io.IOException;
import java.util.Collection;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 * @since v0.0.1
 * @author Navis Raj <navis@ebi.ac.uk>
 */
@Component
@PropertySource("classpath:application.properties")
public class SendMail {
	
	private static final Logger logger = LoggerFactory.getLogger(SendMail.class);
	
	private final InternetAddress fromAddress;
	private final JavaMailSender javamailSender;
	
	/**
	 * Initialising constructor.
	 * 
	 * @param javamailSender Mail sender.
	 * @param from Sent email's {@code From} header field. Derived from {@code spring.mail.from} property.
	 */
	public SendMail(JavaMailSender javamailSender, @Value("${spring.mail.from}") String from) {
	    Assert.notNull(from, "spring.mail.from property must be supplied in application.properties");

		this.javamailSender = javamailSender;
		InternetAddress tmpFrom = null;
		try {
		    tmpFrom = new InternetAddress(from);
		} catch (AddressException addressException) {
		    // If this happens all messages sent will not have a From header field!
		    logger.error("Invalid spring.mail.from address of '{}'", from);
		}
		this.fromAddress = tmpFrom;
	}
	
	public void send(Collection<String> toNotify, String subject, String  body) throws IOException {

		try {
			// Create a default MimeMessage object.
			MimeMessage message = javamailSender.createMimeMessage();

			// Set From: header field of the header.
			message.setFrom(fromAddress);

			Address[] addresses = new Address[toNotify.size()];
			int i = 0;
			for (String address : toNotify) {
				Address mailAddress = new InternetAddress(address);
				addresses[i] = mailAddress;
				i++;
			}
			message.addRecipients(Message.RecipientType.TO, addresses);
			message.setSubject(subject);
			String mailBody = "Hi, \n\n" + body + "\n\n" + "Thanks, \n" + "The CloudPortal Team.";
			message.setText(mailBody);
			javamailSender.send(message);
			logger.info("Sent message successfully....");

		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}

}
