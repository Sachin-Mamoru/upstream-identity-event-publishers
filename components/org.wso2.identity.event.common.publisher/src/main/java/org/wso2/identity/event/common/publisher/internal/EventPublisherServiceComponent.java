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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.wso2.identity.event.common.publisher.EventPublisherService;
import org.wso2.identity.event.common.publisher.EventPublisher;

/**
 * Event Publisher Service Component.
 */
@Component(
        name = "internal.org.wso2.identity.event.common.publisher.EventPublisherServiceComponent",
        immediate = true)
public class EventPublisherServiceComponent {

    private static final Log log = LogFactory.getLog(EventPublisherServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(EventPublisherService.class.getName(),
                    new EventPublisherService(), null);
            if (log.isDebugEnabled()) {
                log.debug("Successfully activated the Event Publisher service.");
            }
        } catch (Throwable e) {
            log.error("Can not activate the Event Publisher service." + e.getMessage(), e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Successfully de-activated the Event Publisher service.");
        }
    }

    @Reference(
            name = "identity.event.publisher",
            service = EventPublisher.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "removeEventPublisher"
    )
    protected void addEventPublisher(EventPublisher eventPublisher) {

        if (log.isDebugEnabled()) {
            log.debug("Adding the event publisher Service : " +
                    eventPublisher.getClass().getName());
        }
        EventPublisherDataHolder.getInstance().addEventPublisher(eventPublisher);
    }

    protected void removeEventPublisher(EventPublisher eventPublisher) {

        if (log.isDebugEnabled()) {
            log.debug("Removing the event publisher Service : " +
                    eventPublisher.getClass().getName());
        }
        EventPublisherDataHolder.getInstance().removeEventPublisher(eventPublisher);
    }
}

