/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.identity.event.common.publisher.model;

import java.util.Map;

/**
 * Model class for Security Event Token Payload.
 */
public class SecurityEventTokenPayload {

    private final String iss;
    private final String jti;
    private final long iat;
    private final String aud;
    private final String txn;
    private final String rci;
    private final Map<String, EventPayload> event;

    private SecurityEventTokenPayload(Builder builder) {

        this.iss = builder.iss;
        this.jti = builder.jti;
        this.iat = builder.iat;
        this.aud = builder.aud;
        this.txn = builder.txn;
        this.rci = builder.rci;
        this.event = builder.event;
    }

    public String getIss() {

        return iss;
    }

    public String getJti() {

        return jti;
    }

    public long getIat() {

        return iat;
    }

    public String getAud() {

        return aud;
    }

    public String getTxn() {

        return txn;
    }

    public String getRci() {

        return rci;
    }

    public Map<String, EventPayload> getEvent() {

        return event;
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * Builder class for Security Event Token Payload.
     */
    public static class Builder {

        private String iss;
        private String jti;
        private long iat;
        private String aud;
        private String txn;
        private String rci;
        private Map<String, EventPayload> event;

        public Builder iss(String iss) {

            this.iss = iss;
            return this;
        }

        public Builder jti(String jti) {

            this.jti = jti;
            return this;
        }

        public Builder iat(long iat) {

            this.iat = iat;
            return this;
        }

        public Builder aud(String aud) {

            this.aud = aud;
            return this;
        }

        public Builder txn(String txn) {

            this.txn = txn;
            return this;
        }

        public Builder rci(String rci) {

            this.rci = rci;
            return this;
        }

        public Builder event(Map<String, EventPayload> event) {

            this.event = event;
            return this;
        }

        public SecurityEventTokenPayload build() {

            return new SecurityEventTokenPayload(this);
        }
    }
}
