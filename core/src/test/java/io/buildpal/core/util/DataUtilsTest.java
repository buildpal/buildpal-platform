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
import org.junit.Assert;
import org.junit.Test;

public class DataUtilsTest {

    @Test
    public void evalTest() {
        DataItem dataItem = new DataItem()
                .setID("SHELVED_LIST")
                .setType(DataItem.Type.STRING)
                .setValue("100");

        JsonObject data = new JsonObject().put(dataItem.getID(), dataItem.json());

        String shelvedList = DataUtils.eval("${data.SHELVED_LIST}", data);

        Assert.assertEquals("Data evaluation for ${data.SHELVED_LIST} should be set to 100",
                "100", shelvedList);
    }
}
