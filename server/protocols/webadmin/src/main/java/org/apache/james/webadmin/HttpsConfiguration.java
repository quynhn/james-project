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

package org.apache.james.webadmin;

import java.util.Objects;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

public class HttpsConfiguration {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Optional<Boolean> enabled = Optional.empty();
        private String keystoreFilePath;
        private String keystorePassword;
        private String truststoreFilePath;
        private String truststorePassword;

        public Builder enable(boolean isEnabled) {
            this.enabled = Optional.of(isEnabled);
            return this;
        }

        public Builder enabled() {
            return enable(true);
        }

        public Builder disabled() {
            return enable(false);
        }

        public Builder raw(String keystoreFilePath,
                           String keystorePassword,
                           String truststoreFilePath,
                           String truststorePassword){
            Preconditions.checkNotNull(keystoreFilePath);
            Preconditions.checkNotNull(keystorePassword);

            this.keystoreFilePath = keystoreFilePath;
            this.keystorePassword = keystorePassword;
            this.truststoreFilePath = truststoreFilePath;
            this.truststorePassword = truststorePassword;
            return this;
        }

        public Builder selfSigned(String keystoreFilePath, String keystorePassword){
            Preconditions.checkNotNull(keystoreFilePath);
            Preconditions.checkNotNull(keystorePassword);

            this.enabled = Optional.of(true);
            this.keystoreFilePath = keystoreFilePath;
            this.keystorePassword = keystorePassword;
            return this;
        }

        public HttpsConfiguration build() {
            Preconditions.checkState(enabled.isPresent(), "You need to specify if https is enabled");
            Preconditions.checkState(!enabled.get() || hasKeystoreInformation(), "If enabled, you need to provide keystore information");
            Preconditions.checkState(optionalHasTrustStoreInformation(), "You need to provide both information about trustStore");
            return new HttpsConfiguration(enabled.get(), keystoreFilePath, keystorePassword, truststoreFilePath, truststorePassword);
        }

        private boolean optionalHasTrustStoreInformation() {
            return (truststoreFilePath == null) == (truststorePassword == null);
        }

        private boolean hasKeystoreInformation() {
            return keystorePassword != null && keystoreFilePath != null;
        }

    }

    private final boolean enabled;
    private final String keystoreFilePath;
    private final String keystorePassword;
    private final String truststoreFilePath;
    private final String truststorePassword;

    @VisibleForTesting
    HttpsConfiguration(boolean enabled, String keystoreFilePath, String keystorePassword, String truststoreFilePath, String truststorePassword) {
        this.enabled = enabled;
        this.keystoreFilePath = keystoreFilePath;
        this.keystorePassword = keystorePassword;
        this.truststoreFilePath = truststoreFilePath;
        this.truststorePassword = truststorePassword;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getKeystoreFilePath() {
        return keystoreFilePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getTruststoreFilePath() {
        return truststoreFilePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    @Override
    public final boolean equals(Object o) {
       if (o instanceof HttpsConfiguration) {
           HttpsConfiguration that = (HttpsConfiguration) o;

           return Objects.equals(this.enabled, that.enabled)
               && Objects.equals(this.keystoreFilePath, that.keystoreFilePath)
               && Objects.equals(this.keystorePassword, that.keystorePassword)
               && Objects.equals(this.truststoreFilePath, that.truststoreFilePath)
               && Objects.equals(this.truststorePassword, that.truststorePassword);
       }
       return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(enabled, keystoreFilePath, keystorePassword, truststoreFilePath, truststorePassword);
    }
}
