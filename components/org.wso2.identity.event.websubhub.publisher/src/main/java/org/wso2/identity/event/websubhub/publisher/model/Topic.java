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

package org.wso2.identity.event.websubhub.publisher.model;

/**
 * Model class representing a WebSubHub Topic.
 */
public class Topic {

    private String id;
    private String topicUri;
    private String version;
    private String tenantDomain;

    /**
     * Default constructor.
     */
    public Topic() {

    }

    /**
     * Constructor with required fields.
     *
     * @param topicUri     The URI of the topic
     * @param tenantDomain The tenant domain
     */
    public Topic(String topicUri, String tenantDomain) {

        this.topicUri = topicUri;
        this.tenantDomain = tenantDomain;
    }

    /**
     * Get the ID of the topic.
     *
     * @return The ID
     */
    public String getId() {

        return id;
    }

    /**
     * Set the ID of the topic.
     *
     * @param id The ID to set
     */
    public void setId(String id) {

        this.id = id;
    }

    /**
     * Get the URI of the topic.
     *
     * @return The topic URI
     */
    public String getTopicUri() {

        return topicUri;
    }

    /**
     * Set the URI of the topic.
     *
     * @param topicUri The topic URI to set
     */
    public void setTopicUri(String topicUri) {

        this.topicUri = topicUri;
    }

    /**
     * Get the version of the topic.
     *
     * @return The version
     */
    public String getVersion() {

        return version;
    }

    /**
     * Set the version of the topic.
     *
     * @param version The version to set
     */
    public void setVersion(String version) {

        this.version = version;
    }

    /**
     * Get the tenant domain of the topic.
     *
     * @return The tenant domain
     */
    public String getTenantDomain() {

        return tenantDomain;
    }

    /**
     * Set the tenant domain of the topic.
     *
     * @param tenantDomain The tenant domain to set
     */
    public void setTenantDomain(String tenantDomain) {

        this.tenantDomain = tenantDomain;
    }
}
