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
package com.ziapple.dao.timeseries.sql.repository;

import com.ziapple.common.data.EntityType;
import com.ziapple.dao.model.sql.ts.TsKvLatestCompositeKey;
import com.ziapple.dao.model.sql.ts.TsKvLatestEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TsKvLatestRepository extends CrudRepository<TsKvLatestEntity, TsKvLatestCompositeKey> {

    List<TsKvLatestEntity> findAllByEntityTypeAndEntityId(EntityType entityType, String entityId);
}
