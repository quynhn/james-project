/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.model;

import java.util.Map;

import org.apache.james.mailbox.exception.UnsupportedAnnotationException;
import org.apache.james.mailbox.model.MailboxACL.MailboxACLEntryKey;

/**
 * Stores an Metadata Extensioin applicable to a mailbox. Inspired by
 * RFC5464 The IMAP METADATA Extension.
 * 
 * Implementations must be immutable. Implementations should override
 * {@link #hashCode()} and {@link #equals(Object)}.
 * 
 */
public interface MailboxAnnotation {
    /**
     * The key used in {@link MailboxAnnotation#getEntries()}. Implementations should
     * override {@link #hashCode()} and {@link #equals(Object)} in such a way
     * that all of {@link #getName()}, {@link #getNameType()} and
     * {@link #isNegative()} are significant.
     * 
     */
    interface MailboxAnnotationEntryKey {
        /**
         * Returns the name of annotation which can have many component. Each component is separared by "/" this
         * {@link MailboxAnnotationEntryKey} applies.
         * 
         * @return annotation name.
         */
        String getName();
    }

    /**
     * Single right applicable to a mailbox.
     */
    interface MailboxAnnotationEntryValue {
        /**
         * Returns the object representation of the entry key.
         * 
         * @return <code>Object</code> representation of this key
         */
        Object getValue();
        
        /**
         * Returns the value type of the value
         * 
         * @return <code>AnnotationValue</code> representation of the type of the value
         */
        AnnotationValue getValueType();
    }

    interface MailboxAnnotationCommand {
        MailboxAnnotationEntryKey getEntryKey();

        MailboxAnnotationEntryValue getEntryValue();
    }

    /**
     * Create the given Annotation and return the created as a new Annotation.
     *
     * @param annoCreate Create to perform
     * @return current Annotation created
     * @throws UnsupportedAnnotationException
     */
    MailboxAnnotation create(MailboxAnnotationCommand annoCreate) throws UnsupportedAnnotationException;

    /**
     * Update the existed Annotation and return the updated Annotation.
     *
     * @param annoUpdate update to perform
     * @return current Annotation updated
     * @throws UnsupportedAnnotationException
     */
    MailboxAnnotation update(MailboxAnnotationCommand annoUpdate) throws UnsupportedAnnotationException;

    /**
     * Delete the given Annotation and return the deleted Annotation.
     *
     * @param annoRemove Delete to perform
     * @return current Annotation deleted
     * @throws UnsupportedAnnotationException
     */
    MailboxAnnotation remove(MailboxAnnotationCommand annoRemove) throws UnsupportedAnnotationException;

    /**
     * {@link Map} of entries.
     * 
     * @return the entries.
     */
    Map<MailboxAnnotationEntryKey, MailboxAnnotationEntryValue> getEntries();

    /**
     * Special name literals.
     */
    enum AnnotationValue {
        nil, string, binary, octets
    }

    /**
     * Separation to (de)serializing the component of name {@link MailboxACLEntryKey}s.
     * 
     */
    char SLASH_CHARACTER = '/';

    char ASTERISK_CHARACTER = '*';

    char PERCENT_CHARACTER = '%';
}
