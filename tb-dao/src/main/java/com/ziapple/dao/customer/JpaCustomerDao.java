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
package com.ziapple.dao.customer;

import com.ziapple.common.data.Customer;
import com.ziapple.common.data.UUIDConverter;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.JpaAbstractSearchTextDao;
import com.ziapple.dao.entity.CustomerEntity;
import com.ziapple.dao.util.DaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.ziapple.dao.entity.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
public class JpaCustomerDao extends JpaAbstractSearchTextDao<CustomerEntity, Customer> implements CustomerDao {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    protected Class<CustomerEntity> getEntityClass() {
        return CustomerEntity.class;
    }

    @Override
    protected CrudRepository<CustomerEntity, String> getCrudRepository() {
        return customerRepository;
    }

    @Override
    public List<Customer> findCustomersByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(customerRepository.findByTenantId(
                UUIDConverter.fromTimeUUID(tenantId),
                Objects.toString(pageLink.getTextSearch(), ""),
                pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public Optional<Customer> findCustomersByTenantIdAndTitle(UUID tenantId, String title) {
        Customer customer = DaoUtil.getData(customerRepository.findByTenantIdAndTitle(UUIDConverter.fromTimeUUID(tenantId), title));
        return Optional.ofNullable(customer);
    }
}
