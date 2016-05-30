package org.apache.james.mailbox.model;

import static org.junit.Assert.assertEquals;

import org.apache.james.mailbox.model.MailboxAnnotation.MailboxAnnotationEntryKey;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SimpleMailboxAnnotationEntryKeyTest {

    private static final String ILLEGAL_NAME = "shared";

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testNull() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("Cannot parse null to a");
        new SimpleMailboxAnnotationEntryKey(null);
    }

    @Test
    public void testEmpty() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("Cannot parse an empty string to a");
        new SimpleMailboxAnnotationEntryKey("");
    }

    @Test
    public void testBlank() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("Cannot parse an empty string to a");
        new SimpleMailboxAnnotationEntryKey("   ");
    }

    @Test
    public void testIllegalName_DoesNotStartWithSlash() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation does not start with a slash");
        new SimpleMailboxAnnotationEntryKey(ILLEGAL_NAME);
    }

    @Test
    public void testIllegalName_ContainAsterisk() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation contains at least a '*' character");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .append(MailboxAnnotation.ASTERISK_CHARACTER)
                .append(ILLEGAL_NAME)
                .toString());
    }

    @Test
    public void testIllegalName_ContainPercent() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation contains at least a '%' character");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .append(MailboxAnnotation.PERCENT_CHARACTER)
                .append(ILLEGAL_NAME)
                .toString());
    }

    @Test
    public void testIllegalName_ContentTwoConsecutiveFlash() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation contains 2 consecutive '/' character");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .toString());
    }

    @Test
    public void testIllegalName_EndWithSlash() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation ends with '/' character");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .toString());
    }

    @Test
    public void testIllegalName_NonASCII() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation contains non-ASCII character except some special characters");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append("-contain┬á922")
                .toString());
    }

    @Test
    public void testIllegalName_SpecialCharacter() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The annotation contains non-ASCII character except some special characters");
        new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append("-contain\t$$922")//Contain 'TAB' character as special character
                .toString());
    }

    @Test
    public void testName() {
        MailboxAnnotationEntryKey key = new SimpleMailboxAnnotationEntryKey(new StringBuilder()
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append(ILLEGAL_NAME)
                .append(MailboxAnnotation.SLASH_CHARACTER)
                .append("comment")
                .toString());
        assertEquals("/shared/comment", key.getName());
    }
}
