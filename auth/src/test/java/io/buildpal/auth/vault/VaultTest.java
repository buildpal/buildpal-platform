package io.buildpal.auth.vault;

import io.buildpal.auth.util.AuthConfigUtils;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static io.buildpal.auth.util.AuthConfigUtilsTest.KEY;
import static io.buildpal.auth.util.AuthConfigUtilsTest.SALT;
import static io.buildpal.core.domain.Entity.NAME;

public class VaultTest {

    private static final String PATH = "build/vault.jceks";


    @Test
    public void saveAndRetrieveHashTest() throws Exception {

        Vault vault = new Vault(KEY, PATH);
        String key = "test";

        byte[] hash = AuthConfigUtils.hash(key.toCharArray(), SALT);
        vault.save(key, new Vault.Data(hash));

        Vault.Data data = vault.retrieve(key);

        Assert.assertTrue("Hashes of the same string should be equal.",
                Arrays.equals(hash, data.getHashBytes()));
    }

    @Test
    public void saveAndRetrieveTest() throws Exception {

        Vault vault = new Vault(KEY, PATH);
        String name = "testJson";

        JsonObject data = new JsonObject().put(NAME, name).put("someData", "abc");
        vault.save(name, data);

        JsonObject savedData = vault.retrieveJson(name);

        Assert.assertTrue("Saved data and retrieved data should contain the same key and values.",
                savedData.encode().equals(data.encode()));
    }
}
