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


package org.apache.james.mailets.configuration;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public class ProcessorConfiguration implements SerializableAsXml {

    public static Builder builder() {
        return new Builder();
    }

    public static Builder transport() {
        return builder().state("transport");
    }

    public static class Builder {

        private String state;
        private boolean enableJmx;
        private ImmutableList.Builder<MailetConfiguration> mailets;

        private Builder() {
            mailets = ImmutableList.builder();
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder enableJmx(boolean enableJmx) {
            this.enableJmx = enableJmx;
            return this;
        }

        public Builder addMailet(MailetConfiguration mailetConfiguration) {
            this.mailets.add(mailetConfiguration);
            return this;
        }

        public ProcessorConfiguration build() {
            Preconditions.checkState(!Strings.isNullOrEmpty(state), "'state' is mandatory");
            return new ProcessorConfiguration(state, enableJmx, mailets.build());
        }
    }

    private final String state;
    private final boolean enableJmx;
    private final ImmutableList<MailetConfiguration> mailets;

    private ProcessorConfiguration(String state, boolean enableJmx, ImmutableList<MailetConfiguration> mailets) {
        this.state = state;
        this.enableJmx = enableJmx;
        this.mailets = mailets;
    }

    public String getState() {
        return state;
    }

    public boolean isEnableJmx() {
        return enableJmx;
    }

    public ImmutableList<MailetConfiguration> getMailets() {
        return mailets;
    }

    @Override
    public String serializeAsXml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<processor state=\"").append(getState()).append("\" enableJmx=\"").append(isEnableJmx() ? "true" : "false").append("\">\n");
        for (MailetConfiguration mailet : getMailets()) {
            builder.append(mailet.serializeAsXml());
        }
        builder.append("</processor>\n");
        return builder.toString();
    }
}
