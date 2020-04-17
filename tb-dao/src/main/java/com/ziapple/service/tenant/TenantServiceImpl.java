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
package com.ziapple.service.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.common.data.Tenant;
import com.ziapple.common.data.TextPageData;
import com.ziapple.common.data.id.EntityId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.tenant.TenantDao;
import com.ziapple.dao.exception.DataValidationException;
import com.ziapple.service.*;
import com.ziapple.service.customer.CustomerService;
import com.ziapple.service.device.DeviceService;
import com.ziapple.service.entityview.EntityViewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.ziapple.service.Validator.validateId;

@Service
@Slf4j
public class TenantServiceImpl extends AbstractEntityService implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private TenantDao tenantDao;


    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findById(tenantId, tenantId.getId());
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId) {
        log.trace("Executing TenantIdAsync [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    @Override
    public Tenant saveTenant(Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        tenantValidator.validate(tenant, Tenant::getId);
        return tenantDao.save(tenant.getId(), tenant);
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        tenantDao.removeById(tenantId, tenantId.getId());
        deleteEntityRelations(tenantId, tenantId);
    }

    @Override
    public TextPageData<Tenant> findTenants(TextPageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Tenant> tenants = tenantDao.findTenantsByRegion(new TenantId(EntityId.NULL_UUID), DEFAULT_TENANT_REGION, pageLink);
        return new TextPageData<>(tenants, pageLink);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(new TenantId(EntityId.NULL_UUID),DEFAULT_TENANT_REGION);
    }

    private DataValidator<Tenant> tenantValidator =
            new DataValidator<Tenant>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, Tenant tenant) {
                    if (StringUtils.isEmpty(tenant.getTitle())) {
                        throw new DataValidationException("Tenant title should be specified!");
                    }
                    if (!StringUtils.isEmpty(tenant.getEmail())) {
                        validateEmail(tenant.getEmail());
                    }
                }
    };

    private PaginatedRemover<String, Tenant> tenantsRemover =
            new PaginatedRemover<String, Tenant>() {

        @Override
        protected List<Tenant> findEntities(TenantId tenantId, String region, TextPageLink pageLink) {
            return tenantDao.findTenantsByRegion(tenantId, region, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Tenant entity) {
            deleteTenant(new TenantId(entity.getUuidId()));
        }
    };
}
