/*******************************************************************************
 * Copyright 2017 osswangxining@163.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iotp.infomgt.dao.rule;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.iotp.infomgt.dao.model.ModelConstants.NULL_UUID;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.iotp.infomgt.dao.DaoUtil;
import org.iotp.infomgt.dao.model.ModelConstants;
import org.iotp.infomgt.dao.model.nosql.RuleMetaDataEntity;
import org.iotp.infomgt.dao.nosql.CassandraAbstractSearchTextDao;
import org.iotp.infomgt.dao.util.NoSqlDao;
import org.iotp.infomgt.data.id.RuleId;
import org.iotp.infomgt.data.id.TenantId;
import org.iotp.infomgt.data.page.TextPageLink;
import org.iotp.infomgt.data.rule.RuleMetaData;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.querybuilder.Select;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@NoSqlDao
public class CassandraBaseRuleDao extends CassandraAbstractSearchTextDao<RuleMetaDataEntity, RuleMetaData> implements RuleDao {

    @Override
    protected Class<RuleMetaDataEntity> getColumnFamilyClass() {
        return RuleMetaDataEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.RULE_COLUMN_FAMILY_NAME;
    }

    @Override
    public RuleMetaData findById(RuleId ruleId) {
        return findById(ruleId.getId());
    }

    @Override
    public List<RuleMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.RULE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.RULE_TENANT_ID_PROPERTY, tenantId.getId())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<RuleMetaData> findAllTenantRulesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant rules by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<RuleMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.RULE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(ModelConstants.RULE_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<RuleMetaData> findRulesByPlugin(String pluginToken) {
        log.debug("Search rules by api token [{}]", pluginToken);
        Select select = select().from(ModelConstants.RULE_BY_PLUGIN_TOKEN);
        Select.Where query = select.where();
        query.and(eq(ModelConstants.RULE_PLUGIN_TOKEN_PROPERTY, pluginToken));
        return DaoUtil.convertDataList(findListByStatement(query));
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete rule meta-data entity by id [{}]", id);
        boolean result = removeById(id);
        log.debug("Delete result: [{}]", result);
    }

    @Override
    public void deleteById(RuleId ruleId) {
        deleteById(ruleId.getId());
    }
}
