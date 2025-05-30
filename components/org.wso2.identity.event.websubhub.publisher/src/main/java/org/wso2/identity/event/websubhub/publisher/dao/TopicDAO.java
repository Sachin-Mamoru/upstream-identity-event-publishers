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

package org.wso2.identity.event.websubhub.publisher.dao;

import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.model.Topic;

import java.util.List;

/**
 * Data Access Object interface for WebSubHub topic management.
 */
public interface TopicDAO {

    /**
     * Add a topic to the database.
     *
     * @param topic The topic to add
     * @return The ID of the added topic
     * @throws WebSubAdapterException If an error occurs while adding the topic
     */
    public String addTopic(Topic topic) throws WebSubAdapterException;

    /**
     * Get a topic by its URI and tenant domain.
     *
     * @param topicUri     The URI of the topic
     * @param tenantDomain The tenant domain
     * @return The topic, or null if not found
     * @throws WebSubAdapterException If an error occurs while getting the topic
     */
    public Topic getTopic(String topicUri, String tenantDomain) throws WebSubAdapterException;

    /**
     * Check if a topic exists.
     *
     * @param topicUri     The URI of the topic
     * @param tenantDomain The tenant domain
     * @return True if the topic exists, false otherwise
     * @throws WebSubAdapterException If an error occurs while checking if the topic exists
     */
    public boolean isTopicExists(String topicUri, String tenantDomain) throws WebSubAdapterException;

    /**
     * Delete a topic by its URI and tenant domain.
     *
     * @param topicUri     The URI of the topic
     * @param tenantDomain The tenant domain
     * @throws WebSubAdapterException If an error occurs while deleting the topic
     */
    public void deleteTopic(String topicUri, String tenantDomain) throws WebSubAdapterException;

    /**
     * Get all topics for a tenant.
     *
     * @param tenantDomain The tenant domain
     * @return List of topics for the tenant
     * @throws WebSubAdapterException If an error occurs while getting the topics
     */
    public List<Topic> getAllTopics(String tenantDomain) throws WebSubAdapterException;
}
