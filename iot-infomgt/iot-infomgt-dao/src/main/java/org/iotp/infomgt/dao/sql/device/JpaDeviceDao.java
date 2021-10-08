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
package org.iotp.infomgt.dao.sql.device;

import static org.iotp.infomgt.dao.model.ModelConstants.NULL_UUID_STR;
import static org.iotp.infomgt.data.common.UUIDConverter.fromTimeUUID;
import static org.iotp.infomgt.data.common.UUIDConverter.fromTimeUUIDs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.iotp.infomgt.dao.DaoUtil;
import org.iotp.infomgt.dao.device.DeviceDao;
import org.iotp.infomgt.dao.model.sql.DeviceEntity;
import org.iotp.infomgt.dao.model.sql.TenantDeviceTypeEntity;
import org.iotp.infomgt.dao.sql.JpaAbstractSearchTextDao;
import org.iotp.infomgt.dao.util.SqlDao;
import org.iotp.infomgt.data.Device;
import org.iotp.infomgt.data.TenantDeviceType;
import org.iotp.infomgt.data.common.UUIDConverter;
import org.iotp.infomgt.data.id.TenantId;
import org.iotp.infomgt.data.page.TextPageLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDeviceDao extends JpaAbstractSearchTextDao<DeviceEntity, Device> implements DeviceDao {

  @Autowired
  private DeviceRepository deviceRepository;

  @Override
  protected Class<DeviceEntity> getEntityClass() {
    return DeviceEntity.class;
  }

  @Override
  protected CrudRepository<DeviceEntity, String> getCrudRepository() {
    return deviceRepository;
  }

  @Override
  public List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
    return DaoUtil.convertDataList(
        deviceRepository.findByTenantId(fromTimeUUID(tenantId), Objects.toString(pageLink.getTextSearch(), ""),
            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
            new PageRequest(0, pageLink.getLimit())));
  }

  @Override
  public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
    return service.submit(() -> DaoUtil.convertDataList(
        deviceRepository.findDevicesByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(deviceIds))));
  }

  @Override
  public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
    return DaoUtil.convertDataList(deviceRepository.findByTenantIdAndCustomerId(fromTimeUUID(tenantId),
        fromTimeUUID(customerId), Objects.toString(pageLink.getTextSearch(), ""),
        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
        new PageRequest(0, pageLink.getLimit())));
  }

  @Override
  public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId,
      List<UUID> deviceIds) {
    return service.submit(
        () -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(fromTimeUUID(tenantId),
            fromTimeUUID(customerId), fromTimeUUIDs(deviceIds))));
  }

  @Override
  public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
    Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
    return Optional.ofNullable(device);
  }

  @Override
  public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
    return DaoUtil.convertDataList(deviceRepository.findByTenantIdAndType(fromTimeUUID(tenantId), type,
        Objects.toString(pageLink.getTextSearch(), ""),
        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
        new PageRequest(0, pageLink.getLimit())));
  }

  @Override
  public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type,
      TextPageLink pageLink) {
    return DaoUtil.convertDataList(deviceRepository.findByTenantIdAndCustomerIdAndType(fromTimeUUID(tenantId),
        fromTimeUUID(customerId), type, Objects.toString(pageLink.getTextSearch(), ""),
        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
        new PageRequest(0, pageLink.getLimit())));
  }

  @Override
  public ListenableFuture<List<TenantDeviceType>> findTenantDeviceTypesAsync() {
    return service.submit(() -> convertTenantDeviceTypeEntityToDto(deviceRepository.findTenantDeviceTypes()));
  }

  private List<TenantDeviceType> convertTenantDeviceTypeEntityToDto(List<TenantDeviceTypeEntity> entities) {
    List<TenantDeviceType> list = Collections.emptyList();
    if (entities != null && !entities.isEmpty()) {
      list = new ArrayList<>();
      for (TenantDeviceTypeEntity entity : entities) {
        list.add(new TenantDeviceType(entity.getType(), new TenantId(UUIDConverter.fromString(entity.getTenantId()))));
      }
    }
    return list;
  }
}
