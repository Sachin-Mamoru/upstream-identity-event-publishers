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

import okhttp3.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class contains the utility methods for adding correlation logs for websubhub publisher.
 */
public class WebSubHubCorrelationLogUtils {

    private static final Log correlationLog = LogFactory.getLog("correlation");
    private static final String CORRELATION_LOG_SYSTEM_PROPERTY = "enableCorrelationLogs";
    private static final String CORRELATION_LOG_REQUEST_START = "HTTP-Out-Request";
    private static final String CORRELATION_LOG_REQUEST_END = "HTTP-Out-Response";
    private static final String CORRELATION_LOG_SEPARATOR = "|";
    private static Boolean isEnableCorrelationLogs;

    /**
     * Trigger correlation logs for http out request.
     *
     * @param request Http out request.
     */
    public static void triggerCorrelationLogForRequest(Request request) {

        if (isCorrelationLogsEnabled() && correlationLog.isInfoEnabled()) {
            List<String> logPropertiesList = new ArrayList<>();
            logPropertiesList.add(CORRELATION_LOG_REQUEST_START);
            logPropertiesList.add(Long.toString(System.currentTimeMillis()));
            logPropertiesList.add(request.method());
            logPropertiesList.add(request.url().query());
            logPropertiesList.add(request.url().encodedPath());

            correlationLog.info(createFormattedLog(logPropertiesList));
        }
    }

    /**
     * Trigger correlation logs for HTTP out response using OkHttp Request.
     *
     * @param request          Http out request.
     * @param requestStartTime The time when the request was sent.
     * @param otherParams      Additional parameters to be logged.
     */
    public static void triggerCorrelationLogForResponse(Request request, long requestStartTime,
                                                        String... otherParams) {

        logHttpRequestCorrelationDataInternal(
                request.method(),
                request.url().query(),
                request.url().encodedPath(),
                requestStartTime,
                otherParams
        );
    }

    /**
     * Trigger correlation logs for HTTP out request using Apache HttpClient.
     *
     * @param request          Http out request.
     * @param requestStartTime The time when the request was sent.
     * @param otherParams      Additional parameters to be logged.
     */
    public static void logHttpRequestCorrelationData(HttpEntityEnclosingRequestBase request,
                                                     long requestStartTime,
                                                     String... otherParams) {

        logHttpRequestCorrelationDataInternal(
                request.getMethod(),
                request.getURI().getQuery(),
                request.getURI().getPath(),
                requestStartTime,
                otherParams
        );
    }

    /**
     * Shared internal method to write correlation log for HTTP requests.
     * This method is used by both OkHttp and Apache HttpClient implementations.
     *
     * @param method           HTTP method of the request.
     * @param query            Query string of the request.
     * @param path             Path of the request.
     * @param requestStartTime The time when the request was sent.
     * @param otherParams      Additional parameters to be logged.
     */
    private static void logHttpRequestCorrelationDataInternal(String method, String query, String path,
                                                              long requestStartTime, String... otherParams) {

        if (isCorrelationLogsEnabled() && correlationLog.isInfoEnabled()) {
            long currentTime = System.currentTimeMillis();
            long timeTaken = currentTime - requestStartTime;

            List<String> logPropertiesList = new ArrayList<>();
            logPropertiesList.add(Long.toString(timeTaken));
            logPropertiesList.add(CORRELATION_LOG_REQUEST_END);
            logPropertiesList.add(Long.toString(requestStartTime));
            logPropertiesList.add(method);
            logPropertiesList.add(query);
            logPropertiesList.add(path);
            Collections.addAll(logPropertiesList, otherParams);

            correlationLog.info(createFormattedLog(logPropertiesList));
        }
    }

    /**
     * Is correlation logs enabled in the system.
     *
     * @return Boolean indicating correlation logs enabled or not.
     */
    private static boolean isCorrelationLogsEnabled() {

        if (isEnableCorrelationLogs == null) {
            isEnableCorrelationLogs = Boolean.parseBoolean(System.getProperty(CORRELATION_LOG_SYSTEM_PROPERTY));
        }
        return isEnableCorrelationLogs;
    }

    /**
     * Create the log line that should be printed.
     *
     * @param logPropertiesList List of log values that should be printed in the log.
     * @return The log line.
     */
    private static String createFormattedLog(List<String> logPropertiesList) {

        return String.join(CORRELATION_LOG_SEPARATOR, logPropertiesList);
    }

    /**
     * WebSubHub outgoing request status.
     */
    public enum RequestStatus {

        COMPLETED("completed"),
        FAILED("failed"),
        CANCELLED("cancelled");

        private final String status;

        RequestStatus(String status) {

            this.status = status;
        }

        public String getStatus() {

            return status;
        }
    }
}
