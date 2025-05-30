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

package org.wso2.identity.event.websubhub.publisher.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.dao.TopicDAO;
import org.wso2.identity.event.websubhub.publisher.dao.impl.TopicDAOImpl;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.model.Topic;
import org.wso2.identity.event.websubhub.publisher.service.TopicManagementService;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubCorrelationLogUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_TENANT_DOMAIN;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_TOPIC_URIS;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_REGISTERING_HUB_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_TOPIC_PERSISTENCE;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.DEREGISTER;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_ACTIVE_SUBS;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_REASON;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.REGISTER;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.buildURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.constructHubTopic;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.getWebSubBaseURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleClientException;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleErrorResponse;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleFailedOperation;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleSuccessfulOperation;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.parseEventHubResponse;

/**
 * Implementation of the TopicManagementService interface.
 */
public class TopicManagementServiceImpl implements TopicManagementService {

    private static final Log log = LogFactory.getLog(TopicManagementServiceImpl.class);
    private final TopicDAO topicDAO;

    /**
     * Constructor with default dependencies.
     */
    public TopicManagementServiceImpl() {

        this.topicDAO = new TopicDAOImpl();
    }

    @Override
    public void registerTopics(List<String> channelUris, String tenantDomain) throws WebSubAdapterException {

        if (channelUris == null || channelUris.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TOPIC_URIS);
        }

