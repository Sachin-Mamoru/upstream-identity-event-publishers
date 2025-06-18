/*
 * Copyright (c) 2024-2025, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.identity.event.websubhub.publisher.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.IdentityKeyStoreResolver;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverConstants;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverException;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverUtil;
import org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.CORRELATION_ID_REQUEST_HEADER;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.Http.WEBSUBHUB_KEYSTORE_NAME;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;

/**
 * Class to retrieve the HTTP Clients.
 */
public class ClientManager {

    private static final Log LOG = LogFactory.getLog(ClientManager.class);
    private final CloseableHttpAsyncClient httpAsyncClient;
    private final CloseableHttpClient httpClient;
    private final OkHttpClient okHttpClient;

    public ClientManager() throws WebSubAdapterException {

        try {
            PoolingNHttpClientConnectionManager asyncConnectionManager =
                    createPoolingConnectionManager(PoolingNHttpClientConnectionManager.class);
            RequestConfig config = createRequestConfig();

            // Initialize HttpAsyncClient
            HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients.custom()
                    .setDefaultRequestConfig(config)
                    .setConnectionManager(asyncConnectionManager)
                    .setSSLContext(createSSLContext());
            httpAsyncClient = httpAsyncClientBuilder.build();
            httpAsyncClient.start();
            LOG.debug("HttpAsyncClient started with config: connectTimeout=" +
                    config.getConnectTimeout() + ", connectionRequestTimeout=" +
                    config.getConnectionRequestTimeout() + ", socketTimeout=" +
                    config.getSocketTimeout() + ", maxConnections=" +
                    asyncConnectionManager.getMaxTotal() + ", maxConnectionsPerRoute=" +
                    asyncConnectionManager.getDefaultMaxPerRoute());

            // Initialize CloseableHttpClient
            PoolingHttpClientConnectionManager syncConnectionManager =
                    createPoolingConnectionManager(PoolingHttpClientConnectionManager.class);
            httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .setConnectionManager(syncConnectionManager)
                    .setSSLContext(createSSLContext())
                    .build();
            LOG.debug("CloseableHttpClient initialized with config: connectTimeout=" +
                    config.getConnectTimeout() + ", connectionRequestTimeout=" +
                    config.getConnectionRequestTimeout() + ", socketTimeout=" +
                    config.getSocketTimeout() + ", maxConnections=" +
                    syncConnectionManager.getMaxTotal() + ", maxConnectionsPerRoute=" +
                    syncConnectionManager.getDefaultMaxPerRoute());

            okHttpClient = getOkHttpClientForMTLS();
        } catch (IOException e) {
            throw handleServerException(
                    WebSubHubAdapterConstants.ErrorMessages.ERROR_GETTING_ASYNC_CLIENT, e);
        }
    }

    public CloseableHttpAsyncClient getHttpAsyncClient() {

        if (!httpAsyncClient.isRunning()) {
            LOG.debug("HttpAsyncClient is not running, starting client");
            httpAsyncClient.start();
        }
        return httpAsyncClient;
    }

    public CloseableHttpClient getHttpClient() {

        return httpClient;
    }

    public OkHttpClient getOkHttpClient() {

        return okHttpClient;
    }

    private RequestConfig createRequestConfig() {

        return RequestConfig.custom()
                .setConnectTimeout(WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                        .getHTTPConnectionTimeout())
                .setConnectionRequestTimeout(WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                        .getHttpConnectionRequestTimeout())
                .setSocketTimeout(
                        WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getHttpReadTimeout())
                .setRedirectsEnabled(false)
                .setRelativeRedirectsAllowed(false)
                .build();
    }

    private SSLContext createSSLContext() throws WebSubAdapterException {

        try {
            return SSLContexts.custom()
                    .loadTrustMaterial(WebSubHubAdapterDataHolder.getInstance().getTrustStore(), null)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw handleServerException(
                    WebSubHubAdapterConstants.ErrorMessages.ERROR_CREATING_SSL_CONTEXT, e);
        }
    }

    private <T> T createPoolingConnectionManager(Class<T> managerType) throws IOException {

        int maxConnections =
                WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getDefaultMaxConnections();
        int maxConnectionsPerRoute =
                WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getDefaultMaxConnectionsPerRoute();

        if (managerType.equals(PoolingNHttpClientConnectionManager.class)) {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
            PoolingNHttpClientConnectionManager manager = new PoolingNHttpClientConnectionManager(ioReactor);
            manager.setMaxTotal(maxConnections);
            manager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            LOG.debug("PoolingNHttpClientConnectionManager created with maxConnections: " + maxConnections +
                    " and maxConnectionsPerRoute: " + maxConnectionsPerRoute);
            return managerType.cast(manager);
        } else if (managerType.equals(PoolingHttpClientConnectionManager.class)) {
            PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
            manager.setMaxTotal(maxConnections);
            manager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
            LOG.debug("PoolingHttpClientConnectionManager created with maxConnections: " + maxConnections +
                    " and maxConnectionsPerRoute: " + maxConnectionsPerRoute);
            return managerType.cast(manager);
        } else {
            throw new IllegalArgumentException("Unsupported connection manager type: " + managerType.getName());
        }
    }

