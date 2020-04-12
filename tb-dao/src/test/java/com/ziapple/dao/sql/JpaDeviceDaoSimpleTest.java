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
package com.ziapple.dao.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.ziapple.common.data.Device;
import com.ziapple.common.data.id.CustomerId;
import com.ziapple.common.data.id.DeviceId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.dao.AbstractJpaDaoTest;
import com.ziapple.dao.repository.DeviceDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDeviceDaoSimpleTest extends AbstractJpaDaoTest {

    @Autowired
    private DeviceDao deviceDao;

    @Test
    public void testByTenantId() {
        UUID tenantId1 = UUIDs.timeBased();
        UUID customerId1 = UUIDs.timeBased();
        UUID tenantId2 = UUIDs.timeBased();
        UUID customerId2 = UUIDs.timeBased();

        List<UUID> deviceIds = new ArrayList<>();

        for(int i = 0; i < 20; i++) {
            UUID deviceId1 = UUIDs.timeBased();
            UUID deviceId2 = UUIDs.timeBased();
            deviceDao.save(new TenantId(tenantId1), getDevice(tenantId1, customerId1, deviceId1));
            deviceDao.save(new TenantId(tenantId2), getDevice(tenantId2, customerId2, deviceId2));
            deviceIds.add(deviceId1);
            deviceIds.add(deviceId2);
        }
        List<Device> list = deviceDao.find(new TenantId(tenantId1));
        System.out.println(list);
    }

    private Device getDevice(UUID tenantId, UUID customerID) {
        return getDevice(tenantId, customerID, UUIDs.timeBased());
    }

    private Device getDevice(UUID tenantId, UUID customerID, UUID deviceId) {
        Device device = new Device();
        device.setId(new DeviceId(deviceId));
        device.setTenantId(new TenantId(tenantId));
        device.setCustomerId(new CustomerId(customerID));
        device.setName("SEARCH_TEXT");
        return device;
    }
}
