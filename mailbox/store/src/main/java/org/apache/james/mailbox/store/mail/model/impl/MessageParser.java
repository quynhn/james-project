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

package org.apache.james.mailbox.store.mail.model.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.Cid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentIdField;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class MessageParser {

    private static final MimeConfig MIME_ENTITY_CONFIG = MimeConfig.custom()
        .setMaxContentLen(-1)
        .setMaxHeaderCount(-1)
        .setMaxHeaderLen(-1)
        .setMaxHeaderCount(-1)
        .setMaxLineLen(-1)
        .build();
    private static final String TEXT_MEDIA_TYPE = "text";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_ID = "Content-ID";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final List<String> ATTACHMENT_CONTENT_DISPOSITIONS = ImmutableList.of(
            ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT.toLowerCase(Locale.US),
            ContentDispositionField.DISPOSITION_TYPE_INLINE.toLowerCase(Locale.US));
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageParser.class);
    private static final Function<String, Optional<Cid>> STRING_TO_CID_FUNCTION = new Function<String, Optional<Cid>>() {
        @Override
        public Optional<Cid> apply(String cid) {
            try {
                return Optional.of(Cid.from(cid));
            } catch (IllegalArgumentException e) {
                return Optional.absent();
            }
        }
    };

    private static final Function<ContentIdField, Optional<Cid>> CONTENT_ID_FIELD_TO_OPTIONAL_CID_FUNCTION = new Function<ContentIdField, Optional<Cid>>() {
        @Override
        public Optional<Cid> apply(ContentIdField field) {
            return Optional.fromNullable(field.getId())
                    .transform(STRING_TO_CID_FUNCTION)
                    .or(Optional.<Cid>absent());
        }
    };

    public List<MessageAttachment> retrieveAttachments(InputStream fullContent) throws MimeException, IOException {
        DefaultMessageBuilder defaultMessageBuilder = new DefaultMessageBuilder();
        defaultMessageBuilder.setMimeEntityConfig(MIME_ENTITY_CONFIG);
        Message message = defaultMessageBuilder.parseMessage(fullContent);
        Body body = message.getBody();
        try {
            Optional<ContentDispositionField> contentDisposition = readHeader(message, CONTENT_DISPOSITION, ContentDispositionField.class);

            if (isMessageWithOnlyOneAttachment(contentDisposition)) {
                return ImmutableList.of(retrieveAttachment(new DefaultMessageWriter(), message));
            }

            if (body instanceof Multipart) {
                Multipart multipartBody = (Multipart)body;
                return listAttachments(multipartBody, Context.fromSubType(multipartBody.getSubType()));
            } else {
                return ImmutableList.of();
            }
        } finally {
            body.dispose();
        }
    }

    private boolean isMessageWithOnlyOneAttachment(Optional<ContentDispositionField> contentDisposition) {
        return contentDisposition.isPresent() && contentDisposition.get().isAttachment();
    }

    private List<MessageAttachment> listAttachments(Multipart multipart, Context context) throws IOException {
        ImmutableList.Builder<MessageAttachment> attachments = ImmutableList.builder();
        MessageWriter messageWriter = new DefaultMessageWriter();
        for (Entity entity : multipart.getBodyParts()) {
            if (isMultipart(entity)) {
                attachments.addAll(listAttachments((Multipart) entity.getBody(), Context.fromEntity(entity)));
            } else {
                if (isAttachment(entity, context)) {
                    try {
                        attachments.add(retrieveAttachment(messageWriter, entity));
                    } catch (IllegalStateException e) {
                        LOGGER.error("The attachment is not well-formed: " + e.getCause());
                    } catch (IOException e) {
                        LOGGER.error("There is error on retrieve attachment: " + e.getCause());
                    }
                }
            }
        }
        return attachments.build();
    }

    private MessageAttachment retrieveAttachment(MessageWriter messageWriter, Entity entity) throws IOException {
        Optional<ContentTypeField> contentTypeField = getContentTypeField(entity);
        Optional<String> contentType = contentType(contentTypeField);
        Optional<String> name = name(contentTypeField);
        Optional<Cid> cid = cid(readHeader(entity, CONTENT_ID, ContentIdField.class));
        boolean isInline = isInline(readHeader(entity, CONTENT_DISPOSITION, ContentDispositionField.class));

        return MessageAttachment.builder()
                .attachment(Attachment.builder()
                    .bytes(getBytes(messageWriter, entity.getBody()))
                    .type(contentType.or(DEFAULT_CONTENT_TYPE))
                    .build())
                .name(name.orNull())
                .cid(cid.orNull())
                .isInline(isInline)
                .build();
    }

    private <T extends ParsedField> Optional<T> readHeader(Entity entity, String headerName, Class<T> clazz) {
        return castField(entity.getHeader().getField(headerName), clazz);
    }

    private Optional<ContentTypeField> getContentTypeField(Entity entity) {
        return castField(entity.getHeader().getField(CONTENT_TYPE), ContentTypeField.class);
    }

    @SuppressWarnings("unchecked")
    private <U extends ParsedField> Optional<U> castField(Field field, Class<U> clazz) {
        if (field == null || !clazz.isInstance(field)) {
            return Optional.absent();
        }
        return Optional.of((U) field);
    }

    private Optional<String> contentType(Optional<ContentTypeField> contentTypeField) {
        return contentTypeField.transform(new Function<ContentTypeField, Optional<String>>() {
            @Override
            public Optional<String> apply(ContentTypeField field) {
                return Optional.fromNullable(field.getMimeType());
            }
        }).or(Optional.<String> absent());
    }

    private Optional<String> name(Optional<ContentTypeField> contentTypeField) {
        return contentTypeField.transform(new Function<ContentTypeField, Optional<String>>() {
            @Override
            public Optional<String> apply(ContentTypeField field) {
                return Optional.fromNullable(field.getParameter("name"))
                  .transform(
                          new Function<String, String>() {
                              public String apply(String input) {
                                  DecodeMonitor monitor = null;
                                  return DecoderUtil.decodeEncodedWords(input, monitor);
                              }
                          });
            }
        }).or(Optional.<String> absent());
    }

    private Optional<Cid> cid(Optional<ContentIdField> contentIdField) {
        return contentIdField.transform(CONTENT_ID_FIELD_TO_OPTIONAL_CID_FUNCTION)
                .or(Optional.<Cid>absent());
    }

    private boolean isMultipart(Entity entity) {
        return entity.isMultipart() && entity.getBody() instanceof Multipart;
    }

    private boolean isInline(Optional<ContentDispositionField> contentDispositionField) {
        return contentDispositionField.transform(new Function<ContentDispositionField, Boolean>() {
            @Override
            public Boolean apply(ContentDispositionField field) {
                return field.isInline();
            }
        }).or(false);
    }

    private boolean isAttachment(Entity part, Context context) {
        if (context == Context.BODY && isTextPart(part)) {
            return false;
        }
        return Optional.fromNullable(part.getDispositionType())
                .transform(new Function<String, Boolean>() {

                    @Override
                    public Boolean apply(String dispositionType) {
                        return ATTACHMENT_CONTENT_DISPOSITIONS.contains(dispositionType.toLowerCase(Locale.US));
                    }
                }).or(false);
    }

    private boolean isTextPart(Entity part) {
        Optional<ContentTypeField> contentTypeField = getContentTypeField(part);
        if (contentTypeField.isPresent()) {
            String mediaType = contentTypeField.get().getMediaType();
            if (mediaType != null && mediaType.equals(TEXT_MEDIA_TYPE)) {
                return true;
            }
        }
        return false;
    }

    private byte[] getBytes(MessageWriter messageWriter, Body body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        messageWriter.writeBody(body, out);
        return out.toByteArray();
    }

    private static enum Context {
        BODY,
        OTHER;

        private static final String ALTERNATIVE_SUB_TYPE = "alternative";
        private static final String MULTIPART_ALTERNATIVE = "multipart/" + ALTERNATIVE_SUB_TYPE;

        public static Context fromEntity(Entity entity) {
            if (isMultipartAlternative(entity)) {
                return BODY;
            }
            return OTHER;
        }

        public static Context fromSubType(String subPart) {
            if (isAlternative(subPart)) {
                return BODY;
            }
            return OTHER;
        }

        private static boolean isMultipartAlternative(Entity entity) {
            return entity.getMimeType().equalsIgnoreCase(MULTIPART_ALTERNATIVE);
        }

        private static boolean isAlternative(String subPart) {
            return subPart.equalsIgnoreCase(ALTERNATIVE_SUB_TYPE);
        }

    }
}
