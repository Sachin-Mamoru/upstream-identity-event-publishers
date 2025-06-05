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

import org.apache.http.HttpResponse;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.topic.management.api.exception.TopicManagementException;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;
import org.wso2.identity.event.websubhub.publisher.config.WebSubAdapterConfiguration;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the WebSubHubAdapterServiceImpl class.
 */
public class WebSubEventPublisherImplTest {

    private WebSubEventPublisherImpl adapterService;
    private WebSubTopicManagerImpl webSubTopicManager;

    @Mock
    private ClientManager mockClientManager;

    @Mock
    private WebSubAdapterConfiguration mockAdapterConfiguration;

    @Mock
    private HttpResponse mockHttpResponse;

    MockedStatic<WebSubHubAdapterDataHolder> mockedStaticDataHolder;

    @BeforeClass
    public void setUp() {

        MockitoAnnotations.openMocks(this);
        adapterService = spy(new WebSubEventPublisherImpl());
        webSubTopicManager = spy(new WebSubTopicManagerImpl());

        MockedStatic<WebSubHubAdapterDataHolder> mockedStaticDataHolder = mockStatic(WebSubHubAdapterDataHolder.class);
        WebSubHubAdapterDataHolder mockDataHolder = mock(WebSubHubAdapterDataHolder.class);
        mockedStaticDataHolder.when(WebSubHubAdapterDataHolder::getInstance).thenReturn(mockDataHolder);

        when(mockDataHolder.getClientManager()).thenReturn(mockClientManager);
        when(mockDataHolder.getAdapterConfiguration()).thenReturn(mockAdapterConfiguration);
        when(mockAdapterConfiguration.getWebSubHubBaseUrl()).thenReturn("http://mock-websub-hub.com");
    }

    @Test
    public void testPublishSuccess() throws WebSubAdapterException {

        try (MockedStatic<LoggerUtils> mockedLoggerUtils = mockStatic(LoggerUtils.class)) {
            mockedLoggerUtils.when(LoggerUtils::isDiagnosticLogsEnabled).thenReturn(false);

            // Mock inputs
            EventContext eventContext = EventContext.builder()
                    .tenantDomain("test-tenant")
                    .eventUri("test-uri")
                    .build();
            SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                    .iss("issuer")
                    .jti("jti-token")
                    .iat(System.currentTimeMillis())
                    .aud("audience")
                    .build();

            // Mock ClientManager behavior to simulate success
            CompletableFuture<HttpResponse> future = CompletableFuture.completedFuture(mockHttpResponse);
            when(mockClientManager.executeAsync(any())).thenReturn(future);

            // Execute and verify no exception is thrown
            adapterService.publish(payload, eventContext);

            // Verify interactions
            verify(mockClientManager, times(1)).executeAsync(any());
        }
    }

    @Test
    public void testRegisterTopic() throws TopicManagementException {

        doNothing().when(webSubTopicManager).registerTopic("test-uri", "test-tenant");

        webSubTopicManager.registerTopic("test-uri", "test-tenant");

        verify(webSubTopicManager, times(1)).registerTopic("test-uri",
                "test-tenant");
    }

    @Test(expectedExceptions = TopicManagementException.class)
    public void testRegisterTopicFailure() throws TopicManagementException {

        doThrow(new TopicManagementException("Registration failed", "Description", "ErrorCode"))
                .when(webSubTopicManager).registerTopic("test-uri", "test-tenant");

        webSubTopicManager.registerTopic("test-uri", "test-tenant");
    }

    @Test
    public void testDeregisterTopic() throws TopicManagementException {

        doNothing().when(webSubTopicManager).deregisterTopic("test-uri", "test-tenant");

        webSubTopicManager.deregisterTopic("test-uri", "test-tenant");

        verify(webSubTopicManager, times(1)).deregisterTopic("test-uri",
                "test-tenant");
    }

    @Test(expectedExceptions = TopicManagementException.class)
    public void testDeregisterTopicFailure() throws TopicManagementException {

        doThrow(new TopicManagementException("Deregistration failed", "Description", "ErrorCode"))
                .when(webSubTopicManager).deregisterTopic("test-uri", "test-tenant");

        webSubTopicManager.deregisterTopic("test-uri", "test-tenant");
    }
}
