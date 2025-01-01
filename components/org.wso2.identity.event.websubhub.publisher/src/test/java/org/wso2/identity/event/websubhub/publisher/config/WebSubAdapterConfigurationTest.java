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

package org.wso2.identity.event.websubhub.publisher.config;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for WebSubAdapterConfiguration.
 */
public class WebSubAdapterConfigurationTest {

    @Test
    public void testDefaultConfiguration() throws WebSubAdapterException {

        OutboundAdapterConfigurationProvider provider = mock(OutboundAdapterConfigurationProvider.class);
        when(provider.getProperty(anyString())).thenReturn(null);

        WebSubAdapterConfiguration config = new WebSubAdapterConfiguration(provider);
        Assert.assertFalse(config.isAdapterEnabled());
        Assert.assertEquals(config.getHTTPConnectionTimeout(), 300);
    }
}
