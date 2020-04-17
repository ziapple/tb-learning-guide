/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package com.ziapple.service.device;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.common.data.*;
import com.ziapple.common.data.id.CustomerId;
import com.ziapple.common.data.id.DeviceId;
import com.ziapple.common.data.id.EntityId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.common.data.relation.EntityRelation;
import com.ziapple.common.data.relation.EntitySearchDirection;
import com.ziapple.common.data.security.DeviceCredentials;
import com.ziapple.common.data.security.DeviceCredentialsType;
import com.ziapple.dao.customer.CustomerDao;
import com.ziapple.dao.device.DeviceDao;
import com.ziapple.dao.tenant.TenantDao;
import com.ziapple.dao.exception.DataValidationException;
import com.ziapple.service.*;
import com.ziapple.service.entityview.EntityViewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.ziapple.common.data.CacheConstants.DEVICE_CACHE;
import static com.ziapple.dao.entity.ModelConstants.NULL_UUID;
import static com.ziapple.dao.util.DaoUtil.toUUIDs;
import static com.ziapple.service.Validator.*;

@Service
@Slf4j
public class DeviceServiceImpl extends AbstractEntityService implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Device findDeviceById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findById(tenantId, deviceId.getId());
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findByIdAsync(tenantId, deviceId.getId());
    }

    @Cacheable(cacheNames = DEVICE_CACHE, key = "{#tenantId, #name}")
    @Override
    public Device findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<Device> deviceOpt = deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name);
        return deviceOpt.orElse(null);
    }

    @CacheEvict(cacheNames = DEVICE_CACHE, key = "{#device.tenantId, #device.name}")
    @Override
    public Device saveDeviceWithAccessToken(Device device, String accessToken) {
        return doSaveDevice(device, accessToken);
    }

    @CacheEvict(cacheNames = DEVICE_CACHE, key = "{#device.tenantId, #device.name}")
    @Override
    public Device saveDevice(Device device) {
        return doSaveDevice(device, null);
    }

    private Device doSaveDevice(Device device, String accessToken) {
        log.trace("Executing saveDevice [{}]", device);
        deviceValidator.validate(device, Device::getTenantId);
        Device savedDevice;
        if (!sqlDatabaseUsed) {
            savedDevice = deviceDao.save(device.getTenantId(), device);
        } else {
            try {
                savedDevice = deviceDao.save(device.getTenantId(), device);
            } catch (Exception t) {
                ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
                if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("device_name_unq_key")) {
                    throw new DataValidationException("Device with such name already exists!");
                } else {
                    throw t;
                }
            }
        }
        if (device.getId() == null) {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(!StringUtils.isEmpty(accessToken) ? accessToken : RandomStringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(device.getTenantId(), deviceCredentials);
        }
        return savedDevice;
    }

    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Override
    public Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Override
    public void deleteDevice(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing deleteDevice [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        Device device = deviceDao.findById(tenantId, deviceId.getId());
        try {
            List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(device.getTenantId(), deviceId).get();
            if (entityViews != null && !entityViews.isEmpty()) {
                throw new DataValidationException("Can't delete device that is assigned to entity views!");
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while finding entity views for deviceId [{}]", deviceId, e);
            throw new RuntimeException("Exception while finding entity views for deviceId [" + deviceId + "]", e);
        }

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(tenantId, deviceCredentials);
        }
        deleteEntityRelations(tenantId, deviceId);

        List<Object> list = new ArrayList<>();
        list.add(device.getTenantId());
        list.add(device.getName());
        Cache cache = cacheManager.getCache(DEVICE_CACHE);
        cache.evict(list);

        deviceDao.removeById(tenantId, deviceId.getId());
    }

    @Override
    public TextPageData<Device> findDevicesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }


    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDevicesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerDeviceUnasigner.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Device>> devices = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Device>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    futures.add(findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });

        devices = Futures.transform(devices, new Function<List<Device>, List<Device>>() {
            @Nullable
            @Override
            public List<Device> apply(@Nullable List<Device> deviceList) {
                return deviceList == null ? Collections.emptyList() : deviceList.stream().filter(device -> query.getDeviceTypes().contains(device.getType())).collect(Collectors.toList());
            }
        });

        return devices;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantDeviceTypes = deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
        return Futures.transform(tenantDeviceTypes,
                deviceTypes -> {
                    deviceTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return deviceTypes;
                });
    }

    private DataValidator<Device> deviceValidator =
            new DataValidator<Device>() {

                @Override
                protected void validateCreate(TenantId tenantId, Device device) {
                    if (!sqlDatabaseUsed) {
                        deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                                d -> {
                                    throw new DataValidationException("Device with such name already exists!");
                                }
                        );
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Device device) {
                    if (!sqlDatabaseUsed) {
                        deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                                d -> {
                                    if (!d.getUuidId().equals(device.getUuidId())) {
                                        throw new DataValidationException("Device with such name already exists!");
                                    }
                                }
                        );
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Device device) {
                    if (StringUtils.isEmpty(device.getType())) {
                        throw new DataValidationException("Device type should be specified!");
                    }
                    if (StringUtils.isEmpty(device.getName())) {
                        throw new DataValidationException("Device name should be specified!");
                    }
                    if (device.getTenantId() == null) {
                        throw new DataValidationException("Device should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(device.getTenantId(), device.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Device is referencing to non-existent tenant!");
                        }
                    }
                    if (device.getCustomerId() == null) {
                        device.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(device.getTenantId(), device.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign device to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(device.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign device to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Device> tenantDevicesRemover =
        new PaginatedRemover<TenantId, Device>() {

            @Override
            protected List<Device> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
            }

            @Override
            protected void removeEntity(TenantId tenantId, Device entity) {
                deleteDevice(tenantId, new DeviceId(entity.getUuidId()));
            }
        };

    private PaginatedRemover<CustomerId, Device> customerDeviceUnasigner = new PaginatedRemover<CustomerId, Device>() {

        @Override
        protected List<Device> findEntities(TenantId tenantId, CustomerId id, TextPageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Device entity) {
            unassignDeviceFromCustomer(tenantId, new DeviceId(entity.getUuidId()));
        }
    };
}
