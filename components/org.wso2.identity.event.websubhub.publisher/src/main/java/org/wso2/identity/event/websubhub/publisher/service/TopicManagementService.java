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

package org.wso2.identity.event.websubhub.publisher.service;

import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.model.Topic;

import java.util.List;

/**
 * Service interface for managing WebSubHub topics.
 */
public interface TopicManagementService {

    /**
     * Register a list of topics with WebSubHub and persist them.
     *
     * @param channelUris    List of topic URIs to register
     * @param tenantDomain The tenant domain
     * @throws WebSubAdapterException If an error occurs during topic registration
     */
    void registerTopics(List<String> channelUris, String tenantDomain) throws WebSubAdapterException;

    /**
     * Deregister a list of topics from WebSubHub.
     *
     * @param channelUris    List of topic URIs to deregister
     * @param tenantDomain The tenant domain
     * @throws WebSubAdapterException If an error occurs during topic deregistration
     */
    void deregisterTopics(List<String> channelUris, String tenantDomain) throws WebSubAdapterException;

    /**
     * Get all registered topics for a tenant.
     *
     * @param tenantDomain The tenant domain
     * @return List of registered topics
     * @throws WebSubAdapterException If an error occurs while getting the topics
     */
    List<Topic> getAllTopics(String tenantDomain) throws WebSubAdapterException;

    /**
     * Check if a topic is registered.
     *
     * @param channelUri     The URI of the topic
     * @param tenantDomain The tenant domain
     * @return True if the topic is registered, false otherwise
     * @throws WebSubAdapterException If an error occurs while checking the topic
     */
    boolean isTopicRegistered(String channelUri, String tenantDomain) throws WebSubAdapterException;
}
