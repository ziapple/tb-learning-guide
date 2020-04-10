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
package com.ziapple.dao.repository;

import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.common.data.Device;
import com.ziapple.common.data.UUIDConverter;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.JpaAbstractSearchTextDao;
import com.ziapple.dao.device.DeviceDao;
import com.ziapple.dao.model.DeviceEntity;
import com.ziapple.dao.util.DaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.ziapple.common.data.UUIDConverter.fromTimeUUID;
import static com.ziapple.common.data.UUIDConverter.fromTimeUUIDs;
import static com.ziapple.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
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
        if (StringUtils.isEmpty(pageLink.getTextSearch())) {
            return DaoUtil.convertDataList(
                    deviceRepository.findByTenantId(
                            fromTimeUUID(tenantId),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        } else {
            return DaoUtil.convertDataList(
                    deviceRepository.findByTenantId(
                            fromTimeUUID(tenantId),
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        }
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(deviceIds))));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(fromTimeUUID(tenantId), fromTimeUUID(customerId), fromTimeUUIDs(deviceIds))));
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(device);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }
}
