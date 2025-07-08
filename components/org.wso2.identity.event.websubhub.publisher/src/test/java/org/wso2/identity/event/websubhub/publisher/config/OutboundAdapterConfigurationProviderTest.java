package org.wso2.identity.event.websubhub.publisher.config;

import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Test class for OutboundAdapterConfigurationProvider.
 */
public class OutboundAdapterConfigurationProviderTest {

    @Test
    public void testLoadPropertiesFromMockFile() throws Exception {

        try (MockedStatic<IdentityUtil> mockedIdentityUtil = mockStatic(IdentityUtil.class)) {

            String testResourcesPath = Paths.get("src", "test", "resources").toAbsolutePath().toString();

            mockedIdentityUtil.when(IdentityUtil::getIdentityConfigDirPath)
                    .thenReturn(testResourcesPath);

            OutboundAdapterConfigurationProvider provider = OutboundAdapterConfigurationProvider.getInstance();

            String testProperty = provider.getProperty("adapter.websubhub.enabled");
            Assert.assertNotNull(testProperty, "Property should not be null");
            Assert.assertEquals(testProperty, "false", "Property value should match the mock file content");
        }
    }

    @Test
    public void testLoadPropertiesException() throws Exception {

        Constructor<OutboundAdapterConfigurationProvider> constructor =
                OutboundAdapterConfigurationProvider.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class);
             MockedStatic<IdentityUtil> mockedIdentityUtil = mockStatic(IdentityUtil.class)) {

            mockedIdentityUtil.when(IdentityUtil::getIdentityConfigDirPath)
                    .thenReturn("/mock/config/dir");

            mockedFiles.when(() -> Files.notExists(any(Path.class))).thenReturn(true);

            try {
                constructor.newInstance();
                fail("AdapterConfigurationException was expected but not thrown.");
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                assertTrue(cause instanceof EventPublisherException,
                        "Cause should be AdapterConfigurationException");
                assertTrue(cause.getMessage().contains("configuration file doesn't exist"),
                        "Exception message should indicate file not found");
            }
        }
    }

    @Test
    public void testLoadPropertiesSuccess() throws Exception {

        Constructor<OutboundAdapterConfigurationProvider> constructor =
                OutboundAdapterConfigurationProvider.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try (MockedStatic<IdentityUtil> mockedIdentityUtil = mockStatic(IdentityUtil.class)) {
            String resourceDir = getClass().getClassLoader().getResource("").getPath();
            mockedIdentityUtil.when(IdentityUtil::getIdentityConfigDirPath).thenReturn(resourceDir);

            OutboundAdapterConfigurationProvider provider = constructor.newInstance();
            String propertyValue1 = provider.getProperty("adapter.websubhub.enabled");
            String propertyValue2 = provider.getProperty("adapter.websubhub.baseUrl");
            String propertyValue3 = provider.getProperty("adapter.websubhub.httpConnectionTimeout");

            assertNotNull(propertyValue1, "Property value should not be null");
            assertEquals(propertyValue1, "false", "Property value should match the expected value");
            assertNotNull(propertyValue2, "Property value should not be null");
            assertEquals(propertyValue2, "http://localhost:9090", "Property value should match the expected value");
            assertNotNull(propertyValue3, "Property value should not be null");
            assertEquals(propertyValue3, "300", "Property value should match the expected value");
        }
    }
}
