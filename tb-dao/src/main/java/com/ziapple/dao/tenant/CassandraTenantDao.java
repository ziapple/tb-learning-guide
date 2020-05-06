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
package com.ziapple.dao.tenant;

import com.ziapple.common.data.Tenant;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.model.nosql.TenantEntity;
import com.ziapple.dao.nosql.CassandraAbstractSearchTextDao;
import com.ziapple.dao.util.DaoUtil;
import com.ziapple.dao.util.filter.NoSqlDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.ziapple.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraTenantDao extends CassandraAbstractSearchTextDao<TenantEntity, Tenant> implements TenantDao {

    @Override
    protected Class<TenantEntity> getColumnFamilyClass() {
        return TenantEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return TENANT_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<Tenant> findTenantsByRegion(TenantId tenantId, String region, TextPageLink pageLink) {
        log.debug("Try to find tenants by region [{}] and pageLink [{}]", region, pageLink);
        List<TenantEntity> tenantEntities = findPageWithTextSearch(tenantId, TENANT_BY_REGION_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(TENANT_REGION_PROPERTY, region)), 
                pageLink); 
        log.trace("Found tenants [{}] by region [{}] and pageLink [{}]", tenantEntities, region, pageLink);
        return DaoUtil.convertDataList(tenantEntities);
    }

}
