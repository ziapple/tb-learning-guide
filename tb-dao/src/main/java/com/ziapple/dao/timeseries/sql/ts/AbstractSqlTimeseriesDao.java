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
package com.ziapple.dao.timeseries.sql.ts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ziapple.common.data.id.EntityId;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.kv.*;
import com.ziapple.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractSqlTimeseriesDao extends JpaAbstractDaoListeningExecutorService {

    private static final String DESC_ORDER = "DESC";

    protected ListenableFuture<List<TsKvEntry>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<List<List<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected abstract ListenableFuture<List<TsKvEntry>> findAllAsync(TenantId tenantId, EntityId entityId, ReadTsKvQuery query);

    protected ListenableFuture<List<TsKvEntry>> getTskvEntriesFuture(ListenableFuture<List<Optional<TsKvEntry>>> future) {
        return Futures.transform(future, new Function<List<Optional<TsKvEntry>>, List<TsKvEntry>>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<Optional<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected ListenableFuture<List<TsKvEntry>> findNewLatestEntryFuture(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query) {
        long startTs = 0;
        long endTs = query.getStartTs() - 1;
        ReadTsKvQuery findNewLatestQuery = new BaseReadTsKvQuery(query.getKey(), startTs, endTs, endTs - startTs, 1,
                Aggregation.NONE, DESC_ORDER);
        return findAllAsync(tenantId, entityId, findNewLatestQuery);
    }
}
