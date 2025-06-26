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
import org.wso2.identity.event.websubhub.publisher.internal.ClientManager;
import org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterDataHolder;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebSubEventSubscriberImplTest {

    private WebSubEventSubscriberImpl subscriberService;
    private AutoCloseable mocks;

    @Mock
    private ClientManager mockClientManager;

    @Mock
    private CloseableHttpResponse mockHttpResponse;

    private MockedStatic<WebSubHubAdapterDataHolder> mockedStaticDataHolder;
    private MockedStatic<WebSubHubAdapterUtil> mockedStaticUtil;

    @BeforeMethod
    public void setUp() {

        mocks = MockitoAnnotations.openMocks(this);
        subscriberService = new WebSubEventSubscriberImpl();

        mockedStaticDataHolder = mockStatic(WebSubHubAdapterDataHolder.class);
        WebSubHubAdapterDataHolder mockDataHolder = mock(WebSubHubAdapterDataHolder.class);
        when(mockDataHolder.getClientManager()).thenReturn(mockClientManager);
        mockedStaticDataHolder.when(WebSubHubAdapterDataHolder::getInstance).thenReturn(mockDataHolder);

        mockedStaticUtil = mockStatic(WebSubHubAdapterUtil.class);
        mockedStaticUtil.when(WebSubHubAdapterUtil::getWebSubBaseURL).thenReturn("https://mock-websub-hub.com");
        mockedStaticUtil.when(() -> WebSubHubAdapterUtil.constructHubTopic(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1) + "-" +
                        invocation.getArgument(0));
    }

    @AfterMethod
    public void tearDown() throws Exception {

        if (mocks != null) {
            mocks.close();
        }
        if (mockedStaticDataHolder != null) {
            mockedStaticDataHolder.close();
        }
        if (mockedStaticUtil != null) {
            mockedStaticUtil.close();
        }
    }

    @Test
    public void testSubscribeSuccess() throws Exception {

        List<String> channels = Arrays.asList("topic1", "topic2");
        String eventProfileVersion = "v1";
        String callbackUrl = "http://test-callback.com";
        String secret = "secret";
        String tenantDomain = "test-tenant";

        HttpPost mockHttpPost = mock(HttpPost.class);
        when(mockClientManager.createHttpPost(anyString(), any())).thenReturn(mockHttpPost);
        when(mockClientManager.executeSubscriberRequest(any())).thenReturn(mockHttpResponse);
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(202); // SC_ACCEPTED

        subscriberService.subscribe(channels, eventProfileVersion, callbackUrl, secret, tenantDomain);

        verify(mockClientManager, times(2)).executeSubscriberRequest(any());
    }

    @Test
    public void testUnsubscribeSuccess() throws Exception {

        List<String> channels = Arrays.asList("topic1", "topic2");
        String eventProfileVersion = "v1";
        String callbackUrl = "http://test-callback.com";
        String tenantDomain = "test-tenant";

        HttpPost mockHttpPost = mock(HttpPost.class);
        when(mockClientManager.createHttpPost(anyString(), any())).thenReturn(mockHttpPost);
        when(mockClientManager.executeSubscriberRequest(any())).thenReturn(mockHttpResponse);
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(202); // SC_ACCEPTED

        subscriberService.unsubscribe(channels, eventProfileVersion, callbackUrl, tenantDomain);

        verify(mockClientManager, times(2)).executeSubscriberRequest(any());
    }

    @Test(expectedExceptions = WebhookMgtException.class)
    public void testSubscribeThrowsException() throws Exception {

        List<String> channels = Arrays.asList("topic1");
        String eventProfileVersion = "v1";
        String callbackUrl = "http://test-callback.com";
        String secret = "secret";
        String tenantDomain = "test-tenant";

        when(mockClientManager.createHttpPost(anyString(), any()))
                .thenThrow(
                        new org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException("fail", null));

        subscriberService.subscribe(channels, eventProfileVersion, callbackUrl, secret, tenantDomain);
    }

    @Test(expectedExceptions = WebhookMgtException.class)
    public void testUnsubscribeThrowsException() throws Exception {

        List<String> channels = Arrays.asList("topic1");
        String eventProfileVersion = "v1";
        String callbackUrl = "http://test-callback.com";
        String tenantDomain = "test-tenant";

        when(mockClientManager.createHttpPost(anyString(), any()))
                .thenThrow(
                        new org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException("fail", null));

        subscriberService.unsubscribe(channels, eventProfileVersion, callbackUrl, tenantDomain);
    }
}
