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
package org.iotp.infomgt.dao.model.nosql;

import static org.iotp.infomgt.dao.model.ModelConstants.ID_PROPERTY;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_ENABLED_PROPERTY;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_PASSWORD_PROPERTY;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY;
import static org.iotp.infomgt.dao.model.ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY;

import java.util.UUID;

import org.iotp.infomgt.dao.model.BaseEntity;
import org.iotp.infomgt.data.id.UserCredentialsId;
import org.iotp.infomgt.data.id.UserId;
import org.iotp.infomgt.data.security.UserCredentials;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;

@Table(name = USER_CREDENTIALS_COLUMN_FAMILY_NAME)
public final class UserCredentialsEntity implements BaseEntity<UserCredentials> {

    @Transient
    private static final long serialVersionUID = 1348221414123438374L;
    
    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;
    
    @Column(name = USER_CREDENTIALS_USER_ID_PROPERTY)
    private UUID userId;

    @Column(name = USER_CREDENTIALS_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = USER_CREDENTIALS_PASSWORD_PROPERTY)
    private String password;

    @Column(name = USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY)
    private String activateToken;

    @Column(name = USER_CREDENTIALS_RESET_TOKEN_PROPERTY)
    private String resetToken;

    public UserCredentialsEntity() {
        super();
    }

    public UserCredentialsEntity(UserCredentials userCredentials) {
        if (userCredentials.getId() != null) {
            this.id = userCredentials.getId().getId();
        }
        if (userCredentials.getUserId() != null) {
            this.userId = userCredentials.getUserId().getId();
        }
        this.enabled = userCredentials.isEnabled();
        this.password = userCredentials.getPassword();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
    }
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getActivateToken() {
        return activateToken;
    }

    public void setActivateToken(String activateToken) {
        this.activateToken = activateToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activateToken == null) ? 0 : activateToken.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((resetToken == null) ? 0 : resetToken.hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserCredentialsEntity other = (UserCredentialsEntity) obj;
        if (activateToken == null) {
            if (other.activateToken != null)
                return false;
        } else if (!activateToken.equals(other.activateToken))
            return false;
        if (enabled != other.enabled)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (resetToken == null) {
            if (other.resetToken != null)
                return false;
        } else if (!resetToken.equals(other.resetToken))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
    }

    @Override
    public UserCredentials toData() {
        UserCredentials userCredentials = new UserCredentials(new UserCredentialsId(id));
        userCredentials.setCreatedTime(UUIDs.unixTimestamp(id));
        if (userId != null) {
            userCredentials.setUserId(new UserId(userId));
        }
        userCredentials.setEnabled(enabled);
        userCredentials.setPassword(password);
        userCredentials.setActivateToken(activateToken);
        userCredentials.setResetToken(resetToken);
        return userCredentials;
    }

}
