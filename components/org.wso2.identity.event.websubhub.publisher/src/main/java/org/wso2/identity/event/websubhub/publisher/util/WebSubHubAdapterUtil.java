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

package org.wso2.identity.event.websubhub.publisher.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.MDC;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.utils.DiagnosticLog;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterClientException;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterServerException;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils.CORRELATION_ID_MDC;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_INVALID_WEB_SUB_HUB_BASE_URL;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.CORRELATION_ID_REQUEST_HEADER;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_CALLBACK;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_MODE;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.HUB_TOPIC;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.RESPONSE_FOR_SUCCESSFUL_OPERATION;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.URL_KEY_VALUE_SEPARATOR;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.URL_PARAM_SEPARATOR;

/**
 * Utility class for WebSubHubAdapter.
 */
public class WebSubHubAdapterUtil {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterUtil.class);

    private WebSubHubAdapterUtil() {

    }

    /**
     * Get the correlation ID.
     *
     * @return Correlation ID.
     */
    public static String getCorrelationID() {

        String correlationID = MDC.get(CORRELATION_ID_MDC);
        if (StringUtils.isBlank(correlationID)) {
            correlationID = UUID.randomUUID().toString();
            MDC.put(CORRELATION_ID_MDC, correlationID);
        }
        return correlationID;
    }

    /**
     * Handle client exceptions.
     *
     * @param error Error message.
     * @param data  Data.
     * @return WebSubAdapterClientException.
     */
    public static WebSubAdapterClientException handleClientException(
            WebSubHubAdapterConstants.ErrorMessages error, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new WebSubAdapterClientException(error.getMessage(), description, error.getCode());
    }

    /**
     * Handle server exceptions.
     *
     * @param error     Error message.
     * @param throwable Throwable.
     * @param data      Data.
     * @return WebSubAdapterServerException.
     */
    public static WebSubAdapterServerException handleServerException(
            WebSubHubAdapterConstants.ErrorMessages error, Throwable throwable, String... data) {

        String description = error.getDescription();
        if (ArrayUtils.isNotEmpty(data)) {
            description = String.format(description, data);
        }
        return new WebSubAdapterServerException(error.getMessage(), description, error.getCode(), throwable);
    }

    /**
     * Parse the response from the event hub.
     *
     * @param response HTTP response.
     * @return Parsed response as a map.
     * @throws IOException If an error occurs while reading the response.
     */
    public static Map<String, String> parseEventHubResponse(CloseableHttpResponse response) throws IOException {

        Map<String, String> map = new HashMap<>();
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            String responseContent = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            log.debug("Parsing response content from event hub: " + responseContent);
            String[] responseParams = responseContent.split(URL_PARAM_SEPARATOR);
            for (String param : responseParams) {
                String[] keyValuePair = param.split(URL_KEY_VALUE_SEPARATOR);
                if (keyValuePair.length == 2) {
                    map.put(keyValuePair[0], keyValuePair[1]);
                }
            }
        }
        return map;
    }

    /**
     * Handle the response correlation log.
     *
     * @param request          HTTP POST request.
     * @param requestStartTime Request start time.
     * @param otherParams      Other parameters.
     */
    public static void handleResponseCorrelationLog(HttpPost request, long requestStartTime, String... otherParams) {

        try {
            MDC.put(CORRELATION_ID_MDC, request.getFirstHeader(CORRELATION_ID_REQUEST_HEADER).getValue());
            WebSubHubCorrelationLogUtils.triggerCorrelationLogForResponse(request, requestStartTime, otherParams);
        } finally {
            MDC.remove(CORRELATION_ID_MDC);
        }
    }

    /**
     * Log the diagnostic success.
     *
     * @param eventContext Event context.
     * @param url          URL.
     * @param topic        Topic.
     */
    public static void logDiagnosticSuccess(EventContext eventContext, String url, String topic) {

        if (LoggerUtils.isDiagnosticLogsEnabled()) {
            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog
                    .DiagnosticLogBuilder(WebSubHubAdapterConstants.LogConstants.WEB_SUB_HUB_ADAPTER,
                    WebSubHubAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT);
            diagnosticLogBuilder
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.URL, url)
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TENANT_DOMAIN,
                            eventContext.getTenantDomain())
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TOPIC, topic)
                    .resultMessage("Event data published to WebSubHub.")
                    .resultStatus(DiagnosticLog.ResultStatus.SUCCESS)
                    .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
            LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
        }
    }

    /**
     * Log the diagnostic failure.
     *
     * @param eventContext Event context.
     * @param url          URL.
     * @param topic        Topic.
     */
    public static void logDiagnosticFailure(EventContext eventContext, String url, String topic) {

        if (LoggerUtils.isDiagnosticLogsEnabled()) {
            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog
                    .DiagnosticLogBuilder(WebSubHubAdapterConstants.LogConstants.WEB_SUB_HUB_ADAPTER,
                    WebSubHubAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT);
            diagnosticLogBuilder
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.URL, url)
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TENANT_DOMAIN,
                            eventContext.getTenantDomain())
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TOPIC, topic)
                    .resultMessage("Failed to publish event data to WebSubHub.")
                    .resultStatus(DiagnosticLog.ResultStatus.FAILED)
                    .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
            LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
        }
    }

    /**
     * Log the publishing event.
     *
     * @param url          URL.
     * @param eventContext Event context.
     * @param topic        Topic.
     */
    public static void logPublishingEvent(String url, EventContext eventContext, String topic) {

        log.debug("Publishing event data to WebSubHub. URL: " + url + " tenant domain: " +
                eventContext.getTenantDomain());
        if (LoggerUtils.isDiagnosticLogsEnabled()) {
            DiagnosticLog.DiagnosticLogBuilder diagnosticLogBuilder = new DiagnosticLog.DiagnosticLogBuilder(
                    WebSubHubAdapterConstants.LogConstants.WEB_SUB_HUB_ADAPTER,
                    WebSubHubAdapterConstants.LogConstants.ActionIDs.PUBLISH_EVENT);
            diagnosticLogBuilder
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.URL, url)
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TENANT_DOMAIN,
                            eventContext.getTenantDomain())
                    .inputParam(WebSubHubAdapterConstants.LogConstants.InputKeys.TOPIC, topic)
                    .resultMessage("Publishing event data to WebSubHub.")
                    .resultStatus(DiagnosticLog.ResultStatus.SUCCESS)
                    .logDetailLevel(DiagnosticLog.LogDetailLevel.INTERNAL_SYSTEM);
            LoggerUtils.triggerDiagnosticLogEvent(diagnosticLogBuilder);
        }
    }

    /**
     * Build URL for WebSub Hub operations.
     *
     * @param topic            Topic.
     * @param webSubHubBaseUrl WebSub Hub base URL.
     * @param operation        Operation.
     * @return URL.
     * @throws WebSubAdapterServerException If an error occurs while building the URL.
     */
    public static String buildURL(String topic, String webSubHubBaseUrl, String operation)
            throws WebSubAdapterServerException {

        try {
            URIBuilder uriBuilder = new URIBuilder(webSubHubBaseUrl);
            uriBuilder.addParameter(HUB_MODE, operation);
            uriBuilder.addParameter(HUB_TOPIC, topic);
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            log.error("Error building URL", e);
            throw handleServerException(ERROR_INVALID_WEB_SUB_HUB_BASE_URL, e);
        }
    }

    /**
     * Build URL for WebSub Hub subscription operations.
     *
     * @param topic            Topic.
     * @param webSubHubBaseUrl WebSub Hub base URL.
     * @param operation        Operation.
     * @param callbackUrl      Callback URL.
     * @return URL.
     * @throws WebSubAdapterServerException If an error occurs while building the URL.
     */
    public static String buildSubscriptionURL(String topic, String webSubHubBaseUrl, String operation,
                                              String callbackUrl)
            throws WebSubAdapterServerException {

        try {
            URIBuilder uriBuilder = new URIBuilder(webSubHubBaseUrl);
            uriBuilder.addParameter(HUB_MODE, operation);
            uriBuilder.addParameter(HUB_TOPIC, topic);
            uriBuilder.addParameter(HUB_CALLBACK, callbackUrl);
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            log.error("Error building subscription URL", e);
            throw handleServerException(ERROR_INVALID_WEB_SUB_HUB_BASE_URL, e);
        }
    }

    /**
     * Construct the hub topic by combining tenant domain and topic suffix.
     *
     * @param topicSuffix  Topic suffix.
     * @param tenantDomain Tenant domain.
     * @return Hub topic.
     */
    public static String constructHubTopic(String topicSuffix, String tenantDomain) {

        return tenantDomain + WebSubHubAdapterConstants.Http.TOPIC_SEPARATOR + topicSuffix;
    }

    /**
     * Get the WebSub Hub base URL from configuration.
     *
     * @return WebSub Hub base URL.
     * @throws WebSubAdapterException If the WebSub Hub base URL is not configured.
     */
    public static String getWebSubBaseURL() throws WebSubAdapterException {

        String webSubHubBaseUrl =
                WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getWebSubHubBaseUrl();

        if (StringUtils.isEmpty(webSubHubBaseUrl)) {
            throw handleClientException(WebSubHubAdapterConstants.ErrorMessages.WEB_SUB_BASE_URL_NOT_CONFIGURED);
        }
        return webSubHubBaseUrl;
    }

    /**
     * Handle successful WebSub Hub operations.
     *
     * @param entity    HTTP entity.
     * @param topic     Topic.
     * @param operation Operation.
     * @throws WebSubAdapterException If an error occurs while handling the response.
     * @throws IOException            If an error occurs while reading the response.
     */
    public static void handleSuccessfulOperation(HttpEntity entity, String topic, String operation)
            throws WebSubAdapterException, IOException {

        if (entity != null) {
            String responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (RESPONSE_FOR_SUCCESSFUL_OPERATION.equals(responseString)) {
                log.debug("Success WebSub Hub operation: " + operation + ", topic: " + topic);
            } else {
                throw handleServerException(ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB, null,
                        topic, operation, responseString);
            }
        } else {
            String message = String.format(ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB.getDescription(), topic, operation);
            throw new WebSubAdapterServerException(message, ERROR_EMPTY_RESPONSE_FROM_WEBSUB_HUB.getCode());
        }
    }

    /**
     * Handle error responses (e.g., conflict or not found) from WebSub Hub.
     *
     * @param entity    HTTP entity.
     * @param topic     Topic.
     * @param operation Operation.
     * @throws IOException If an error occurs while reading the response.
     */
    public static void handleErrorResponse(HttpEntity entity, String topic, String operation) throws IOException {

        String responseString = "";
        if (entity != null) {
            responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
        log.warn(String.format(ERROR_INVALID_RESPONSE_FROM_WEBSUB_HUB.getDescription(),
                topic, operation, responseString));
    }

    /**
     * Handle failed WebSub Hub operations.
     *
     * @param entity       HTTP entity.
     * @param topic        Topic.
     * @param operation    Operation.
     * @param responseCode Response code.
     * @throws IOException            If an error occurs while reading the response.
     * @throws WebSubAdapterException If an error occurs while handling the response.
     */
    public static void handleFailedOperation(HttpEntity entity, String topic, String operation, int responseCode)
            throws IOException, WebSubAdapterException {

        String responseString = "";
        if (entity != null) {
            responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
        String message = String.format(ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB.getDescription(),
                topic, operation, responseString);
        log.error(message + ", Response code:" + responseCode);
        throw new WebSubAdapterServerException(message, ERROR_BACKEND_ERROR_FROM_WEBSUB_HUB.getCode());
    }
}
