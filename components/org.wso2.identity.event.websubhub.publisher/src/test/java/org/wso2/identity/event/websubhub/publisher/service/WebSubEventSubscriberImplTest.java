/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.webhook.management.api.exception.WebhookMgtException;
import org.wso2.identity.event.websubhub.publisher.config.WebSubAdapterConfiguration;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubCorrelationLogUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.WEB_SUB_HUB_ADAPTER_NAME;

/**
 * Unit tests for the WebSubEventSubscriberImpl class.
 */
public class WebSubEventSubscriberImplTest {

    private static final Log log = LogFactory.getLog(WebSubEventSubscriberImplTest.class);

    private WebSubEventSubscriberImpl subscriberService;
    private AutoCloseable mocks;

    @Mock
    private ClientManager mockClientManager;

    @Mock
    private WebSubAdapterConfiguration mockAdapterConfiguration;

    @Mock
    private CloseableHttpResponse mockHttpResponse;

    @Mock
    private StatusLine mockStatusLine;

    @Mock
    private HttpEntity mockHttpEntity;

    private MockedStatic<WebSubHubAdapterDataHolder> mockedStaticDataHolder;
    private MockedStatic<WebSubHubAdapterUtil> mockedStaticUtil;
    private MockedStatic<WebSubHubCorrelationLogUtils> mockedCorrelationLogUtils;

    @BeforeMethod
    public void setUp() {

        log.info("Starting test setup for WebSubEventSubscriberImplTest");
        mocks = MockitoAnnotations.openMocks(this);
        subscriberService = new WebSubEventSubscriberImpl();
        setupMocks();
    }

    private void setupMocks() {

        try {
            mockedStaticDataHolder = mockStatic(WebSubHubAdapterDataHolder.class);
            WebSubHubAdapterDataHolder mockDataHolder = mock(WebSubHubAdapterDataHolder.class);

            mockedStaticUtil = mockStatic(WebSubHubAdapterUtil.class);
            mockedStaticUtil.when(WebSubHubAdapterUtil::getWebSubBaseURL)
                    .thenReturn("https://mock-websub-hub.com");
            mockedStaticUtil.when(() -> WebSubHubAdapterUtil.constructHubTopic(anyString(), anyString()))
                    .thenAnswer(invocation -> invocation.getArgument(1) + "-" +
                            invocation.getArgument(0));
            mockedStaticUtil.when(
                            () -> WebSubHubAdapterUtil.buildSubscriptionURL(anyString(), anyString(), anyString(),
                                    anyString()))
                    .thenReturn(
                            "https://mock-websub-hub.com?hub.mode=subscribe&hub.topic=test-tenant-test-" +
                                    "uri&hub.callback=https://test-callback.com");

            mockedCorrelationLogUtils = mockStatic(WebSubHubCorrelationLogUtils.class);
            mockedCorrelationLogUtils.when(() -> WebSubHubCorrelationLogUtils.triggerCorrelationLogForRequest(any()))
                    .thenAnswer(invocation -> null);

            when(mockDataHolder.getClientManager()).thenReturn(mockClientManager);
            when(mockDataHolder.getAdapterConfiguration()).thenReturn(mockAdapterConfiguration);
            when(mockAdapterConfiguration.getWebSubHubBaseUrl()).thenReturn("https://mock-websub-hub.com");

            when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
            when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
        } catch (Exception e) {
            log.error("Error setting up mocks", e);
            cleanupMocks();
            throw e;
        }
    }

    private void cleanupMocks() {

        log.info("Cleaning up mocks");
        try {
            if (mockedStaticDataHolder != null) {
                mockedStaticDataHolder.close();
                mockedStaticDataHolder = null;
            }
            if (mockedStaticUtil != null) {
                mockedStaticUtil.close();
                mockedStaticUtil = null;
            }
            if (mockedCorrelationLogUtils != null) {
                mockedCorrelationLogUtils.close();
                mockedCorrelationLogUtils = null;
            }
        } catch (Exception e) {
            log.error("Error closing mocks", e);
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {

        log.info("Tearing down test for WebSubEventSubscriberImplTest");
        cleanupMocks();
        if (mocks != null) {
            mocks.close();
            mocks = null;
        }
    }

    @Test
    public void testGetName() {

        log.info("Testing getName method");
        // Verify the name matches the expected constant
        assertEquals(subscriberService.getName(), WEB_SUB_HUB_ADAPTER_NAME);
    }

    @Test
    public void testUnsubscribeFailure() throws WebhookMgtException, WebSubAdapterException {

        log.info("Testing unsubscribe failure method");
        List<String> topics = Arrays.asList("test-uri1", "test-uri2");
        String callbackUrl = "http://test-callback.com";
        String tenantDomain = "test-tenant";

        // Simulate a 500 error for the first topic and 200 for the second
        CompletableFuture<HttpResponse> failedFuture = CompletableFuture.completedFuture(mockHttpResponse);
        CompletableFuture<HttpResponse> successFuture = CompletableFuture.completedFuture(mockHttpResponse);

        // Set up the status codes for the responses
        when(mockStatusLine.getStatusCode()).thenReturn(500).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockClientManager.createHttpPost(anyString(), any())).thenReturn(mock(HttpPost.class));
        when(mockClientManager.executeAsync(any())).thenReturn(failedFuture).thenReturn(successFuture);

        boolean result = subscriberService.unsubscribe(topics, callbackUrl, tenantDomain);

        // Verify the result is false because one of the unsubscribe requests failed
        assertFalse(result);
    }

    @Test
    public void testSubscribeFailure() throws WebSubAdapterException {

        log.info("Testing subscribe failure method");
        List<String> topics = Arrays.asList("test-uri1", "test-uri2");
        String callbackUrl = "http://test-callback.com";
        String tenantDomain = "test-tenant";

        // Simulate a 500 error for the first topic and 200 for the second
        CompletableFuture<HttpResponse> failedFuture = CompletableFuture.completedFuture(mockHttpResponse);
        CompletableFuture<HttpResponse> successFuture = CompletableFuture.completedFuture(mockHttpResponse);

        // Set up the status codes for the responses
        when(mockStatusLine.getStatusCode()).thenReturn(500).thenReturn(200);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockClientManager.createHttpPost(anyString(), any())).thenReturn(mock(HttpPost.class));
        when(mockClientManager.executeAsync(any())).thenReturn(failedFuture).thenReturn(successFuture);

        boolean result = subscriberService.subscribe(topics, callbackUrl, tenantDomain);

        // Verify the result is false because one of the subscribe requests failed
        assertFalse(result);
    }
}

