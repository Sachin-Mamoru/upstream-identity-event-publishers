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

package org.wso2.identity.event.websubhub.publisher.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;
import org.wso2.carbon.identity.event.publisher.api.model.EventContext;
import org.wso2.carbon.identity.event.publisher.api.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.event.publisher.api.service.EventPublisher;
import org.wso2.carbon.identity.topic.management.api.exception.TopicManagementException;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubCorrelationLogUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.wso2.carbon.identity.event.publisher.api.constant.ErrorMessage.ERROR_CODE_CONSTRUCTING_HUB_TOPIC;
import static org.wso2.carbon.identity.event.publisher.api.constant.ErrorMessage.ERROR_CODE_TOPIC_EXISTS_CHECK;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.PUBLISH;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.buildURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.constructHubTopic;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.getWebSubBaseURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleResponseCorrelationLog;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.logDiagnosticFailure;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.logDiagnosticSuccess;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.logPublishingEvent;

/**
 * OSGi service for publishing events using web sub hub.
 */
public class WebSubEventPublisherImpl implements EventPublisher {

    private static final Log log = LogFactory.getLog(WebSubEventPublisherImpl.class);

    @Override
    public String getAssociatedAdaptor() {

        return WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME;
    }

    @Override
    public void publish(SecurityEventTokenPayload eventPayload, EventContext eventContext)
            throws EventPublisherException {

        try {
            makeAsyncAPICall(eventPayload, eventContext,
                    constructHubTopic(eventContext.getEventUri(), eventContext.getEventProfileName(),
                            eventContext.getEventProfileVersion(), eventContext.getTenantDomain()), getWebSubBaseURL());
            log.debug("Event published successfully to the WebSubHub.");
        } catch (WebSubAdapterException e) {
            throw handleServerException(ERROR_CODE_CONSTRUCTING_HUB_TOPIC, e,
                    WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME);
        }
    }

    @Override
    public boolean canHandleEvent(EventContext eventContext) throws EventPublisherException {

        try {
            return WebSubHubAdapterDataHolder.getInstance().getTopicManagementService()
                    .isTopicExists(eventContext.getEventUri(), eventContext.getEventProfileName(),
                            eventContext.getEventProfileVersion(), eventContext.getTenantDomain());
        } catch (TopicManagementException e) {
            throw handleServerException(ERROR_CODE_TOPIC_EXISTS_CHECK, e,
                    WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME);
        }
    }

    private void makeAsyncAPICall(SecurityEventTokenPayload eventPayload, EventContext eventContext,
                                  String topic, String webSubHubBaseUrl) throws WebSubAdapterException {

        String url = buildURL(topic, webSubHubBaseUrl, PUBLISH);

        ClientManager clientManager = WebSubHubAdapterDataHolder.getInstance().getClientManager();
        HttpPost request = clientManager.createHttpPost(url, eventPayload);

        logPublishingEvent(url, eventContext, topic);

        final long requestStartTime = System.currentTimeMillis();

        CompletableFuture<HttpResponse> future = clientManager.executeAsync(request);

        future.thenAccept(response -> handleAsyncResponse
                        (response, request, requestStartTime, eventContext, url, topic))
                .exceptionally(ex -> {
                    handleResponseCorrelationLog(request, requestStartTime,
                            WebSubHubCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                            ex.getMessage());
                    log.error("Publishing event data to WebSubHub failed. ", ex);
                    throw new IdentityRuntimeException("Error occurred while publishing event data to WebSubHub. ", ex);
                });
    }

    private static void handleAsyncResponse(HttpResponse response, HttpPost request, long requestStartTime,
                                            EventContext eventContext, String url, String topic) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(eventContext.getTenantDomain());

        try {
            int responseCode = response.getStatusLine().getStatusCode();
            String responsePhrase = response.getStatusLine().getReasonPhrase();
            log.debug("WebSubHub request completed. Response code: " + responseCode);

            handleResponseCorrelationLog(request, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.COMPLETED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);

            if (responseCode == HttpStatus.SC_OK || responseCode == HttpStatus.SC_CREATED ||
                    responseCode == HttpStatus.SC_ACCEPTED || responseCode == HttpStatus.SC_NO_CONTENT) {
                logDiagnosticSuccess(eventContext, url, topic);
                try {
                    if (response.getEntity() != null) {
                        log.debug("Response data: " + EntityUtils.toString(response.getEntity()));
                    } else {
                        log.debug("Response entity is null.");
                    }
                } catch (IOException e) {
                    log.debug("Error while reading WebSubHub event publisher response. ", e);
                }
            } else {
                logDiagnosticFailure(eventContext, url, topic);
                try {
                    if (response.getEntity() != null) {
                        String errorResponseBody = EntityUtils.toString(response.getEntity());
                        log.error("WebHubSub event publisher received " + responseCode + " code. Response data: " +
                                errorResponseBody);
                    } else {
                        log.error("WebHubSub event publisher received " + responseCode +
                                " code. Response entity is null.");
                    }
                } catch (IOException e) {
                    log.error("Error while reading WebSubHub event publisher response. ", e);
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
