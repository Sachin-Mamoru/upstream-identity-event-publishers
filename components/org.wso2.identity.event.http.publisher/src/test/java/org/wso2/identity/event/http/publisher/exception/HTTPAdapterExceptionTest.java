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

package org.wso2.identity.event.http.publisher.exception;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.identity.event.http.publisher.api.exception.HTTPAdapterException;

/**
 * Exception wrapper class for HTTP Adapter exceptions tests.
 */
public class HTTPAdapterExceptionTest {

    @Test
    public void testConstructorWithMessageAndErrorCode() {

        String message = "Test message";
        String errorCode = "ERR001";
        HTTPAdapterException exception = new HTTPAdapterException(message, errorCode);

        Assert.assertEquals(exception.getMessage(), message);
        Assert.assertEquals(exception.getErrorCode(), errorCode);
        Assert.assertNull(exception.getDescription());
    }

    @Test
    public void testConstructorWithMessageDescriptionAndErrorCode() {

        String message = "Test message";
        String description = "Test description";
        String errorCode = "ERR002";
        HTTPAdapterException exception = new HTTPAdapterException(message, description, errorCode);

        Assert.assertEquals(exception.getMessage(), message);
        Assert.assertEquals(exception.getErrorCode(), errorCode);
        Assert.assertEquals(exception.getDescription(), description);
    }

    @Test
    public void testConstructorWithMessageDescriptionErrorCodeAndCause() {

        String message = "Test message";
        String description = "Test description";
        String errorCode = "ERR003";
        Throwable cause = new RuntimeException("Cause");
        HTTPAdapterException exception = new HTTPAdapterException(message, description, errorCode, cause);

        Assert.assertEquals(exception.getMessage(), message);
        Assert.assertEquals(exception.getErrorCode(), errorCode);
        Assert.assertEquals(exception.getDescription(), description);
        Assert.assertEquals(exception.getCause(), cause);
    }
}
