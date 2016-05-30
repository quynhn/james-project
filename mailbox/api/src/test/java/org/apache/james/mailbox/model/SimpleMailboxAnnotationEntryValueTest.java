package org.apache.james.mailbox.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyLong;

import org.apache.james.mailbox.model.MailboxAnnotation.AnnotationValue;
import org.apache.james.mailbox.model.SimpleMailboxAnnotation.SimpleMailboxAnnotationEntryValue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SimpleMailboxAnnotationEntryValueTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testNullValueType() {
        expected.expect(IllegalArgumentException.class);
        new SimpleMailboxAnnotationEntryValue(anyString(), null);
    }

    @Test
    public void testNullValue() {
        expected.expect(IllegalArgumentException.class);
        new SimpleMailboxAnnotationEntryValue(null, AnnotationValue.binary);
    }

    @Test
    public void testNilEntry() {
        expected.expect(IllegalArgumentException.class);
        new SimpleMailboxAnnotationEntryValue(anyString(), AnnotationValue.nil);
    }

    @Test
    public void testBinaryValueType() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The value is not binary data");
        new SimpleMailboxAnnotationEntryValue(anyString(), AnnotationValue.binary);
    }

    @Test
    public void testStringValueType() {
        expected.expect(IllegalStateException.class);
        expected.expectMessage("The value is not string data");
        new SimpleMailboxAnnotationEntryValue(anyLong(), AnnotationValue.string);
    }
}
