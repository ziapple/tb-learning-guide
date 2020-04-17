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
package com.ziapple.service;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.common.data.HasName;
import com.ziapple.common.data.id.*;
import com.ziapple.service.customer.CustomerService;
import com.ziapple.service.device.DeviceService;
import com.ziapple.service.entityview.EntityViewService;
import com.ziapple.service.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by ashvayka on 04.05.17.
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {
    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        super.deleteEntityRelations(tenantId, entityId);
    }

    @Override
    public ListenableFuture<String> fetchEntityNameAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityNameAsync [{}]", entityId);
        ListenableFuture<String> entityName;
        ListenableFuture<? extends HasName> hasName;
        switch (entityId.getEntityType()) {
            case DEVICE:
                hasName = deviceService.findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId()));
                break;
            case ENTITY_VIEW:
                hasName = entityViewService.findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId()));
                break;
            case TENANT:
                hasName = tenantService.findTenantByIdAsync(tenantId, new TenantId(entityId.getId()));
                break;
            case CUSTOMER:
                hasName = customerService.findCustomerByIdAsync(tenantId, new CustomerId(entityId.getId()));
                break;
            default:
                throw new IllegalStateException("Not Implemented!");
        }
        entityName = Futures.transform(hasName, (Function<HasName, String>) hasName1 -> hasName1 != null ? hasName1.getName() : null );
        return entityName;
    }

}
