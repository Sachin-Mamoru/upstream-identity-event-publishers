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

package org.wso2.identity.event.http.publisher.util;

import org.apache.http.client.methods.HttpPost;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.testng.annotations.Test;
import org.wso2.identity.event.http.publisher.internal.util.HTTPCorrelationLogUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Test class for HTTPCorrelationLogUtils.
 */
public class HTTPCorrelationLogUtilsTest {

    @Test
    public void testHandleResponseCorrelationLog() {

        HttpPost mockRequest = mock(HttpPost.class);
        when(mockRequest.getFirstHeader(anyString())).thenReturn(
                new org.apache.http.message.BasicHeader("X-Correlation-ID", "test-correlation-id"));

        try (MockedStatic<MDC> mockedMDC = Mockito.mockStatic(MDC.class)) {
            HTTPCorrelationLogUtils.handleResponseCorrelationLog(mockRequest, System.currentTimeMillis(), "completed",
                    "200", "OK");
            mockedMDC.verify(() -> MDC.put(anyString(), anyString()), times(1));
            mockedMDC.verify(() -> MDC.remove(anyString()), times(1));
        }
    }

    @Test
    public void testTriggerCorrelationLogForResponse() {

        HttpPost mockRequest = mock(HttpPost.class);
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getURI()).thenReturn(java.net.URI.create("http://localhost/test?param=value"));

        // Enable correlation logs for this test
        System.setProperty("enableCorrelationLogs", "true");

        HTTPCorrelationLogUtils.triggerCorrelationLogForResponse(
                mockRequest, System.currentTimeMillis(), "completed", "200", "OK");

        // No assertion needed, just ensure no exceptions and code path is covered
    }
}