        if (tenantDomain == null || tenantDomain.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TENANT_DOMAIN);
        }

        for (String channelUri : channelUris) {
            String topicUri = constructHubTopic(channelUri, tenantDomain);
            try {
                if (topicUri.isEmpty()) {
                    throw handleClientException(ERROR_INVALID_TOPIC);
                }

                // Check if topic already exists in the data store
                if (topicDAO.isTopicExists(topicUri, tenantDomain)) {
                    log.debug("Topic already registered: " + topicUri + " for tenant: " + tenantDomain);
                    continue;
                }

                registerTopic(topicUri, tenantDomain);

                Topic topic = new Topic(topicUri, tenantDomain);
                try {
                    topicDAO.addTopic(topic);

                    log.debug("Successfully registered and persisted topic: " + topicUri +
                            " for tenant: " + tenantDomain);
                } catch (WebSubAdapterException e) {
                    throw handleServerException(ERROR_TOPIC_PERSISTENCE, e, topicUri, tenantDomain);
                }
            } catch (WebSubAdapterException e) {
                throw handleServerException(ERROR_REGISTERING_HUB_TOPIC, e, topicUri, tenantDomain);
            }
        }
    }

    @Override
    public void deregisterTopics(List<String> channelUris, String tenantDomain) throws WebSubAdapterException {

        if (channelUris == null || channelUris.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TOPIC_URIS);
        }

        if (tenantDomain == null || tenantDomain.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TENANT_DOMAIN);
        }

        for (String channelUri : channelUris) {
            String topicUri = constructHubTopic(channelUri, tenantDomain);
            try {
                if (topicUri.isEmpty()) {
                    throw handleClientException(ERROR_INVALID_TOPIC);
                }

                if (!topicDAO.isTopicExists(topicUri, tenantDomain)) {
                    log.debug("Topic not registered: " + topicUri + " for tenant: " + tenantDomain +
                            ". Skipping deregistration.");
                    continue;
                }

                deregisterTopic(topicUri, tenantDomain);

                topicDAO.deleteTopic(topicUri, tenantDomain);

                log.debug("Successfully deregistered and removed topic: " + topicUri +
                        " for tenant: " + tenantDomain);
            } catch (WebSubAdapterException e) {
                String warnMsg = "Failed to deregister topic: " + topicUri + " for tenant: " + tenantDomain;
                log.warn(warnMsg, e);
            }
        }
    }

    @Override
    public List<Topic> getAllTopics(String tenantDomain) throws WebSubAdapterException {

        if (tenantDomain == null || tenantDomain.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TENANT_DOMAIN);
        }

        return topicDAO.getAllTopics(tenantDomain);
    }

    @Override
    public boolean isTopicRegistered(String channelUri, String tenantDomain) throws WebSubAdapterException {

        if (channelUri == null || channelUri.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TOPIC);
        }

        if (tenantDomain == null || tenantDomain.isEmpty()) {
            throw handleClientException(ERROR_INVALID_TENANT_DOMAIN);
        }

        String topicUri = constructHubTopic(channelUri, tenantDomain);
        return topicDAO.isTopicExists(topicUri, tenantDomain);
    }

    /**
     * Register a topic in the WebSubHub.
     *
     * @param eventUri     Event URI.
     * @param tenantDomain Tenant domain.
     * @throws WebSubAdapterException If an error occurs while registering the topic.
     */
    public void registerTopic(String eventUri, String tenantDomain) throws WebSubAdapterException {

        makeTopicMgtAPICall(constructHubTopic(eventUri, tenantDomain), getWebSubBaseURL(),
                WebSubHubAdapterConstants.Http.REGISTER, tenantDomain);
        log.debug("WebSubHub Topic registered successfully for the event: " + eventUri + " in tenant: " +
                tenantDomain);
    }

    /**
     * Deregister a topic in the WebSubHub.
     *
     * @param eventUri     Event URI.
     * @param tenantDomain Tenant domain.
     * @throws WebSubAdapterException If an error occurs while deregistering the topic.
     */
    public void deregisterTopic(String eventUri, String tenantDomain) throws WebSubAdapterException {

        makeTopicMgtAPICall(constructHubTopic(eventUri, tenantDomain),
                getWebSubBaseURL(), DEREGISTER, tenantDomain);
    }

    private void makeTopicMgtAPICall(String topic, String webSubHubBaseUrl, String operation, String tenantDomain)
            throws WebSubAdapterException {

        String topicMgtUrl = buildURL(topic, webSubHubBaseUrl, operation);

        ClientManager clientManager = WebSubHubAdapterDataHolder.getInstance().getClientManager();
        HttpPost httpPost = clientManager.createHttpPost(topicMgtUrl, null);

        WebSubHubCorrelationLogUtils.triggerCorrelationLogForRequest(httpPost);
        final long requestStartTime = System.currentTimeMillis();

        try (CloseableHttpResponse response = (CloseableHttpResponse) clientManager.execute(httpPost)) {
            handleTopicMgtResponse(response, httpPost, topic, operation, requestStartTime);
        } catch (IOException | WebSubAdapterException e) {
            throw handleServerException(ERROR_REGISTERING_HUB_TOPIC, e, topic, tenantDomain);
        }
    }

    private void handleTopicMgtResponse(CloseableHttpResponse response, HttpPost httpPost,
                                        String topic, String operation, long requestStartTime)
            throws IOException, WebSubAdapterException {

        StatusLine statusLine = response.getStatusLine();
        int responseCode = statusLine.getStatusCode();
        String responsePhrase = statusLine.getReasonPhrase();

        if (responseCode == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(httpPost, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.COMPLETED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            handleSuccessfulOperation(entity, topic, operation);
        } else if ((responseCode == HttpStatus.SC_CONFLICT && operation.equals(REGISTER)) ||
                (responseCode == HttpStatus.SC_NOT_FOUND && operation.equals(DEREGISTER))) {
            HttpEntity entity = response.getEntity();
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(httpPost, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            handleErrorResponse(entity, topic, operation);
        } else {
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(httpPost, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.CANCELLED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            if (responseCode == HttpStatus.SC_FORBIDDEN) {
                handleForbiddenResponse(response, topic);
            }
            HttpEntity entity = response.getEntity();
            handleFailedOperation(entity, topic, operation, responseCode);
        }
    }

    private static void handleForbiddenResponse(CloseableHttpResponse response, String topic) throws IOException,
            WebSubAdapterException {

        Map<String, String> hubResponse = parseEventHubResponse(response);
        if (!hubResponse.isEmpty() && hubResponse.containsKey(HUB_REASON)) {
            String errorMsg = String.format(ERROR_TOPIC_DEREG_FAILURE_ACTIVE_SUBS, topic);
            if (errorMsg.equals(hubResponse.get(HUB_REASON))) {
                log.debug(String.format(TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS.getDescription(),
                        topic, hubResponse.get(HUB_ACTIVE_SUBS)));
                throw handleClientException(TOPIC_DEREGISTRATION_FAILURE_ACTIVE_SUBS, topic,
                        hubResponse.get(HUB_ACTIVE_SUBS));
            }
        }
    }
}
