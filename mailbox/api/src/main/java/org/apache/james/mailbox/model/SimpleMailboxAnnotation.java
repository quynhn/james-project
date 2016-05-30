package org.apache.james.mailbox.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.james.mailbox.exception.UnsupportedAnnotationException;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey;

import com.google.common.base.Preconditions;

public class SimpleMailboxAnnotation implements MailboxAnnotation {

    private static final Pattern NAME_ANNOTATION_PATTERN = Pattern.compile("[^\\x1a-\\x7f]");

    public static final MailboxAnnotation EMPTY;

    private final Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> entries;

    static {
        EMPTY = new SimpleMailboxAnnotation();
    }

    /**
     * Creates a new instance of SimpleMailboxAnnotation containing no entries.
     * 
     */
    public SimpleMailboxAnnotation() {
        this.entries = Collections.emptyMap();
    }

    /**
     * Creates a new instance of SimpleMailboxAnnotation from the given array of
     * entries.
     * 
     * @param entries
     */
    public SimpleMailboxAnnotation(Map.Entry<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>[] entries) {
        if (entries != null) {
            Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> m = 
                    new HashMap<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>(entries.length + entries.length / 2 + 1);
            for (Entry<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> en : entries) {
                m.put(en.getKey(), en.getValue());
            }
            this.entries = Collections.unmodifiableMap(m);
        } else {
            this.entries = Collections.emptyMap();
        }
    }

    /**
     * Creates a new instance of SimpleMailboxAnnotation from the given {@link Map} of
     * entries.
     * 
     * @param entries
     */
    public SimpleMailboxAnnotation(Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> entries) {
        if (entries != null && entries.size() > 0) {
            Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> m = 
                    new HashMap<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>(entries.size() + entries.size() / 2 + 1);
            for (Entry<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> en : entries.entrySet()) {
                m.put(en.getKey(), en.getValue());
            }
            this.entries = Collections.unmodifiableMap(m);
        } else {
            this.entries = Collections.emptyMap();
        }
    }

    /**
     * Default implementation of {@link MailboxACLEntryKey}.
     */
    public static class SimpleMailboxAnnotationEntryKey implements MailboxAnnotationEntryKey {
        private final String name;

        /**
         * Creates a new instance of SimpleMailboxAnnotationEntryKey from the given
         * serialized {@link String}.
         * 
         * @param serialized
         */
        public SimpleMailboxAnnotationEntryKey(String serialized) {

            if (serialized == null) {
                throw new IllegalStateException("Cannot parse null to a " + getClass().getName());
            }
            serialized = serialized.trim();
            if (serialized.length() == 0) {
                throw new IllegalStateException("Cannot parse an empty string to a " + getClass().getName());
            }
            if (serialized.charAt(0) != SLASH_CHARACTER) {
                throw new IllegalStateException("The annotation does not start with a slash " + getClass().getName());
            }
            if (serialized.contains("" + ASTERISK_CHARACTER)) {
                throw new IllegalStateException("The annotation contains at least a '*' character " + getClass().getName());
            }
            if (serialized.contains("" + PERCENT_CHARACTER)) {
                throw new IllegalStateException("The annotation contains at least a '%' character " + getClass().getName());
            }
            if (serialized.contains("" + SLASH_CHARACTER + SLASH_CHARACTER)) {
                throw new IllegalStateException("The annotation contains 2 consecutive '/' character " + getClass().getName());
            }
            if (serialized.endsWith("" + SLASH_CHARACTER)) {
                throw new IllegalStateException("The annotation ends with '/' character " + getClass().getName());
            }
            if (NAME_ANNOTATION_PATTERN.matcher(serialized).find()) {
                throw new IllegalStateException("The annotation contains non-ASCII character except some special characters " + getClass().getName());
            }
            this.name = serialized;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MailboxAnnotationEntryKey) {
                MailboxAnnotationEntryKey other = (MailboxAnnotationEntryKey) o;
                return this.name.equals(other.getName());
            } else {
                return false;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.james.mailbox.MailboxACL.MailboxACLEntryKey#getName()
         */
        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return new StringBuilder(name.length()).append(name).toString();
        }

    }

    public static class SimpleMailboxAnnotationEntryValue implements MailboxAnnotationEntryValue {
        private final int hash;
        private final Object value;
        private final AnnotationValue valueType;

