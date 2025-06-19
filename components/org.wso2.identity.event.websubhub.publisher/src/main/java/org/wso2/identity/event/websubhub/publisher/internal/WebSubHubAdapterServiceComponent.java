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

package org.wso2.identity.event.websubhub.publisher.internal;

import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.topic.management.api.service.TopicManager;
import org.wso2.carbon.identity.webhook.management.api.service.EventSubscriber;
import org.wso2.identity.event.common.publisher.EventPublisher;
import org.wso2.identity.event.websubhub.publisher.config.OutboundAdapterConfigurationProvider;
import org.wso2.identity.event.websubhub.publisher.config.WebSubAdapterConfiguration;
import org.wso2.identity.event.websubhub.publisher.service.WebSubEventPublisherImpl;
import org.wso2.identity.event.websubhub.publisher.service.WebSubEventSubscriberImpl;
import org.wso2.identity.event.websubhub.publisher.service.WebSubTopicManagerImpl;

/**
 * WebSubHub Outbound Event Adapter service component.
 */
@Component(
        name = "org.wso2.identity.event.websubhub.publisher.internal.WebSubHubAdapterServiceComponent",
        immediate = true)
public class WebSubHubAdapterServiceComponent {

    private static final Log log = LogFactory.getLog(WebSubHubAdapterServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            WebSubHubAdapterDataHolder.getInstance().setAdapterConfiguration(new WebSubAdapterConfiguration(
                    OutboundAdapterConfigurationProvider.getInstance()));
            if (WebSubHubAdapterDataHolder.getInstance().getAdapterConfiguration().isAdapterEnabled()) {
                // Register EventPublisher service
                WebSubEventPublisherImpl eventPublisherService = new WebSubEventPublisherImpl();
                context.getBundleContext().registerService(EventPublisher.class.getName(),
                        eventPublisherService, null);

                // Register WebhookSubscriber service
                WebSubEventSubscriberImpl subscriberService = new WebSubEventSubscriberImpl();
                context.getBundleContext().registerService(EventSubscriber.class.getName(),
                        subscriberService, null);

                // Register WebhookTopicManager service
                WebSubTopicManagerImpl topicManagerService = new WebSubTopicManagerImpl();
                context.getBundleContext().registerService(TopicManager.class.getName(),
                        topicManagerService, null);
                WebSubHubAdapterDataHolder.getInstance().setClientManager(new ClientManager());
                WebSubHubAdapterDataHolder.getInstance().setResourceRetriever(new DefaultResourceRetriever());
                log.debug("Successfully activated the WebSubHub adapter service.");
            } else {
                log.error("WebSubHub Adapter is not enabled.");
            }
        } catch (Throwable e) {
            log.error("Can not activate the WebSubHub adapter service: " + e.getMessage(), e);
        }
    }

    @Reference(
            name = "identity.core.init.event.service",
            service = IdentityCoreInitializedEvent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityCoreInitializedEventService"
    )
    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        log.debug("Successfully de-activated the WebSubHub adapter service.");
    }

    @Reference(name = "identity.organization.management.component",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager")
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        WebSubHubAdapterDataHolder.getInstance().setOrganizationManager(organizationManager);
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        WebSubHubAdapterDataHolder.getInstance().setOrganizationManager(null);
    }
}
