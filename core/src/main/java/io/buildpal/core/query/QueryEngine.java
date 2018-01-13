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
import io.buildpal.core.query.sort.Sorter;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class QueryEngine {
    private static final Logger logger = LoggerFactory.getLogger(QueryEngine.class);

    public static List<JsonObject> run(QuerySpec querySpec, List<JsonObject> items) {
        if (querySpec == null || items == null || items.isEmpty()) return items;

        List<JsonObject> filteredItems;

        // Filter items based on the query.
        if (StringUtils.isNotBlank(querySpec.getQuery())) {
            filteredItems = new ArrayList<>();

            CharStream stream = CharStreams.fromString(querySpec.getQuery().trim());
            QueryLexer lexer = new QueryLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            QueryParser parser = new QueryParser(tokens);

            ParseTree parseTree = parser.filter();
            QueryEvaluator evaluator = new QueryEvaluator();

            // Loop through the list and evaluate the query.
            for (JsonObject item : items) {
                if (evaluator.setCurrentItem(item).visit(parseTree)) {
                    filteredItems.add(item);
                }
            }

        } else {
            filteredItems = items;
        }

        sort(querySpec, filteredItems);

        return paginate(querySpec, filteredItems);
    }

    private static void sort(QuerySpec querySpec, List<JsonObject> items) {
        if (items == null || items.isEmpty()) return;

        List<Sort> sorts = querySpec.getSorts();

        if (sorts.isEmpty()) return;

        items.sort(new Sorter(sorts, items.get(0)).comparator());
    }

    private static List<JsonObject> paginate(QuerySpec querySpec, List<JsonObject> items) {
        if (!querySpec.shouldPaginate()) return items;

        int begin = querySpec.begin();
        int end = querySpec.end();

        List<JsonObject> pageOfItems = new ArrayList<>();

        if (end > items.size()) end = items.size();

        if (items.size() < begin) return pageOfItems;

        for (int i=begin; i<end; i++) {
            pageOfItems.add(items.get(i));
        }

        return pageOfItems;
    }
}
