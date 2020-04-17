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
package com.ziapple.dao.entity;

import com.ziapple.common.data.EntityType;
import com.ziapple.common.data.kv.BasicTsKvEntry;
import com.ziapple.common.data.kv.TsKvEntry;
import lombok.Data;

import javax.persistence.*;

import static com.ziapple.dao.entity.ModelConstants.ENTITY_TYPE_COLUMN;
import static com.ziapple.dao.entity.ModelConstants.TS_COLUMN;

@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
public final class TsKvLatestEntity extends AbstractTsKvEntity implements ToData<TsKvEntry> {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_COLUMN)
    private EntityType entityType;

    @Column(name = TS_COLUMN)
    private long ts;

    @Override
    public TsKvEntry toData() {
        return new BasicTsKvEntry(ts, getKvEntry());
    }

    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null;
    }
}
