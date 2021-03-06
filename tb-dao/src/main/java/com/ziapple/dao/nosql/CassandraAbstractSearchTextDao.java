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
package com.ziapple.dao.nosql;

import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Select.Where;
import com.ziapple.common.data.id.TenantId;
import com.ziapple.common.data.page.TextPageLink;
import com.ziapple.dao.model.ModelConstants;
import com.ziapple.dao.model.sql.SearchTextEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Slf4j
public abstract class CassandraAbstractSearchTextDao<E extends SearchTextEntity<D>, D> extends CassandraAbstractModelDao<E, D> {

    @Override
    protected E updateSearchTextIfPresent(E entity) {
        if (entity.getSearchTextSource() != null) {
            entity.setSearchText(entity.getSearchTextSource().toLowerCase());
        } else {
            log.trace("Entity [{}] has null SearchTextSource", entity);
        }
        return entity;
    }

    protected List<E> findPageWithTextSearch(TenantId tenantId, String searchView, List<Clause> clauses, TextPageLink pageLink) {
        Select select = select().from(searchView);
        Where query = select.where();
        for (Clause clause : clauses) {
            query.and(clause);
        }
        query.limit(pageLink.getLimit());
        if (!StringUtils.isEmpty(pageLink.getTextOffset())) {
            query.and(eq(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextOffset()));
            query.and(QueryBuilder.lt(ModelConstants.ID_PROPERTY, pageLink.getIdOffset()));
            List<E> result = findListByStatement(tenantId, query);
            if (result.size() < pageLink.getLimit()) {
                select = select().from(searchView);
                query = select.where();
                for (Clause clause : clauses) {
                    query.and(clause);
                }
                query.and(QueryBuilder.gt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextOffset()));
                if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                    query.and(QueryBuilder.lt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearchBound()));
                }
                int limit = pageLink.getLimit() - result.size();
                query.limit(limit);
                result.addAll(findListByStatement(tenantId, query));
            }
            return result;
        } else if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
            query.and(QueryBuilder.gte(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearch()));
            query.and(QueryBuilder.lt(ModelConstants.SEARCH_TEXT_PROPERTY, pageLink.getTextSearchBound()));
            return findListByStatement(tenantId, query);
        } else {
            return findListByStatement(tenantId, query);
        }
    }


}
