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

package org.wso2.identity.event.websubhub.publisher.constant;

/**
 * SQL Constants for WebSubHub Topic Management.
 * This class is used to store SQL queries and column names.
 */
public final class TopicSQLConstants {

    private TopicSQLConstants() {

    }

    /**
     * This class is used to store column names.
     */
    public static final class Column {

        public static final String ID = "ID";
        public static final String TOPIC_URI = "TOPIC_URI";
        public static final String CHANNEL_URI = "CHANNEL_URI";
        public static final String VERSION = "VERSION";
        public static final String TENANT_DOMAIN = "TENANT_DOMAIN";

        private Column() {

        }
    }

    /**
     * This class is used to store SQL queries.
     */
    public static final class Query {

        public static final String INSERT_TOPIC =
                "INSERT INTO IDN_WEBHOOK_TOPICS (ID, TOPIC_URI, CHANNEL_URI, VERSION, TENANT_DOMAIN) " +
                        "VALUES (:ID;, :TOPIC_URI;, :CHANNEL_URI;, :VERSION;, :TENANT_DOMAIN;)";

        public static final String GET_TOPIC =
                "SELECT * FROM IDN_WEBHOOK_TOPICS WHERE TOPIC_URI = :TOPIC_URI; AND TENANT_DOMAIN = :TENANT_DOMAIN;";

        public static final String DELETE_TOPIC =
                "DELETE FROM IDN_WEBHOOK_TOPICS WHERE TOPIC_URI = :TOPIC_URI; AND TENANT_DOMAIN = :TENANT_DOMAIN;";

        public static final String GET_ALL_TOPICS =
                "SELECT * FROM IDN_WEBHOOK_TOPICS WHERE TENANT_DOMAIN = :TENANT_DOMAIN;";

        private Query() {

        }
    }
}