        public static final MailboxAnnotationEntryValue NIL;

        static {
            NIL = new SimpleMailboxAnnotationEntryValue();
        }

        public SimpleMailboxAnnotationEntryValue(Object value, AnnotationValue valueType) {
            super();
            Preconditions.checkArgument(value != null);
            Preconditions.checkArgument(valueType != null && valueType != AnnotationValue.nil);
            switch (valueType) {
            case binary:
                if (!(value instanceof byte[])) {
                    throw new IllegalStateException("The value is not binary data");
                }
                break;
            case string:
                if (!(value instanceof String)) {
                    throw new IllegalStateException("The value is not string data");
                }
                break;

            default:
                break;
            }
            this.value = value;
            this.valueType = valueType;
            this.hash = hash();
        }

        private SimpleMailboxAnnotationEntryValue() {
            this.valueType = AnnotationValue.nil;
            this.value = null;
            this.hash = hash();
        }

        public Object getValue() {
            return value;
        }

        public AnnotationValue getValueType() {
            return valueType;
        }

        private int hash() {
            final int PRIME = 31;
            int hash = null!= valueType ? valueType.hashCode() : PRIME;
            hash = PRIME * hash + (null != value ? value.hashCode() : PRIME);
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MailboxAnnotationEntryValue) {
                MailboxAnnotationEntryValue other = (MailboxAnnotationEntryValue) o;
                
                if (valueType != null ? !valueType.equals(other.getValueType()) : other.getValueType() != null) return false;
                return !(value != null ? !value.equals(other.getValue()) : other.getValue() != null);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return new StringBuilder().append(valueType).append(value).toString();
        }
    }

    public static class SimpleMailboxAnnotationCommand implements MailboxAnnotationCommand {
        private final MailboxAnnotationEntryKey key;
        private final MailboxAnnotationEntryValue value;

        public SimpleMailboxAnnotationCommand(MailboxAnnotationEntryKey key, MailboxAnnotationEntryValue value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SimpleMailboxAnnotationCommand)) return false;

            SimpleMailboxAnnotationCommand that = (SimpleMailboxAnnotationCommand) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            return !(value != null ? !value.equals(that.value) : that.value != null);

        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public MailboxAnnotationEntryKey getEntryKey() {
            return key;
        }

        @Override
        public MailboxAnnotationEntryValue getEntryValue() {
            return value;
        }
    }

    @Override
    public MailboxAnnotation create(MailboxAnnotationCommand annoCreate) throws UnsupportedAnnotationException {
        return update(annoCreate);
    }

    @Override
    public MailboxAnnotation update(MailboxAnnotationCommand annoUpdate) throws UnsupportedAnnotationException {
        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = new HashMap<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>(this.entries);
        resultEntries.put(annoUpdate.getEntryKey(), annoUpdate.getEntryValue());
        return new SimpleMailboxAnnotation(Collections.unmodifiableMap(resultEntries));
    }

    @Override
    public MailboxAnnotation remove(MailboxAnnotationCommand annoRemove) throws UnsupportedAnnotationException {
        Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> resultEntries = new HashMap<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue>(this.entries);
        if (resultEntries.containsKey(annoRemove.getEntryKey())) {
            resultEntries.remove(annoRemove.getEntryKey());
        }
        return new SimpleMailboxAnnotation(Collections.unmodifiableMap(resultEntries));
    }

    @Override
    public Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> getEntries() {
        // TODO Auto-generated method stub
        return entries;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof MailboxAnnotation) {
            MailboxAnnotation anno = (MailboxAnnotation) o;
            Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> ens = anno.getEntries();
            if (this.entries.size() != ens.size())
                return false;

            try {
                Iterator<Entry<MailboxAnnotationEntryKey,MailboxAnnotationEntryValue>> i = this.entries.entrySet().iterator();
                while (i.hasNext()) {
                    Entry<MailboxAnnotationEntryKey,MailboxAnnotationEntryValue> e = i.next();
                    MailboxAnnotationEntryKey key = e.getKey();
                    MailboxAnnotationEntryValue value = e.getValue();
                    if (value == null) {
                        if (!(ens.get(key)==null && ens.containsKey(key)))
                            return false;
                    } else {
                        if (!value.equals(ens.get(key)))
                            return false;
                    }
                }
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }

            return true;        }
        return false;
    }
    
}
