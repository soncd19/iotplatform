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
package org.iotp.infomgt.dao.model.sql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.iotp.infomgt.dao.model.BaseSqlEntity;
import org.iotp.infomgt.dao.model.ModelConstants;
import org.iotp.infomgt.dao.model.SearchTextEntity;
import org.iotp.infomgt.data.common.UUIDConverter;
import org.iotp.infomgt.data.id.TenantId;
import org.iotp.infomgt.data.id.WidgetsBundleId;
import org.iotp.infomgt.data.widget.WidgetsBundle;

import com.datastax.driver.core.utils.UUIDs;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.WIDGETS_BUNDLE_COLUMN_FAMILY_NAME)
public final class WidgetsBundleEntity extends BaseSqlEntity<WidgetsBundle> implements SearchTextEntity<WidgetsBundle> {

  @Transient
  private static final long serialVersionUID = 6897035686422298096L;

  @Column(name = ModelConstants.WIDGETS_BUNDLE_TENANT_ID_PROPERTY)
  private String tenantId;

  @Column(name = ModelConstants.WIDGETS_BUNDLE_ALIAS_PROPERTY)
  private String alias;

  @Column(name = ModelConstants.WIDGETS_BUNDLE_TITLE_PROPERTY)
  private String title;

  @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
  private String searchText;

  public WidgetsBundleEntity() {
    super();
  }

  public WidgetsBundleEntity(WidgetsBundle widgetsBundle) {
    if (widgetsBundle.getId() != null) {
      this.setId(widgetsBundle.getId().getId());
    }
    if (widgetsBundle.getTenantId() != null) {
      this.tenantId = UUIDConverter.fromTimeUUID(widgetsBundle.getTenantId().getId());
    }
    this.alias = widgetsBundle.getAlias();
    this.title = widgetsBundle.getTitle();
  }

  @Override
  public String getSearchTextSource() {
    return title;
  }

  @Override
  public void setSearchText(String searchText) {
    this.searchText = searchText;
  }

  @Override
  public WidgetsBundle toData() {
    WidgetsBundle widgetsBundle = new WidgetsBundle(new WidgetsBundleId(UUIDConverter.fromString(id)));
    widgetsBundle.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
    if (tenantId != null) {
      widgetsBundle.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
    }
    widgetsBundle.setAlias(alias);
    widgetsBundle.setTitle(title);
    return widgetsBundle;
  }
}
