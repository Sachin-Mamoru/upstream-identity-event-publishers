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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.wso2.carbon.identity.webhook.management.api.service.WebhookSubscriber;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubCorrelationLogUtils;

import java.io.IOException;
import java.util.List;

import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_SUBSCRIBING_TO_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.buildSubscriptionURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.constructHubTopic;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.getWebSubBaseURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleErrorResponse;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleFailedOperation;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleSuccessfulOperation;

/**
 * OSGi service for managing WebSub Hub subscriptions.
 * TODO: Introduce a proper exception handler for the subscriber with explicit error codes.
 */
public class WebSubEventSubscriberImpl implements WebhookSubscriber {

    private static final Log log = LogFactory.getLog(WebSubEventSubscriberImpl.class);

    @Override
    public String getName() {

        return WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME;
    }

    @Override
    public boolean subscribe(List<String> topics, String callbackUrl, String tenantDomain) {

        try {
            for (String topic : topics) {
                try {
                    makeSubscriptionAPICall(constructHubTopic(topic, tenantDomain),
                            getWebSubBaseURL(),
                            WebSubHubAdapterConstants.Http.SUBSCRIBE,
                            callbackUrl);
                    log.debug("WebSub Hub subscription successful for topic: " + topic +
                            " with callback URL: " + callbackUrl + " in tenant: " + tenantDomain);
                } catch (WebSubAdapterException e) {
                    log.error("Error subscribing to topic: " + topic, e);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during subscription process", e);
            return false;
        }
    }

    @Override
    public boolean unsubscribe(List<String> topics, String callbackUrl, String tenantDomain) {

        try {
            for (String topic : topics) {
                try {
                    makeSubscriptionAPICall(constructHubTopic(topic, tenantDomain),
                            getWebSubBaseURL(),
                            WebSubHubAdapterConstants.Http.UNSUBSCRIBE,
                            callbackUrl);
                    log.debug("WebSub Hub unsubscription successful for topic: " + topic +
                            " with callback URL: " + callbackUrl + " in tenant: " + tenantDomain);
                } catch (WebSubAdapterException e) {
                    log.error("Error unsubscribing to topic: " + topic, e);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during unsubscription process", e);
            return false;
        }
    }

    private void makeSubscriptionAPICall(String topic, String webSubHubBaseUrl, String operation, String callbackUrl)
            throws WebSubAdapterException {

        String subscriptionUrl = buildSubscriptionURL(topic, webSubHubBaseUrl, operation, callbackUrl);

        ClientManager clientManager = WebSubHubAdapterDataHolder.getInstance().getClientManager();
        HttpPost httpPost = clientManager.createHttpPost(subscriptionUrl, null);

        WebSubHubCorrelationLogUtils.triggerCorrelationLogForRequest(httpPost);
        final long requestStartTime = System.currentTimeMillis();

        try (CloseableHttpResponse response = (CloseableHttpResponse) clientManager.execute(httpPost)) {
            handleSubscriptionResponse(response, httpPost, topic, operation, requestStartTime);
        } catch (IOException | WebSubAdapterException e) {
            throw handleServerException(ERROR_SUBSCRIBING_TO_TOPIC, e);
        }
    }

    private void handleSubscriptionResponse(CloseableHttpResponse response, HttpPost httpPost,
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
        } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
            HttpEntity entity = response.getEntity();
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(httpPost, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.FAILED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            handleErrorResponse(entity, topic, operation);
        } else {
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(httpPost, requestStartTime,
                    WebSubHubCorrelationLogUtils.RequestStatus.CANCELLED.getStatus(),
                    String.valueOf(responseCode), responsePhrase);
            HttpEntity entity = response.getEntity();
            handleFailedOperation(entity, topic, operation, responseCode);
        }
    }
}
