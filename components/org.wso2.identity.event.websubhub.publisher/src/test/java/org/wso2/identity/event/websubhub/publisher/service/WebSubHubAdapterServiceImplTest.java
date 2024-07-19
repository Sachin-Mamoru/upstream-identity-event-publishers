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

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.websubhub.publisher.config.WebSubAdapterConfiguration;
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the WebSubHubAdapterServiceImpl class.
 */
public class WebSubHubAdapterServiceImplTest {

    private static final String SAMPLE_EVENT_URI = "test/event";
    private static final String SAMPLE_TENANT_DOMAIN = "wso2.com";
    private static final String SAMPLE_WEB_SUB_HUB_BASE_URL = "https://websubhub.wso2.com";

    @Mock
    private WebSubAdapterConfiguration mockAdapterConfiguration;
    @Mock
    private ClientManager mockClientManager;
    @Mock
    private CloseableHttpAsyncClient mockHttpClient;
    @Mock
    private EventContext mockEventContext;
    @Mock
    private SecurityEventTokenPayload mockEventPayload;

    private WebSubHubAdapterServiceImpl webSubHubAdapterService;
    private MockedStatic<LoggerUtils> mockedLoggerUtils;
    private MockedStatic<WebSubHubAdapterUtil> mockedWebSubHubAdapterUtil;

    @BeforeMethod
    public void setUp() throws WebSubAdapterException {
        MockitoAnnotations.openMocks(this);
        webSubHubAdapterService = new WebSubHubAdapterServiceImpl();

        // Set up the singleton instance of WebSubHubAdapterDataHolder
        WebSubHubAdapterDataHolder dataHolder = WebSubHubAdapterDataHolder.getInstance();
        dataHolder.setAdapterConfiguration(mockAdapterConfiguration);
        dataHolder.setClientManager(mockClientManager);

        // Set up the client manager mock
        when(mockClientManager.getClient()).thenReturn(mockHttpClient);

        // Mock static methods
        mockedLoggerUtils = mockStatic(LoggerUtils.class);
        mockedLoggerUtils.when(LoggerUtils::isDiagnosticLogsEnabled).thenReturn(false);

        mockedWebSubHubAdapterUtil = mockStatic(WebSubHubAdapterUtil.class);

        // Common configurations for all tests
        when(mockAdapterConfiguration.isAdapterEnabled()).thenReturn(true);
        when(mockAdapterConfiguration.getWebSubHubBaseUrl()).thenReturn(SAMPLE_WEB_SUB_HUB_BASE_URL);
    }

    @AfterMethod
    public void tearDown() {
        // Close the mocked static methods
        mockedLoggerUtils.close();
        mockedWebSubHubAdapterUtil.close();
    }

    @Test
    public void testPublish() throws WebSubAdapterException {
        // Set up the event context mock
        when(mockEventContext.getEventUri()).thenReturn(SAMPLE_EVENT_URI);
        when(mockEventContext.getTenantDomain()).thenReturn(SAMPLE_TENANT_DOMAIN);

        // Mock the static method for async API call
        mockedWebSubHubAdapterUtil.when(() -> WebSubHubAdapterUtil.makeAsyncAPICall(any(), any(), any(), any()))
                .thenAnswer(invocation -> null);

        // Call the method under test
        webSubHubAdapterService.publish(mockEventPayload, mockEventContext);

        // Verify interactions with the mocks
        verify(mockAdapterConfiguration, times(1)).isAdapterEnabled();
    }

    @Test
    public void testRegisterTopic() throws WebSubAdapterException, IOException {
        // Mock the static method for topic management API call
        mockedWebSubHubAdapterUtil.when(() -> WebSubHubAdapterUtil.makeTopicMgtAPICall(any(), any(), any()))
                .thenAnswer(invocation -> null);

        // Call the method under test
        webSubHubAdapterService.registerTopic(SAMPLE_EVENT_URI, SAMPLE_TENANT_DOMAIN);

        // Verify interactions with the mocks
        verify(mockAdapterConfiguration, times(1)).isAdapterEnabled();
        verify(mockAdapterConfiguration, times(1)).getWebSubHubBaseUrl();
    }

    @Test
    public void testDeregisterTopic() throws WebSubAdapterException, IOException {
        // Mock the static method for topic management API call
        mockedWebSubHubAdapterUtil.when(() -> WebSubHubAdapterUtil.makeTopicMgtAPICall(any(), any(), any()))
                .thenAnswer(invocation -> null);

        // Call the method under test
        webSubHubAdapterService.deregisterTopic(SAMPLE_EVENT_URI, SAMPLE_TENANT_DOMAIN);

        // Verify interactions with the mocks
        verify(mockAdapterConfiguration, times(1)).isAdapterEnabled();
        verify(mockAdapterConfiguration, times(1)).getWebSubHubBaseUrl();
    }
}
