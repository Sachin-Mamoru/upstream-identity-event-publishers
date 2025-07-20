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
 * Error messages for webhook management.
 */
public enum ErrorMessage {

    // client errors

    // server errors
    ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD("HTTPADAPTER-65001", "Invalid payload provided.",
            "Event payload cannot be processed."),
    ERROR_GETTING_ASYNC_CLIENT("HTTPADAPTER-65002", "Error getting the async client to publish events.",
            "Error preparing async client to publish events, tenant: %s."),
    ERROR_CREATING_SSL_CONTEXT("HTTPADAPTER-65003", "Error while preparing SSL context for HTTP http client.",
            "Server error encountered while preparing SSL context for HTTP http client."),
    ERROR_CREATING_HMAC_SIGNATURE("HTTPADAPTER-65004", "Failed to generate HMAC signature.",
            "Failed to generate HMAC signature for the HTTP request. " +
                    "Ensure that the secret key is configured correctly."),
    ERROR_PUBLISHING_EVENT("HTTPADAPTER-65005", "Error while publishing event.",
            "Error while publishing event to the HTTP endpoint using the HTTP adapter."),
    ERROR_ACTIVE_WEBHOOKS_RETRIEVAL("HTTPADAPTER-65006", "Error while retrieving active webhooks.",
            "Error while retrieving active webhooks.");

    private final String code;
    private final String message;
    private final String description;

    ErrorMessage(String code, String message, String description) {

        this.code = code;
        this.message = message;
        this.description = description;
    }

    public String getCode() {

        return code;
    }

    public String getMessage() {

        return message;
    }

    public String getDescription() {

        return description;
    }

    @Override
    public String toString() {

        return code + " : " + message;
    }
}
