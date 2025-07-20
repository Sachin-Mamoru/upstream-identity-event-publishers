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

package org.wso2.identity.event.http.publisher.internal.service.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherServerException;
import org.wso2.carbon.identity.event.publisher.api.model.EventContext;
import org.wso2.carbon.identity.event.publisher.api.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.event.publisher.api.service.EventPublisher;
import org.wso2.carbon.identity.webhook.management.api.exception.WebhookMgtException;
import org.wso2.carbon.identity.webhook.management.api.model.Webhook;
import org.wso2.identity.event.http.publisher.api.exception.HTTPAdapterException;
import org.wso2.identity.event.http.publisher.internal.component.ClientManager;
import org.wso2.identity.event.http.publisher.internal.component.HTTPAdapterDataHolder;
import org.wso2.identity.event.http.publisher.internal.constant.HTTPAdapterConstants;
import org.wso2.identity.event.http.publisher.internal.util.HTTPCorrelationLogUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.wso2.identity.event.http.publisher.internal.constant.ErrorMessage.ERROR_ACTIVE_WEBHOOKS_RETRIEVAL;
import static org.wso2.identity.event.http.publisher.internal.constant.ErrorMessage.ERROR_PUBLISHING_EVENT;
import static org.wso2.identity.event.http.publisher.internal.util.HTTPAdapterUtil.logDiagnosticFailure;
import static org.wso2.identity.event.http.publisher.internal.util.HTTPAdapterUtil.logDiagnosticSuccess;
import static org.wso2.identity.event.http.publisher.internal.util.HTTPAdapterUtil.logPublishingEvent;
import static org.wso2.identity.event.http.publisher.internal.util.HTTPCorrelationLogUtils.handleResponseCorrelationLog;

/**
 * OSGi service for publishing events using http adapter.
 */
public class HTTPEventPublisherImpl implements EventPublisher {

    private static final Log log = LogFactory.getLog(HTTPEventPublisherImpl.class);
    private List<Webhook> activeWebhooks;

    @Override
    public String getAssociatedAdapter() {

        return HTTPAdapterConstants.HTTP_ADAPTER_NAME;
    }

    @Override
    public void publish(SecurityEventTokenPayload eventPayload, EventContext eventContext)
            throws EventPublisherException {

        try {
            makeAsyncAPICall(eventPayload, eventContext);
        } catch (HTTPAdapterException e) {
            throw new EventPublisherServerException(ERROR_PUBLISHING_EVENT.getMessage(),
                    ERROR_PUBLISHING_EVENT.getDescription(), ERROR_PUBLISHING_EVENT.getCode(), e);
        }
    }

    @Override
    public boolean canHandleEvent(EventContext eventContext) throws EventPublisherException {

        try {
            activeWebhooks = HTTPAdapterDataHolder.getInstance().getWebhookManagementService()
                    .getActiveWebhooks(eventContext.getEventProfileName(), eventContext.getEventProfileVersion(),
                            eventContext.getEventUri(), eventContext.getTenantDomain());
            return !activeWebhooks.isEmpty();
        } catch (WebhookMgtException e) {
            throw new EventPublisherServerException(ERROR_ACTIVE_WEBHOOKS_RETRIEVAL.getMessage(),
                    ERROR_ACTIVE_WEBHOOKS_RETRIEVAL.getDescription(), ERROR_ACTIVE_WEBHOOKS_RETRIEVAL.getCode(), e);
        }
    }

    private void makeAsyncAPICall(SecurityEventTokenPayload eventPayload, EventContext eventContext)
            throws HTTPAdapterException {

        ClientManager clientManager = HTTPAdapterDataHolder.getInstance().getClientManager();

        for (Webhook webhook : activeWebhooks) {
            String url = webhook.getEndpoint();
            String secret = webhook.getSecret();

            HttpPost request = clientManager.createHttpPost(url, eventPayload, secret);

            logPublishingEvent(url, eventContext, eventContext.getEventUri());

            final long requestStartTime = System.currentTimeMillis();

            CompletableFuture<HttpResponse> future = clientManager.executeAsync(request);

            future.thenAccept(response -> handleAsyncResponse(
                            response, request, requestStartTime, eventContext, url, eventContext.getEventUri()))
                    .exceptionally(ex -> {
                        handleResponseCorrelationLog(request, requestStartTime,
                                HTTPCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                                ex.getMessage());
                        log.warn("Publishing event data to HTTP failed. ", ex);
                        return null;
                    });
            log.debug("Event published successfully to the HTTP. Endpoint: " + url);
        }
    }

    private static void handleAsyncResponse(HttpResponse response, HttpPost request, long requestStartTime,
                                            EventContext eventContext, String url, String eventUri) {

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(eventContext.getTenantDomain());

        try {
            int responseCode = response.getStatusLine().getStatusCode();
            String responsePhrase = response.getStatusLine().getReasonPhrase();
            log.debug("HTTP request completed. Response code: " + responseCode);

            handleResponseCorrelationLog(request, requestStartTime,
                    HTTPCorrelationLogUtils.RequestStatus.COMPLETED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            if (responseCode >= 200 && responseCode < 300) {
                logDiagnosticSuccess(eventContext, url, eventUri);
                log.debug("HTTP request completed. Response code: " + responseCode);
            } else {
                logDiagnosticFailure(eventContext, url, eventUri);
                log.debug("HTTP request failed with response code: " + responseCode +
                        " due to: " + responsePhrase);
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }
}
