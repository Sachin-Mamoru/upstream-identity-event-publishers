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

package org.wso2.identity.event.websubhub.publisher.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.TransactionException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.identity.event.websubhub.publisher.constant.TopicSQLConstants;
import org.wso2.identity.event.websubhub.publisher.dao.TopicDAO;
import org.wso2.identity.event.websubhub.publisher.exception.WebSubAdapterException;
import org.wso2.identity.event.websubhub.publisher.model.Topic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_TOPIC_PERSISTENCE;
import static org.wso2.identity.event.websubhub.publisher.constant.WebSubHubAdapterConstants.ErrorMessages.ERROR_TOPIC_RETRIEVAL;
import static org.wso2.identity.event.websubhub.publisher.util.WebSubHubAdapterUtil.handleServerException;

/**
 * Implementation of the TopicDAO interface.
 * This class handles all the database operations related to WebSubHub topic management.
 */
public class TopicDAOImpl implements TopicDAO {

    private static final Log log = LogFactory.getLog(TopicDAOImpl.class);

    @Override
    public String addTopic(Topic topic) throws WebSubAdapterException {

        String id = UUID.randomUUID().toString();
        topic.setId(id);

        NamedJdbcTemplate jdbcTemplate = new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
        try {
            jdbcTemplate.withTransaction(template -> {
                template.executeInsert(TopicSQLConstants.Query.INSERT_TOPIC,
                        statement -> {
                            statement.setString(TopicSQLConstants.Column.ID, id);
                            statement.setString(TopicSQLConstants.Column.TOPIC_URI, topic.getTopicUri());
                            statement.setString(TopicSQLConstants.Column.VERSION, topic.getVersion());
                            statement.setString(TopicSQLConstants.Column.TENANT_DOMAIN, topic.getTenantDomain());
                        }, topic, true);
                return null;
            });

            log.debug("Successfully added topic: " + topic.getTopicUri() + " for tenant: "
                    + topic.getTenantDomain());

            return id;
        } catch (TransactionException e) {
            throw handleServerException(ERROR_TOPIC_PERSISTENCE, e, topic.getTopicUri(), topic.getTenantDomain());
        }
    }

    @Override
    public Topic getTopic(String topicUri, String tenantDomain) throws WebSubAdapterException {

        NamedJdbcTemplate jdbcTemplate = new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
        try {
            return jdbcTemplate.withTransaction(template ->
                    template.fetchSingleRecord(TopicSQLConstants.Query.GET_TOPIC,
                            (resultSet, rowNumber) -> mapResultSetToTopic(resultSet),
                            statement -> {
                                statement.setString(TopicSQLConstants.Column.TOPIC_URI, topicUri);
                                statement.setString(TopicSQLConstants.Column.TENANT_DOMAIN, tenantDomain);
                            })
            );
        } catch (TransactionException e) {
            throw handleServerException(ERROR_TOPIC_RETRIEVAL, e, topicUri, tenantDomain);
        }
    }

    @Override
    public boolean isTopicExists(String topicUri, String tenantDomain) throws WebSubAdapterException {

        Topic topic = getTopic(topicUri, tenantDomain);
        return topic != null;
    }

    @Override
    public void deleteTopic(String topicUri, String tenantDomain) throws WebSubAdapterException {

        NamedJdbcTemplate jdbcTemplate = new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
        try {
            jdbcTemplate.withTransaction(template -> {
                template.executeUpdate(TopicSQLConstants.Query.DELETE_TOPIC,
                        statement -> {
                            statement.setString(TopicSQLConstants.Column.TOPIC_URI, topicUri);
                            statement.setString(TopicSQLConstants.Column.TENANT_DOMAIN, tenantDomain);
                        });
                return null;
            });

            if (log.isDebugEnabled()) {
                log.debug("Successfully deleted topic: " + topicUri + " for tenant: " + tenantDomain);
            }
        } catch (TransactionException e) {
            throw handleServerException(ERROR_TOPIC_PERSISTENCE, e, topicUri, tenantDomain);
        }
    }

    @Override
    public List<Topic> getAllTopics(String tenantDomain) throws WebSubAdapterException {

        NamedJdbcTemplate jdbcTemplate = new NamedJdbcTemplate(IdentityDatabaseUtil.getDataSource());
        try {
            return jdbcTemplate.withTransaction(template ->
                    template.executeQuery(TopicSQLConstants.Query.GET_ALL_TOPICS,
                            (resultSet, rowNumber) -> mapResultSetToTopic(resultSet),
                            statement -> statement.setString(TopicSQLConstants.Column.TENANT_DOMAIN, tenantDomain))
            );
        } catch (TransactionException e) {
            throw handleServerException(ERROR_TOPIC_RETRIEVAL, e, "all topics", tenantDomain);
        }
    }

    // --- Private helper methods ---

    /**
     * Maps a database result set to a Topic object.
     *
     * @param resultSet The result set from the database query
     * @return A Topic object populated with data from the result set
     * @throws SQLException If an error occurs while accessing the result set
     */
    private Topic mapResultSetToTopic(ResultSet resultSet) throws SQLException {

        Topic topic = new Topic();
        topic.setId(resultSet.getString(TopicSQLConstants.Column.ID));
        topic.setTopicUri(resultSet.getString(TopicSQLConstants.Column.TOPIC_URI));
        topic.setVersion(resultSet.getString(TopicSQLConstants.Column.VERSION));
        topic.setTenantDomain(resultSet.getString(TopicSQLConstants.Column.TENANT_DOMAIN));
        return topic;
    }
}
