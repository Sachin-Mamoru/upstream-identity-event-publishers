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
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.identity.event.common.publisher.internal.EventPublisherDataHolder;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for EventPublisherService.
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
    private ExecutorService executorService;

    @BeforeClass
    public void setupClass() {
        MockitoAnnotations.openMocks(this);
        executorService = Executors.newFixedThreadPool(10);
    }

    @BeforeMethod
    public void setup() {
        eventPublishers = Arrays.asList(mockEventPublisher1, mockEventPublisher2);
        EventPublisherDataHolder.getInstance().setEventPublishers(eventPublishers);
    }

    @AfterClass
    public void tearDownClass() {
        executorService.shutdown();
    }

    @Test
    public void testPublish() throws Exception {
        CountDownLatch latch = new CountDownLatch(eventPublishers.size());

        for (EventPublisher eventPublisher : eventPublishers) {
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(eventPublisher).publish(mockEventPayload, mockEventContext);
        }

        for (EventPublisher eventPublisher : eventPublishers) {
            executorService.submit(() -> {
                try {
                    eventPublisher.publish(mockEventPayload, mockEventContext);
                } catch (Exception e) {
                    // Handle the exception appropriately
                }
            });
        }

        latch.await(1, TimeUnit.SECONDS); // Wait for the asynchronous tasks to complete

        for (EventPublisher eventPublisher : eventPublishers) {
            verify(eventPublisher, times(1)).publish(mockEventPayload, mockEventContext);
        }
    }
}
