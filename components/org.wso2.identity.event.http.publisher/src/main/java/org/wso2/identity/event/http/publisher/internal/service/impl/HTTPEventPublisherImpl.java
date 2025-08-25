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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.MDC;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherServerException;
import org.wso2.carbon.identity.event.publisher.api.model.EventContext;
import org.wso2.carbon.identity.event.publisher.api.model.SecurityEventTokenPayload;
import org.wso2.carbon.identity.event.publisher.api.service.EventPublisher;
import org.wso2.carbon.identity.webhook.management.api.exception.WebhookMgtException;
import org.wso2.carbon.identity.webhook.management.api.model.Webhook;
import org.wso2.carbon.utils.DiagnosticLog;
import org.wso2.identity.event.http.publisher.api.exception.HTTPAdapterException;
import org.wso2.identity.event.http.publisher.internal.component.ClientManager;
import org.wso2.identity.event.http.publisher.internal.component.HTTPAdapterDataHolder;
import org.wso2.identity.event.http.publisher.internal.constant.HTTPAdapterConstants;
import org.wso2.identity.event.http.publisher.internal.util.HTTPAdapterUtil;
import org.wso2.identity.event.http.publisher.internal.util.HTTPCorrelationLogUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.CORRELATION_ID_MDC;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.TENANT_DOMAIN;
import static org.wso2.identity.event.http.publisher.internal.constant.ErrorMessage.ERROR_ACTIVE_WEBHOOKS_RETRIEVAL;
import static org.wso2.identity.event.http.publisher.internal.util.HTTPAdapterUtil.printPublisherDiagnosticLog;
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

        makeAsyncAPICall(eventPayload, eventContext);
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

    private void makeAsyncAPICall(SecurityEventTokenPayload eventPayload, EventContext eventContext) {

        for (Webhook webhook : activeWebhooks) {
            String url = webhook.getEndpoint();
            String secret = webhook.getSecret();
            sendWithRetries(eventPayload, eventContext, url, secret,
                    HTTPAdapterDataHolder.getInstance().getClientManager().getMaxRetries());
        }
    }

    private void sendWithRetries(SecurityEventTokenPayload eventPayload, EventContext eventContext,
                                 String url, String secret, int retriesLeft) {

        ClientManager clientManager = HTTPAdapterDataHolder.getInstance().getClientManager();
        final HttpPost request;
        try {
            request = clientManager.createHttpPost(url, eventPayload, secret);
        } catch (HTTPAdapterException e) {
            printPublisherDiagnosticLog(eventContext, eventPayload, url,
                    HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT, DiagnosticLog.ResultStatus.FAILED,
                    "Failed to construct HTTP request for HTTP adapter publish.");
            log.debug("Error constructing HTTP request for HTTP adapter publish. No retries will be attempted.", e);
            return;
        }

        printPublisherDiagnosticLog(eventContext, eventPayload, url,
                HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT, DiagnosticLog.ResultStatus.SUCCESS,
                "Publishing event data to endpoint.");

        final long requestStartTime = System.currentTimeMillis();
        final String correlationId = HTTPAdapterUtil.getCorrelationID(eventPayload);

        CompletableFuture<HttpResponse> future = clientManager.executeAsync(request);

        future.whenCompleteAsync((response, throwable) -> {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(eventContext.getTenantDomain());
                if (StringUtils.isNotEmpty(correlationId)) {
                    MDC.put(CORRELATION_ID_MDC, correlationId);
                }
                MDC.put(TENANT_DOMAIN, eventContext.getTenantDomain());
                if (throwable == null) {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        handleResponseCorrelationLog(request, requestStartTime,
                                HTTPCorrelationLogUtils.RequestStatus.COMPLETED.getStatus(),
                                String.valueOf(status), response.getStatusLine().getReasonPhrase());
                        printPublisherDiagnosticLog(eventContext, eventPayload, url,
                                HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT,
                                DiagnosticLog.ResultStatus.SUCCESS, "Event data published to endpoint.");
                        log.debug("HTTP request completed. Response code: " + status +
                                ", Endpoint: " + url + ", Event URI: " + eventContext.getEventUri());
                    } else {
                        if (retriesLeft > 0) {
                            printPublisherDiagnosticLog(eventContext, eventPayload, url,
                                    HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT,
                                    DiagnosticLog.ResultStatus.FAILED,
                                    "Publish attempt failed with status code: " + status +
                                            ". Retrying… (" + retriesLeft + " attempts left)");
                            sendWithRetries(eventPayload, eventContext, url, secret, retriesLeft - 1);
                        }
                        handleResponseCorrelationLog(request, requestStartTime,
                                HTTPCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                                String.valueOf(status), response.getStatusLine().getReasonPhrase());
                        printPublisherDiagnosticLog(eventContext, eventPayload, url,
                                HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT,
                                DiagnosticLog.ResultStatus.FAILED,
                                "Failed to publish event data to endpoint. Status code: " + status +
                                        ". Maximum retries reached.");
                    }
                } else {
                    if (retriesLeft > 0) {
                        printPublisherDiagnosticLog(eventContext, eventPayload, url,
                                HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT,
                                DiagnosticLog.ResultStatus.FAILED,
                                "Publish attempt failed due to exception. Retrying… (" +
                                        retriesLeft + " attempts left)");
                        sendWithRetries(eventPayload, eventContext, url, secret, retriesLeft - 1);
                    }
                    handleResponseCorrelationLog(request, requestStartTime,
                            HTTPCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                            throwable.getMessage());
                    printPublisherDiagnosticLog(eventContext, eventPayload, url,
                            HTTPAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT,
                            DiagnosticLog.ResultStatus.FAILED,
                            "Failed to publish event data to endpoint. Maximum retries reached.");
                    log.warn("Failed to publish event data to endpoint: " + url + "Maximum retries reached.");
                    log.debug("Failed to publish event data to endpoint: " + url, throwable);
                }
            } finally {
                if (StringUtils.isNotEmpty(correlationId)) {
                    MDC.remove(CORRELATION_ID_MDC);
                }
                MDC.remove(TENANT_DOMAIN);
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, clientManager.getAsyncCallbackExecutor());
    }
}
