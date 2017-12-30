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

package io.buildpal.auth.util;

import io.buildpal.auth.vault.Vault;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static io.buildpal.core.config.Constants.PATH;

public class AuthConfigUtilsTest {
    public static final char[] KEY = "junit_key".toCharArray();

    public static final byte[] SALT = "junit_salt".getBytes();

    @Test
    public void hashTest() throws Exception {

        Vault vault = new Vault(KEY, PATH);

        byte[] hash1 = AuthConfigUtils.hash("test".toCharArray(), SALT);
        byte[] hash2 = AuthConfigUtils.hash("test".toCharArray(), SALT);
        byte[] hash3 = AuthConfigUtils.hash("tEst".toCharArray(), SALT);

        Assert.assertTrue("Hashes of the same string should be equal.",
                Arrays.equals(hash1, hash2));
        Assert.assertFalse("Hashes of the different strings should not be equal.",
                Arrays.equals(hash2, hash3));
    }
}

