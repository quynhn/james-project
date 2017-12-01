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

package org.apache.james.webadmin.utils;

import static spark.Spark.halt;
import com.fasterxml.jackson.core.JsonProcessingException;

import spark.HaltException;

public class ErrorResponder {
    public enum ErrorType {
        INVALID_ARGUMENT("InvalidArgument"),
        WRONG_STATE("WrongState"),
        SERVER_ERROR("ServerError");

        private final String type;

        ErrorType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private int statusCode;
    private ErrorType type;
    private String message;
    private String cause;

    public static ErrorResponder builder() {
        return new ErrorResponder();
    }

    public ErrorResponder statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ErrorResponder type(ErrorType type) {
        this.type = type;
        return this;
    }

    public ErrorResponder message(String message) {
        this.message = message;
        return this;
    }

    public ErrorResponder cause(String cause) {
        this.cause = cause;
        return this;
    }

    public HaltException haltError() {
        try {
            return halt(statusCode, new JsonTransformer().render(new ErrorDetail(statusCode, type.getType(), message, cause)));
        } catch (JsonProcessingException e) {
            return halt(statusCode);
        }
    }

    private static class ErrorDetail {
        private final int statusCode;
        private final String type;
        private final String message;
        private final String cause;

        public ErrorDetail(int statusCode, String type, String message, String cause) {
            this.statusCode = statusCode;
            this.type = type;
            this.message = message;
            this.cause = cause;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getCause() {
            return cause;
        }
    }
}
