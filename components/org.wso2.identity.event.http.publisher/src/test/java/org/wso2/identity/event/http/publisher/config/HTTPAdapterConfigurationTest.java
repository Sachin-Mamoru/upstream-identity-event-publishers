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

package org.wso2.identity.event.http.publisher.config;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.identity.event.http.publisher.api.exception.HTTPAdapterException;
import org.wso2.identity.event.http.publisher.internal.config.HTTPAdapterConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Test class for HTTPAdapterConfiguration.
 */
public class HTTPAdapterConfigurationTest {

    @Test
    public void testDefaultConfiguration() throws HTTPAdapterException {

        Map<String, String> properties = new HashMap<>();
        HTTPAdapterConfiguration config = new HTTPAdapterConfiguration(properties);

        Assert.assertFalse(config.isAdapterEnabled());
        Assert.assertEquals(config.getHTTPConnectionTimeout(), 300);
        Assert.assertEquals(config.getHttpReadTimeout(), 300);
        Assert.assertEquals(config.getHttpConnectionRequestTimeout(), 300);
        Assert.assertEquals(config.getDefaultMaxConnections(), 20);
        Assert.assertEquals(config.getDefaultMaxConnectionsPerRoute(), 2);
    }

    @Test
    public void testCustomConfiguration() throws HTTPAdapterException {

        Map<String, String> properties = new HashMap<>();
        properties.put("enabled", "true");
        properties.put("httpConnectionTimeout", "500");
        properties.put("httpReadTimeout", "600");
        properties.put("httpConnectionRequestTimeout", "700");
        properties.put("defaultMaxConnections", "150");
        properties.put("defaultMaxConnectionsPerRoute", "30");

        HTTPAdapterConfiguration config = new HTTPAdapterConfiguration(properties);

        Assert.assertTrue(config.isAdapterEnabled());
        Assert.assertEquals(config.getHTTPConnectionTimeout(), 500);
        Assert.assertEquals(config.getHttpReadTimeout(), 600);
        Assert.assertEquals(config.getHttpConnectionRequestTimeout(), 700);
        Assert.assertEquals(config.getDefaultMaxConnections(), 150);
        Assert.assertEquals(config.getDefaultMaxConnectionsPerRoute(), 30);
    }

    @Test
    public void testInvalidValuesFallbackToDefault() throws HTTPAdapterException {

        Map<String, String> properties = new HashMap<>();
        properties.put("enabled", "false");
        properties.put("httpConnectionTimeout", "invalid");
        properties.put("httpReadTimeout", null);
        properties.put("httpConnectionRequestTimeout", "");
        properties.put("defaultMaxConnections", "notanumber");
        properties.put("defaultMaxConnectionsPerRoute", "-");

        HTTPAdapterConfiguration config = new HTTPAdapterConfiguration(properties);

        Assert.assertFalse(config.isAdapterEnabled());
        Assert.assertEquals(config.getHTTPConnectionTimeout(), 300);
        Assert.assertEquals(config.getHttpReadTimeout(), 300);
        Assert.assertEquals(config.getHttpConnectionRequestTimeout(), 300);
        Assert.assertEquals(config.getDefaultMaxConnections(), 20);
        Assert.assertEquals(config.getDefaultMaxConnectionsPerRoute(), 2);
    }
}
