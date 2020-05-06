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
import com.ziapple.common.data.UUIDConverter;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.model.sql.TenantEntity;
import com.ziapple.dao.sql.JpaAbstractSearchTextDao;
import com.ziapple.dao.util.DaoUtil;
import com.ziapple.dao.util.SqlDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.ziapple.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 4/30/2017.
 */
@Component
@SqlDao
public class JpaTenantDao extends JpaAbstractSearchTextDao<TenantEntity, Tenant> implements TenantDao {

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    protected Class<TenantEntity> getEntityClass() {
        return TenantEntity.class;
    }

    @Override
    protected CrudRepository<TenantEntity, String> getCrudRepository() {
        return tenantRepository;
    }

    @Override
    public List<Tenant> findTenantsByRegion(TenantId tenantId, String region, TextPageLink pageLink) {
        return DaoUtil.convertDataList(tenantRepository
                .findByRegionNextPage(
                        region,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }
}
