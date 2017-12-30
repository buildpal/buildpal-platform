/*
 * Copyright 2017 Buildpal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.buildpal.core.query;

import io.buildpal.core.query.sort.Sort;
import io.vertx.core.MultiMap;
import io.vertx.core.http.CaseInsensitiveHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuerySpec {

    private static final List<Sort> NO_SORTS = Collections.emptyList();

    private static final String QUERY = "q";
    private static final String PAGE = "page";
    private static final String LIMIT = "limit";
    private static final String SORT = "sort";

    private MultiMap multiMap;

    private Integer begin;
    private Integer end;

    public QuerySpec() {
        this(new CaseInsensitiveHeaders());
    }
    
    public QuerySpec(MultiMap multiMap) {
        this.multiMap = multiMap;
    }

    public int getPage() {
        return multiMap.contains(PAGE) ? getInteger(multiMap.get(PAGE)) : -1;
    }

    public QuerySpec setPage(int page) {
        multiMap.set(PAGE, String.valueOf(page));
        begin = end = null;

        return this;
    }

    public int getLimit() {
        return multiMap.contains(LIMIT) ? getInteger(multiMap.get(LIMIT)) : -1;
    }

    public QuerySpec setLimit(int limit) {
        multiMap.set(LIMIT, String.valueOf(limit));
        begin = end = null;

        return this;
    }

    public String getQuery() {
        return multiMap.get(QUERY);
    }

    public QuerySpec setQuery(String query) {
        multiMap.set(QUERY, query);

        return this;
    }

    public List<Sort> getSorts() {
        if (multiMap.contains(SORT)) {

            List<Sort> sorts = new ArrayList<>();
            List<String> rawSorts = multiMap.getAll(SORT);

            rawSorts.forEach(s -> {
                Sort sort = Sort.tryParse(s);

                if (sort != null) sorts.add(sort);
            });

            return sorts;

        } else {
            return NO_SORTS;
        }
    }

    boolean shouldPaginate() {
        return getPage() > 0 && getLimit() > 0;
    }

    int begin() {
        return begin == null ? (getPage() - 1) * getLimit() : begin;
    }

    int end() {
        return end == null ? begin() + getLimit() : end;
    }

    private static int getInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;

        } else {
            try {
                return Integer.parseInt((String) value);

            } catch (Exception ex) {
                return -1;
            }
        }
    }
}
