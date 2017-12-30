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

package io.buildpal.core.util;

import io.buildpal.core.domain.DataItem;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DataUtils {

    public static String eval(String statement, JsonObject data) {
        for (String key : data.getMap().keySet()) {
            DataItem dataItem = new DataItem(data.getJsonObject(key));

            statement = eval(statement, dataItem);
        }

        return statement;
    }

    public static String eval(String statement, DataItem dataItem) {
        return StringUtils.replace(statement, dataItem.key(), dataItem.value());
    }

    public static List<Integer> parseIntList(String value) {
        List<Integer> integers = new ArrayList<>();

        if (StringUtils.isNotBlank(value)) {
            for (String splitVal : value.split(",")) {
                splitVal = splitVal.trim();

                if (StringUtils.isNotBlank(splitVal)) {
                    integers.add(Integer.parseInt(splitVal));
                }
            }
        }

        return integers;
    }
}
