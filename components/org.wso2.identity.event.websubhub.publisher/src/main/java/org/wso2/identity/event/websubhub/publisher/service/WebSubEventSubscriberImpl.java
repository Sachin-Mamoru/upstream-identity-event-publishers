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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.wso2.carbon.identity.webhook.management.api.exception.WebhookMgtException;
import org.wso2.carbon.identity.webhook.management.api.service.EventSubscriber;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubCorrelationLogUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_SUBSCRIBING_TO_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_CALLBACK;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_MODE;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_SECRET;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.constructHubTopic;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.getWebSubBaseURL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleErrorResponse;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleFailedOperation;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleSuccessfulOperation;

/**
 * OSGi service for managing WebSubHub subscriptions.
 * TODO: Introduce a proper exception handler for the subscriber with explicit error codes.
 * TODO: Add diagnostic logs
 */
public class WebSubEventSubscriberImpl implements EventSubscriber {

    private static final Log log = LogFactory.getLog(WebSubEventSubscriberImpl.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 400;

    @Override
    public String getName() {

        return WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME;
    }

    @Override
    public void subscribe(List<String> channels, String eventProfileVersion, String endpoint, String secret,
                          String tenantDomain) throws WebhookMgtException {

        for (String channel : channels) {
            try {
                makeSubscriptionAPICall(constructHubTopic(channel, eventProfileVersion, tenantDomain),
                        getWebSubBaseURL(), WebSubHubAdapterConstants.Http.SUBSCRIBE, endpoint, secret);
                log.debug("WebSubHub subscription successful for channel: " + channel +
                        " with endpoint: " + endpoint + " in tenant: " + tenantDomain);
            } catch (WebSubAdapterException e) {
                throw new WebhookMgtException(e.getMessage(), e);
            }
        }
    }

    @Override
    public void unsubscribe(List<String> channels, String eventProfileVersion, String endpoint,
                            String tenantDomain) throws WebhookMgtException {

        for (String channel : channels) {
            try {
                makeSubscriptionAPICall(constructHubTopic(channel, eventProfileVersion, tenantDomain),
                        getWebSubBaseURL(), WebSubHubAdapterConstants.Http.UNSUBSCRIBE, endpoint, null);
                log.debug("WebSubHub unsubscription successful for channel: " + channel +
                        " with endpoint: " + endpoint + " in tenant: " + tenantDomain);
            } catch (WebSubAdapterException e) {
                throw new WebhookMgtException(e.getMessage(), e);
            }
        }
    }

    private void makeSubscriptionAPICall(String topic, String webSubHubBaseUrl, String operation, String callbackUrl,
                                         String secret) throws WebSubAdapterException {

        ClientManager clientManager = WebSubHubAdapterDataHolder.getInstance().getClientManager();

        int attempt = 0;
        while (true) {
            HttpPost httpPost = clientManager.createHttpPost(webSubHubBaseUrl, null);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(HUB_CALLBACK, callbackUrl));
            params.add(new BasicNameValuePair(HUB_MODE, operation));
            params.add(new BasicNameValuePair(HUB_TOPIC, topic));
            if (secret != null) {
                params.add(new BasicNameValuePair(HUB_SECRET, secret));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            WebSubHubCorrelationLogUtils.triggerCorrelationLogForRequest(httpPost);
            final long requestStartTime = System.currentTimeMillis();

            try (CloseableHttpResponse response = (CloseableHttpResponse) clientManager.execute(httpPost)) {
                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode >= 500 && attempt < MAX_RETRIES) {
                    attempt++;
                    log.debug("Retrying subscription API call, attempt " + attempt + " for topic: " + topic);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw handleServerException(ERROR_SUBSCRIBING_TO_TOPIC, ie);
                    }
                    continue;
                }
                handleSubscriptionResponse(response, httpPost, topic, operation, requestStartTime);
                break;
            } catch (IOException | WebSubAdapterException e) {
                log.debug("Error subscribing to topic: " + topic + ". Error: " + e.getMessage(), e);
                if (attempt < MAX_RETRIES) {
                    attempt++;
                    log.debug("Retrying subscription API call, attempt " + attempt + " for topic: " + topic);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw handleServerException(ERROR_SUBSCRIBING_TO_TOPIC, ie);
                    }
                    continue;
                }
                throw handleServerException(ERROR_SUBSCRIBING_TO_TOPIC, e);
            }
        }
    }

    private void handleSubscriptionResponse(CloseableHttpResponse response, HttpPost httpPost,
                                            String topic, String operation, long requestStartTime)
            throws IOException, WebSubAdapterException {

        StatusLine statusLine = response.getStatusLine();
        int responseCode = statusLine.getStatusCode();
        String responsePhrase = statusLine.getReasonPhrase();

        if (responseCode == HttpStatus.SC_ACCEPTED) {
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
