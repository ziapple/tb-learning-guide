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

import com.ziapple.common.data.id.EntityId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.service.relation.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Slf4j
public abstract class AbstractEntityService {

    @Autowired
    protected RelationService relationService;

    @Value("${database.entities.type:sql}")
    private String databaseType;

    protected boolean sqlDatabaseUsed;

    @PostConstruct
    public void init() {
        sqlDatabaseUsed = "sql".equalsIgnoreCase(databaseType);
    }

    protected void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        relationService.deleteEntityRelations(tenantId, entityId);
    }

    protected Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of ((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of ((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

}
