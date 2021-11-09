package uk.ac.ebi.tsc.portal.api.utils;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author geoff
 */
public class SendMailTest {

    @Test
    public void testConstructor() {
        try {
           new SendMail(null, null);
           fail("Should not accept a null value for the spring.mail.from property");
        } catch (IllegalArgumentException iae) {}

        // This shouldn't fail, but should write an error to the logs
        new SendMail(null, "null@null@null");

        // This shouldn't fail either, but shouldn't write an error to the logs
        new SendMail(null, "fish@chips");
    }
}