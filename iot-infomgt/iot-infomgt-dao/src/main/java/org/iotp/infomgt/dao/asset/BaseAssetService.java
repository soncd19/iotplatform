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
package org.iotp.infomgt.dao.asset;

import static org.iotp.infomgt.dao.DaoUtil.toUUIDs;
import static org.iotp.infomgt.dao.model.ModelConstants.NULL_UUID;
import static org.iotp.infomgt.dao.util.Validator.validateId;
import static org.iotp.infomgt.dao.util.Validator.validateIds;
import static org.iotp.infomgt.dao.util.Validator.validatePageLink;
import static org.iotp.infomgt.dao.util.Validator.validateString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.iotp.infomgt.dao.customer.CustomerDao;
import org.iotp.infomgt.dao.entity.AbstractEntityService;
import org.iotp.infomgt.dao.exception.DataValidationException;
import org.iotp.infomgt.dao.relation.EntitySearchDirection;
import org.iotp.infomgt.dao.tenant.TenantDao;
import org.iotp.infomgt.dao.util.DataValidator;
import org.iotp.infomgt.dao.util.PaginatedRemover;
import org.iotp.infomgt.data.Asset;
import org.iotp.infomgt.data.Customer;
import org.iotp.infomgt.data.Tenant;
import org.iotp.infomgt.data.TenantAssetType;
import org.iotp.infomgt.data.common.ThingType;
import org.iotp.infomgt.data.id.AssetId;
import org.iotp.infomgt.data.id.CustomerId;
import org.iotp.infomgt.data.id.EntityId;
import org.iotp.infomgt.data.id.TenantId;
import org.iotp.infomgt.data.page.TextPageData;
import org.iotp.infomgt.data.page.TextPageLink;
import org.iotp.infomgt.data.relation.EntityRelation;
import org.iotp.infomgt.data.security.AssetCredentials;
import org.iotp.infomgt.data.security.AssetCredentialsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BaseAssetService extends AbstractEntityService implements AssetService {

  @Autowired
  private AssetDao assetDao;

  @Autowired
  private TenantDao tenantDao;

  @Autowired
  private CustomerDao customerDao;

  @Autowired
  private AssetCredentialsService assetCredentialsService;

  @Override
  public Asset findAssetById(AssetId assetId) {
    log.trace("Executing findAssetById [{}]", assetId);
    validateId(assetId, "Incorrect assetId " + assetId);
    return assetDao.findById(assetId.getId());
  }

  @Override
  public ListenableFuture<Asset> findAssetByIdAsync(AssetId assetId) {
    log.trace("Executing findAssetById [{}]", assetId);
    validateId(assetId, "Incorrect assetId " + assetId);
    return assetDao.findByIdAsync(assetId.getId());
  }

  @Override
  public Optional<Asset> findAssetByTenantIdAndName(TenantId tenantId, String name) {
    log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    return assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name);
  }

  @Override
  public Asset saveAsset(Asset asset) {
    log.trace("Executing saveAsset [{}]", asset);
    assetValidator.validate(asset);
    Asset savedAsset = assetDao.save(asset);
    if (asset.getId() == null) {
      AssetCredentials assetCredentials = new AssetCredentials();
      assetCredentials.setAssetId(new AssetId(savedAsset.getUuidId()));
      assetCredentials.setCredentialsType(AssetCredentialsType.ACCESS_TOKEN);
      assetCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
      assetCredentialsService.createAssetCredentials(assetCredentials);
    }
    return savedAsset;
  }

  @Override
  public Asset assignAssetToCustomer(AssetId assetId, CustomerId customerId) {
    Asset asset = findAssetById(assetId);
    asset.setCustomerId(customerId);
    return saveAsset(asset);
  }

  @Override
  public Asset unassignAssetFromCustomer(AssetId assetId) {
    Asset asset = findAssetById(assetId);
    asset.setCustomerId(null);
    return saveAsset(asset);
  }

  @Override
  public void deleteAsset(AssetId assetId) {
    log.trace("Executing deleteAsset [{}]", assetId);
    validateId(assetId, "Incorrect assetId " + assetId);
    AssetCredentials assetCredentials = assetCredentialsService.findAssetCredentialsByAssetId(assetId);
    if (assetCredentials != null) {
      assetCredentialsService.deleteAssetCredentials(assetCredentials);
    }
    deleteEntityRelations(assetId);
    assetDao.removeById(assetId.getId());
  }

  @Override
  public TextPageData<Asset> findAssetsByTenantId(TenantId tenantId, TextPageLink pageLink) {
    log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validatePageLink(pageLink, "Incorrect page link " + pageLink);
    List<Asset> assets = assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
    return new TextPageData<>(assets, pageLink);
  }

  @Override
  public TextPageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
    log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type,
        pageLink);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateString(type, "Incorrect type " + type);
    validatePageLink(pageLink, "Incorrect page link " + pageLink);
    List<Asset> assets = assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);
    return new TextPageData<>(assets, pageLink);
  }

  @Override
  public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds) {
    log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateIds(assetIds, "Incorrect assetIds " + assetIds);
    return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
  }

  @Override
  public void deleteAssetsByTenantId(TenantId tenantId) {
    log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    tenantAssetsRemover.removeEntities(tenantId);
  }

  @Override
  public TextPageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId,
      TextPageLink pageLink) {
    log.trace("Executing findAssetsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId,
        customerId, pageLink);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateId(customerId, "Incorrect customerId " + customerId);
    validatePageLink(pageLink, "Incorrect page link " + pageLink);
    List<Asset> assets = assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    return new TextPageData<Asset>(assets, pageLink);
  }

  @Override
  public TextPageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId,
      String type, TextPageLink pageLink) {
    log.trace(
        "Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]",
        tenantId, customerId, type, pageLink);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateId(customerId, "Incorrect customerId " + customerId);
    validateString(type, "Incorrect type " + type);
    validatePageLink(pageLink, "Incorrect page link " + pageLink);
    List<Asset> assets = assetDao.findAssetsByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type,
        pageLink);
    return new TextPageData<>(assets, pageLink);
  }

  @Override
  public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId,
      CustomerId customerId, List<AssetId> assetIds) {
    log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]",
        tenantId, customerId, assetIds);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateId(customerId, "Incorrect customerId " + customerId);
    validateIds(assetIds, "Incorrect assetIds " + assetIds);
    return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(),
        toUUIDs(assetIds));
  }

  @Override
  public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
    log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    validateId(customerId, "Incorrect customerId " + customerId);
    new CustomerAssetsUnassigner(tenantId).removeEntities(customerId);
  }

  @Override
  public ListenableFuture<List<Asset>> findAssetsByQuery(AssetSearchQuery query) {
    ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
    ListenableFuture<List<Asset>> assets = Futures.transform(relations,
        (AsyncFunction<List<EntityRelation>, List<Asset>>) relations1 -> {
          EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
          List<ListenableFuture<Asset>> futures = new ArrayList<>();
          for (EntityRelation relation : relations1) {
            EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
            if (entityId.getEntityType() == ThingType.ASSET) {
              futures.add(findAssetByIdAsync(new AssetId(entityId.getId())));
            }
          }
          return Futures.successfulAsList(futures);
        });

    assets = Futures.transform(assets, new Function<List<Asset>, List<Asset>>() {
      @Nullable
      @Override
      public List<Asset> apply(@Nullable List<Asset> assetList) {
        return assetList.stream().filter(asset -> query.getAssetTypes().contains(asset.getType()))
            .collect(Collectors.toList());
      }
    });

    return assets;
  }

  @Override
  public ListenableFuture<List<TenantAssetType>> findAssetTypesByTenantId(TenantId tenantId) {
    log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
    validateId(tenantId, "Incorrect tenantId " + tenantId);
    ListenableFuture<List<TenantAssetType>> tenantAssetTypeEntities = assetDao.findTenantAssetTypesAsync();
    ListenableFuture<List<TenantAssetType>> tenantAssetTypes = Futures.transform(tenantAssetTypeEntities,
        (Function<List<TenantAssetType>, List<TenantAssetType>>) assetTypeEntities -> {
          List<TenantAssetType> assetTypes = new ArrayList<>();
          for (TenantAssetType assetType : assetTypeEntities) {
            if (assetType.getTenantId().equals(tenantId)) {
              assetTypes.add(assetType);
            }
          }
          assetTypes.sort(Comparator.comparing(TenantAssetType::getType));
          return assetTypes;
        });
    return tenantAssetTypes;
  }

  private DataValidator<Asset> assetValidator = new DataValidator<Asset>() {

    @Override
    protected void validateCreate(Asset asset) {
      assetDao.findAssetsByTenantIdAndName(asset.getTenantId().getId(), asset.getName()).ifPresent(d -> {
        throw new DataValidationException("Asset with such name already exists!");
      });
    }

    @Override
    protected void validateUpdate(Asset asset) {
      assetDao.findAssetsByTenantIdAndName(asset.getTenantId().getId(), asset.getName()).ifPresent(d -> {
        if (!d.getId().equals(asset.getId())) {
          throw new DataValidationException("Asset with such name already exists!");
        }
      });
    }

    @Override
    protected void validateDataImpl(Asset asset) {
      if (StringUtils.isEmpty(asset.getType())) {
        throw new DataValidationException("Asset type should be specified!");
      }
      if (StringUtils.isEmpty(asset.getName())) {
        throw new DataValidationException("Asset name should be specified!");
      }
      if (asset.getTenantId() == null) {
        throw new DataValidationException("Asset should be assigned to tenant!");
      } else {
        Tenant tenant = tenantDao.findById(asset.getTenantId().getId());
        if (tenant == null) {
          throw new DataValidationException("Asset is referencing to non-existent tenant!");
        }
      }
      if (asset.getCustomerId() == null) {
        asset.setCustomerId(new CustomerId(NULL_UUID));
      } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
        Customer customer = customerDao.findById(asset.getCustomerId().getId());
        if (customer == null) {
          throw new DataValidationException("Can't assign asset to non-existent customer!");
        }
        if (!customer.getTenantId().equals(asset.getTenantId())) {
          throw new DataValidationException("Can't assign asset to customer from different tenant!");
        }
      }
    }
  };

  private PaginatedRemover<TenantId, Asset> tenantAssetsRemover = new PaginatedRemover<TenantId, Asset>() {

    @Override
    protected List<Asset> findEntities(TenantId id, TextPageLink pageLink) {
      return assetDao.findAssetsByTenantId(id.getId(), pageLink);
    }

    @Override
    protected void removeEntity(Asset entity) {
      deleteAsset(new AssetId(entity.getId().getId()));
    }
  };

  class CustomerAssetsUnassigner extends PaginatedRemover<CustomerId, Asset> {

    private TenantId tenantId;

    CustomerAssetsUnassigner(TenantId tenantId) {
      this.tenantId = tenantId;
    }

    @Override
    protected List<Asset> findEntities(CustomerId id, TextPageLink pageLink) {
      return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
    }

    @Override
    protected void removeEntity(Asset entity) {
      unassignAssetFromCustomer(new AssetId(entity.getId().getId()));
    }
  }
}