    /**
     * Create an HTTP POST request.
     *
     * @param url     The URL for the HTTP POST request.
     * @param payload The payload to include in the request body.
     * @return A configured HttpPost instance.
     * @throws WebSubAdapterException If an error occurs while creating the request.
     */
    public HttpPost createHttpPost(String url, Object payload) throws WebSubAdapterException {

        HttpPost request = new HttpPost(url);
        request.setHeader(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        request.setHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.setHeader(CORRELATION_ID_REQUEST_HEADER, WebSubHubAdapterUtil.getCorrelationID());

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        try {
            String jsonString = mapper.writeValueAsString(payload);
            request.setEntity(new StringEntity(jsonString));
        } catch (IOException e) {
            throw WebSubHubAdapterUtil.handleClientException(ERROR_PUBLISHING_EVENT_INVALID_PAYLOAD);
        }

        return request;
    }

    /**
     * Execute an HTTP POST request asynchronously.
     *
     * @param httpPost The HTTP POST request to execute.
     * @return A CompletableFuture containing the HTTP response.
     */
    public CompletableFuture<HttpResponse> executeAsync(HttpPost httpPost) {

        //TODO: Incorporate retry mechanism
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getHttpAsyncClient().execute(httpPost, null).get();
            } catch (InterruptedException ie) {
                // Restore interrupted status
                Thread.currentThread().interrupt();
                throw new IdentityRuntimeException("Thread was interrupted", ie);
            } catch (ExecutionException ee) {
                throw new IdentityRuntimeException("Execution exception", ee);
            } catch (Exception ex) {
                throw new IdentityRuntimeException("Exception occurred", ex);
            }
        });
    }

    /**
     * Execute an HTTP POST request synchronously.
     *
     * @param httpPost The HTTP POST request to execute.
     * @return The HTTP response.
     */
    public HttpResponse execute(HttpPost httpPost) throws IOException {

        return getHttpClient().execute(httpPost);
    }

    public Response makeHTTPPostForMTLS(Request request) throws WebSubAdapterException {

        try {
            try (Response response = getOkHttpClient().newCall(request).execute()) {
                return response;
            }
        } catch (IOException e) {
            throw handleServerException(
                    WebSubHubAdapterConstants.ErrorMessages.ERROR_MAKING_CALL_IO_EXCEPTION, e);
        }
    }

    private OkHttpClient getOkHttpClientForMTLS() throws WebSubAdapterException {

        try {
            long callTimeOut = Long.parseLong("10000");
            long connectTimeOut = WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration()
                    .getHTTPConnectionTimeout();
            long readTimeOut = WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().getHttpReadTimeout();
            long writeTimeOut = Long.parseLong("5000");

            IdentityKeyStoreResolver resolver = IdentityKeyStoreResolver.getInstance();

            // Load custom keystore and default truststore
            KeyStore customKeyStore = resolver.getCustomKeyStore(SUPER_TENANT_DOMAIN_NAME, WEBSUBHUB_KEYSTORE_NAME);
            KeyStore trustStore = resolver.getTrustStore(SUPER_TENANT_DOMAIN_NAME);

            // Initialize KeyManagerFactory with custom keystore
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(customKeyStore, resolver.getCustomKeyStoreConfig(
                    IdentityKeyStoreResolverUtil.buildCustomKeyStoreName(WEBSUBHUB_KEYSTORE_NAME),
                    RegistryResources.SecurityManagement.CustomKeyStore.PROP_KEY_PASSWORD).toCharArray());

            // Initialize TrustManagerFactory with truststore
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // Build SSL context with both
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
                    new java.security.SecureRandom());

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            // Create a ssl socket factory.
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            if (ArrayUtils.isEmpty(trustManagers)) {
                LOG.debug("Trust Managers array is empty");
                throw handleServerException(
                        WebSubHubAdapterConstants.ErrorMessages.ERROR_TRUST_MANAGERS_ARRAY_EMPTY);
            }

            if (!(trustManagers[0] instanceof X509TrustManager)) {
                LOG.debug("Trust Manager is not a instance of X509TrustManager.");
                throw handleServerException(
                        WebSubHubAdapterConstants.ErrorMessages.ERROR_TRUST_MANAGER_NOT_X509);
            }

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0])
                    .callTimeout(callTimeOut, TimeUnit.MILLISECONDS)
                    .connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS)
                    .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
                    .writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
                    .hostnameVerifier((hostname, session) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException |
                 IdentityKeyStoreResolverException | UnrecoverableKeyException e) {
            throw handleServerException(
                    WebSubHubAdapterConstants.ErrorMessages.ERROR_INITIATING_CLIENT, e);
        }
    }
}
