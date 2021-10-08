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
package org.iotp.infomgt.dao.user;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.UUID;

import org.iotp.infomgt.dao.DaoUtil;
import org.iotp.infomgt.dao.model.ModelConstants;
import org.iotp.infomgt.dao.model.nosql.UserCredentialsEntity;
import org.iotp.infomgt.dao.nosql.CassandraAbstractModelDao;
import org.iotp.infomgt.dao.util.NoSqlDao;
import org.iotp.infomgt.data.security.UserCredentials;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.querybuilder.Select.Where;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@NoSqlDao
public class CassandraUserCredentialsDao extends CassandraAbstractModelDao<UserCredentialsEntity, UserCredentials> implements UserCredentialsDao {

    @Override
    protected Class<UserCredentialsEntity> getColumnFamilyClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public UserCredentials findByUserId(UUID userId) {
        log.debug("Try to find user credentials by userId [{}] ", userId);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_USER_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, userId));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by userId [{}]", userCredentialsEntity, userId);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByActivateToken(String activateToken) {
        log.debug("Try to find user credentials by activateToken [{}] ", activateToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_ACTIVATE_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, activateToken));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by activateToken [{}]", userCredentialsEntity, activateToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByResetToken(String resetToken) {
        log.debug("Try to find user credentials by resetToken [{}] ", resetToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_RESET_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, resetToken));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by resetToken [{}]", userCredentialsEntity, resetToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

}
