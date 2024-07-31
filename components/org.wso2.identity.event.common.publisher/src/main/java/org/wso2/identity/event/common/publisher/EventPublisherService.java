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

package org.wso2.identity.event.common.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.identity.event.common.publisher.internal.EventPublisherDataHolder;
import org.wso2.identity.event.common.publisher.model.EventContext;
import org.wso2.identity.event.common.publisher.model.SecurityEventTokenPayload;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event Publisher Service.
 */
public class EventPublisherService {

    private static final Log log = LogFactory.getLog(EventPublisherService.class);
    private static final int THREAD_POOL_SIZE = 10; // Customize the thread pool size based on your needs
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


    /**
     * Publish the event to the event publishers.
     *
     * @param eventPayload  Security Event Token Payload.
     * @param eventContext  Event Context.
     */
    public void publish(SecurityEventTokenPayload eventPayload, EventContext eventContext) {

        List<EventPublisher> eventPublishers =
                EventPublisherDataHolder.getInstance().getEventPublishers();

        for (EventPublisher eventPublisher : eventPublishers) {
            log.debug("Invoking registered event publisher: " + eventPublisher.getClass().getName());
            CompletableFuture.runAsync(() -> {
                try {
                    eventPublisher.publish(eventPayload, eventContext);
                } catch (Exception e) {
                    log.error("Error while publishing event with publisher: " +
                            eventPublisher.getClass().getName(), e);
                }
            }, executorService).exceptionally(ex -> {
                log.error("Error occurred in async event publishing: " + ex.getMessage(), ex);
                return null;
            });
        }
    }
}
