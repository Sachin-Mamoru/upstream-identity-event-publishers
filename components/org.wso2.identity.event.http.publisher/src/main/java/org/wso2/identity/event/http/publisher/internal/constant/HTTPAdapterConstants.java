/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.identity.event.http.publisher.internal.constant;

/**
 * Keep constants required by the HTTP Event Adapter.
 */
public class HTTPAdapterConstants {

    public static final String HTTP_ADAPTER_NAME = "httppublisher";

    /**
     * HTTP Adapter related constants.
     */
    public static class Http {

        public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
        public static final String CORRELATION_ID_REQUEST_HEADER = "activityid";
        public static final String X_WSO2_EVENT_SIGNATURE = "x-wso2-event-signature";
        public static final Integer DEFAULT_HTTP_CONNECTION_TIMEOUT = 300;
        public static final Integer DEFAULT_HTTP_READ_TIMEOUT = 300;
        public static final Integer DEFAULT_HTTP_CONNECTION_REQUEST_TIMEOUT = 300;
        public static final Integer DEFAULT_HTTP_MAX_CONNECTIONS = 20;
        public static final Integer DEFAULT_HTTP_MAX_CONNECTIONS_PER_ROUTE = 2;

        private Http() {

        }
    }

    /**
     * Constants related to logging.
     */
    public static class LogConstants {

        private LogConstants() {

        }

        public static final String HTTP_ADAPTER = "http-adapter";

        /**
         * Class related to Action IDs.
         */
        public static class ActionIDs {

            private ActionIDs() {

            }

            public static final String PUBLISH_EVENT = "publish-event";
        }

        /**
         * Class related to Input Keys.
         */
        public static class InputKeys {

            private InputKeys() {

            }

            public static final String ENDPOINT = "endpoint";
            public static final String EVENTS = "events";
            public static final String EVENT_URI = "event uri";
            public static final String EVENT_PROFILE_NAME = "event profile name";
        }
    }

    private HTTPAdapterConstants() {

    }
}
