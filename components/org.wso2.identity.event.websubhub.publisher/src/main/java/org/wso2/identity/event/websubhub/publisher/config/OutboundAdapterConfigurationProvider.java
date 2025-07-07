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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.publisher.api.exception.EventPublisherException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import static java.util.Objects.isNull;
import static org.wso2.carbon.identity.event.publisher.api.constant.ErrorMessage.CONFIG_FILE_NOT_FOUND;
import static org.wso2.carbon.identity.event.publisher.api.constant.ErrorMessage.CONFIG_FILE_PERMISSION_DENIED;
import static org.wso2.carbon.identity.event.publisher.api.constant.ErrorMessage.CONFIG_FILE_RETRIEVAL_ERROR;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Config.CONFIG_FILE_NAME;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;

/**
 * Class to build the output adapter configurations.
 */
public class OutboundAdapterConfigurationProvider {

    private final Properties adapterProperties;
    private static OutboundAdapterConfigurationProvider instance;

    private OutboundAdapterConfigurationProvider() throws EventPublisherException {

        adapterProperties = this.loadProperties();
    }

    public static OutboundAdapterConfigurationProvider getInstance() throws EventPublisherException {

        if (instance == null) {
            instance = new OutboundAdapterConfigurationProvider();
        }
        return instance;
    }

    @SuppressWarnings("PATH_TRAVERSAL_IN")
    private Properties loadProperties() throws EventPublisherException {

        Properties properties = new Properties();

        Path path = Paths.get(IdentityUtil.getIdentityConfigDirPath(), CONFIG_FILE_NAME);

        if (Files.notExists(path)) {
            throw handleServerException(CONFIG_FILE_NOT_FOUND, CONFIG_FILE_NAME);
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(channel)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw handleServerException(CONFIG_FILE_RETRIEVAL_ERROR, e, CONFIG_FILE_NAME);
        } catch (SecurityException e) {
            throw handleServerException(CONFIG_FILE_PERMISSION_DENIED, e, CONFIG_FILE_NAME);
        }

        return properties;
    }

    /**
     * Returns the value of the property provided.
     *
     * @param propertyName name of the config property.
     * @return value of the property provided or null if not found or blank.
     */
    public String getProperty(String propertyName) {

        if (isNull(propertyName)) {
            return null;
        }
        String propertyValue = this.adapterProperties.getProperty(propertyName);
        return propertyValue != null && !propertyValue.trim().isEmpty() ? propertyValue : null;
    }
}
