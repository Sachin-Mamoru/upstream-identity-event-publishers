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

package org.wso2.identity.event.common.publisher.internal;

import org.wso2.identity.event.common.publisher.EventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * Event Publisher Data Holder.
 */
public class EventPublisherDataHolder {

    private static final EventPublisherDataHolder instance = new EventPublisherDataHolder();
    private List<EventPublisher> eventPublishers = new ArrayList<>();

    private EventPublisherDataHolder() {

    }

    public static EventPublisherDataHolder getInstance() {

        return instance;
    }

    /**
     * Get the list of event publishers.
     *
     * @return List of event publishers.
     */
    public List<EventPublisher> getEventPublishers() {

        return eventPublishers;
    }

    /**
     * Add event publisher implementation.
     *
     * @param eventPublisher Event publisher implementation.
     */
    public void addEventPublisher(EventPublisher eventPublisher) {

        eventPublishers.add(eventPublisher);
    }

    /**
     * Remove event publisher implementation.
     *
     * @param eventPublisher Event publisher implementation.
     */
    public void removeEventPublisher(EventPublisher eventPublisher) {

        eventPublishers.remove(eventPublisher);
    }

    /**
     * Set a list of event publishers.
     *
     * @param eventPublishers List of event publishers.
     */
    public void setEventPublishers(List<EventPublisher> eventPublishers) {

        this.eventPublishers = eventPublishers;
    }
}
