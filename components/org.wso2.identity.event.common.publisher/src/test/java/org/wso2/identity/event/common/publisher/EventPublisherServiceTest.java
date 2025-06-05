/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
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

package org.wso2.identity.event.common.publisher;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.identity.event.common.publisher.internal.EventPublisherDataHolder;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.common.publisher.model.EventPayload;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;
import org.wso2.identity.event.common.publisher.model.common.SimpleSubject;
import org.wso2.identity.event.common.publisher.model.common.Subject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Consolidated test class for EventPublisherService, EventContext, and SecurityEventTokenPayload.
 */
public class EventPublisherServiceTest {

    @Mock
    private EventPublisher mockEventPublisher1;
    @Mock
    private EventPublisher mockEventPublisher2;
    @Mock
    private EventContext mockEventContext;
    @Mock
    private SecurityEventTokenPayload mockEventPayload;

    private List<EventPublisher> eventPublishers;
    private EventPublisherService eventPublisherService;

    @BeforeClass
    public void setupClass() {

        MockitoAnnotations.openMocks(this);
        eventPublisherService = Mockito.spy(new EventPublisherService()); // Spy on the service
    }

    @BeforeMethod
    public void setup() {

        Mockito.reset(mockEventPublisher1, mockEventPublisher2); // Reset mocks
        eventPublishers = Arrays.asList(mockEventPublisher1, mockEventPublisher2);
        EventPublisherDataHolder.getInstance().setEventPublishers(eventPublishers); // Set publishers
    }

    @AfterMethod
    public void tearDown() {

        EventPublisherDataHolder.getInstance().setEventPublishers(null); // Clear singleton state
    }

    @Test
    public void testPublishWithException() throws Exception {

        CountDownLatch latch = new CountDownLatch(eventPublishers.size());

        doAnswer(invocation -> {
            latch.countDown();
            throw new RuntimeException("Test Exception");
        }).when(mockEventPublisher1).publish(mockEventPayload, mockEventContext);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockEventPublisher2).publish(mockEventPayload, mockEventContext);

        eventPublisherService.publish(mockEventPayload, mockEventContext);

        latch.await(1, TimeUnit.SECONDS);

        verify(mockEventPublisher1, times(1)).publish(mockEventPayload, mockEventContext);
        verify(mockEventPublisher2, times(1)).publish(mockEventPayload, mockEventContext);
    }

    @Test
    public void testSuccessfulPublish() throws Exception {

        CountDownLatch latch = new CountDownLatch(eventPublishers.size());

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockEventPublisher1).publish(mockEventPayload, mockEventContext);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockEventPublisher2).publish(mockEventPayload, mockEventContext);

        eventPublisherService.publish(mockEventPayload, mockEventContext);

        latch.await(1, TimeUnit.SECONDS);

        verify(mockEventPublisher1, times(1)).publish(mockEventPayload, mockEventContext);
        verify(mockEventPublisher2, times(1)).publish(mockEventPayload, mockEventContext);
    }

    @Test
    public void testPublishWithNoPublishers() {

        EventPublisherDataHolder.getInstance().setEventPublishers(Collections.emptyList());

        // Call the service method
        eventPublisherService.publish(mockEventPayload, mockEventContext);

        // Verify no interactions occurred with mock publishers
        verifyNoInteractions(mockEventPublisher1, mockEventPublisher2);
    }

    @Test
    public void testEventContextBuilder() {

        EventContext eventContext = EventContext.builder()
                .tenantDomain("example.com")
                .eventUri("http://example.com/event")
                .eventProfileVersion("1.0.0")
                .build();

        Assert.assertEquals(eventContext.getTenantDomain(), "example.com");
        Assert.assertEquals(eventContext.getEventUri(), "http://example.com/event");
        Assert.assertEquals(eventContext.getEventProfileVersion(), "1.0.0");
    }

    @Test
    public void testSecurityEventTokenPayloadBuilder() {

        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put("key1", new EventPayload() {
        });

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .events(eventMap)
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertNotNull(payload.getEvents());
        Assert.assertEquals(payload.getEvents().get("key1"), eventMap.get("key1"));
    }

    @Test
    public void testSecurityEventTokenPayloadWithNullEvent() {

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .events(null)
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertNull(payload.getEvents());
    }

    @Test
    public void testSecurityEventTokenPayloadWithEmptyEvent() {

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .events(new HashMap<>())
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertNotNull(payload.getEvents());
        Assert.assertTrue(payload.getEvents().isEmpty());
    }

    @Test
    public void testEventPublisherServiceWithNullPayload() {

        eventPublisherService.publish(null, mockEventContext);
        verifyNoInteractions(mockEventPublisher1, mockEventPublisher2);
    }

    @Test
    public void testSecurityEventTokenPayloadWithSubId() {

        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put("key1", new EventPayload() {
        });

        Subject subId = SimpleSubject.createOpaqueSubject("subId123");

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .subId(subId)
                .events(eventMap)
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertEquals(payload.getSubId(), subId);
        Assert.assertNotNull(payload.getEvents());
        Assert.assertEquals(payload.getEvents().get("key1"), eventMap.get("key1"));
    }

    @Test
    public void testSecurityEventTokenPayloadWithNullSubId() {

        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put("key1", new EventPayload() {
        });

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .subId(null)
                .events(eventMap)
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertNull(payload.getSubId());
        Assert.assertNotNull(payload.getEvents());
        Assert.assertEquals(payload.getEvents().get("key1"), eventMap.get("key1"));
    }

    @Test
    public void testSecurityEventTokenPayloadWithEmptySubId() {

        Map<String, EventPayload> eventMap = new HashMap<>();
        eventMap.put("key1", new EventPayload() {
        });

        Subject subId = SimpleSubject.createOpaqueSubject("");

        SecurityEventTokenPayload payload = SecurityEventTokenPayload.builder()
                .iss("issuer")
                .jti("jti123")
                .iat(123456789L)
                .aud("audience")
                .txn("transaction")
                .rci("rci123")
                .subId(subId)
                .events(eventMap)
                .build();

        Assert.assertEquals(payload.getIss(), "issuer");
        Assert.assertEquals(payload.getJti(), "jti123");
        Assert.assertEquals(payload.getIat(), 123456789L);
        Assert.assertEquals(payload.getAud(), "audience");
        Assert.assertEquals(payload.getTxn(), "transaction");
        Assert.assertEquals(payload.getRci(), "rci123");
        Assert.assertEquals(payload.getSubId(), subId);
        Assert.assertNotNull(payload.getEvents());
        Assert.assertEquals(payload.getEvents().get("key1"), eventMap.get("key1"));
    }
}
