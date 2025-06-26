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
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.webhook.management.api.exception.WebhookMgtException;
import org.wso2.carbon.identity.webhook.management.api.model.Webhook;
import org.wso2.carbon.identity.webhook.management.api.model.Subscription;
import org.wso2.carbon.identity.webhook.management.api.model.SubscriptionStatus;
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
 * TODO: Add diagnostic logs
 */
public class WebSubEventSubscriberImpl implements EventSubscriber {

    private static final Log log = LogFactory.getLog(WebSubEventSubscriberImpl.class);
    private static final String eventProfileVersion = "v1";

    @Override
    public String getName() {

        return WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME;
    }

    @Override
    public List<Subscription> subscribe(Webhook webhook) {

        List<Subscription> subscriptions = new ArrayList<>();
        String tenantDomain = IdentityTenantUtil.getTenantDomain(webhook.getTenantId());
        try {
            for (Subscription eventSubscription : webhook.getEventsSubscribed()) {
                try {
                    makeSubscriptionAPICall(
                            constructHubTopic(eventSubscription.getChannelUri(), eventProfileVersion, tenantDomain),
                            getWebSubBaseURL(), WebSubHubAdapterConstants.Http.SUBSCRIBE, webhook.getEndpoint(),
                            webhook.getSecret());
                    log.debug("WebSubHub subscription successful for channel: " + eventSubscription.getChannelUri() +
                            " with endpoint: " + webhook.getEndpoint() + " in tenant: " + tenantDomain);

                    Subscription subscription = Subscription.builder()
                            .channelUri(eventSubscription.getChannelUri())
                            .status(SubscriptionStatus.SUBSCRIPTION_ACCEPTED)
                            .build();
                    subscriptions.add(subscription);
                } catch (WebSubAdapterException e) {
                    log.debug("Error subscribing to channel" + eventSubscription.getChannelUri() + " with endpoint: " +
                            webhook.getEndpoint() + " in tenant: " + tenantDomain + ". Error: " + e.getMessage(), e);
                    Subscription subscription = Subscription.builder()
                            .channelUri(eventSubscription.getChannelUri())
                            .status(SubscriptionStatus.SUBSCRIPTION_ERROR)
                            .build();
                    subscriptions.add(subscription);
                }
            }
        } catch (WebhookMgtException e) {
            log.debug("Error retrieving events from webhook: " + webhook.getUuid() +
                    " in tenant: " + tenantDomain + ". Error: " + e.getMessage(), e);
        }
        return subscriptions;
    }

    @Override
    public List<Subscription> unsubscribe(Webhook webhook) {

        List<Subscription> subscriptions = new ArrayList<>();
        String tenantDomain = IdentityTenantUtil.getTenantDomain(webhook.getTenantId());
        try {
            for (Subscription eventSubscription : webhook.getEventsSubscribed()) {
                try {
                    makeSubscriptionAPICall(
                            constructHubTopic(eventSubscription.getChannelUri(), eventProfileVersion, tenantDomain),
                            getWebSubBaseURL(), WebSubHubAdapterConstants.Http.UNSUBSCRIBE, webhook.getEndpoint(),
                            null);
                    log.debug("WebSubHub unsubscription successful for channel: " + eventSubscription.getChannelUri() +
                            " with endpoint: " + webhook.getEndpoint() + " in tenant: " + tenantDomain);

                    Subscription subscription = Subscription.builder()
                            .channelUri(eventSubscription.getChannelUri())
                            .status(SubscriptionStatus.UNSUBSCRIPTION_ACCEPTED)
                            .build();
                    subscriptions.add(subscription);
                } catch (WebSubAdapterException e) {
                    log.debug("Error unsubscribing from channel: " + eventSubscription.getChannelUri() +
                            " with endpoint: " + webhook.getEndpoint() +
                            " in tenant: " + tenantDomain + ". Error: " + e.getMessage(), e);

                    Subscription subscription = Subscription.builder()
                            .channelUri(eventSubscription.getChannelUri())
                            .status(SubscriptionStatus.UNSUBSCRIPTION_ERROR)
                            .build();
                    subscriptions.add(subscription);
                }
            }
        } catch (WebhookMgtException e) {
            log.debug("Error retrieving events from webhook: " + webhook.getUuid() +
                    " in tenant: " + IdentityTenantUtil.getTenantDomain(webhook.getTenantId()) +
                    ". Error: " + e.getMessage(), e);
        }
        return subscriptions;
    }

    private void makeSubscriptionAPICall(String topic, String webSubHubBaseUrl, String operation, String callbackUrl,
                                         String secret) throws WebSubAdapterException {

        ClientManager clientManager = WebSubHubAdapterDataHolder.getInstance().getClientManager();

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

        try (CloseableHttpResponse response = (CloseableHttpResponse) clientManager.executeSubscriberRequest(
                httpPost)) {
            handleSubscriptionResponse(response, httpPost, topic, operation, requestStartTime);
        } catch (IOException | WebSubAdapterException e) {
            log.debug("Error subscribing to topic: " + topic + ". Error: " + e.getMessage(), e);
            throw handleServerException(ERROR_SUBSCRIBING_TO_TOPIC, e);
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
