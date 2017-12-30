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

package io.buildpal.auth.vault;

import io.vertx.core.json.JsonObject;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

import static io.buildpal.core.config.Constants.KEY;
import static io.buildpal.core.config.Constants.PATH;

/**
 * Store and retrieve data to and from the key store.
 */
public class Vault {

    private static final String JCEKS_KEY_STORE = "JCEKS";
    private static final String PBE = "PBE";

    private String keyStoreFilePath;
    private KeyStore keyStore;
    private KeyStore.PasswordProtection passwordProtection;

    private boolean loaded = false;

    /**
     * Initialize a key store and key factory.
     *
     * @param vaultConfig holds configuration information for the vault - path and key.
     */
    public Vault(JsonObject vaultConfig) throws Exception {
        this(vaultConfig.getString(KEY).toCharArray(), vaultConfig.getString(PATH));
    }

    /**
     * Initialize a key store and key factory.
     */
    public Vault(char[] key, String path) throws Exception {

        keyStoreFilePath = Objects.requireNonNull(path,"Vault path is not specified.");

        Objects.requireNonNull(key,"Vault key is not specified.");

        keyStore = KeyStore.getInstance(JCEKS_KEY_STORE);
        passwordProtection = new KeyStore.PasswordProtection(key);
    }

    /**
     * Save the hashed data using the specified key.
     *
     * @param key key that will be used to store the hashed data.
     * @param data the hashed data to be stored in the key store.
     */
    public void save(String key, Data data) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {

        load();

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE);
        SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(data.hash));

        keyStore.setEntry(key, new KeyStore.SecretKeyEntry(secretKey), passwordProtection);
        keyStore.store(new FileOutputStream(keyStoreFilePath), passwordProtection.getPassword());
    }

    /**
     * Save the data using the specified key in the key store.
     *
     * @param key used to save and retrieve data.
     * @param data the data to be stored in the key store.
     */
    public void save(String key, JsonObject data) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeySpecException {

        load();

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE);
        SecretKey secretKey = secretKeyFactory.generateSecret(new PBEKeySpec(data.encode().toCharArray()));

        keyStore.setEntry(key, new KeyStore.SecretKeyEntry(secretKey), passwordProtection);
        keyStore.store(new FileOutputStream(keyStoreFilePath), passwordProtection.getPassword());
    }

    /**
     * Get the data saved in the key store based on the given key.
     *
     * @param key used to retrieve data from the key store.
     * @return the stored data corresponding to the specified key.
     */
    public JsonObject retrieveJson(String key) throws InvalidKeySpecException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {

        load();

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE);
        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(key, passwordProtection);

        PBEKeySpec keySpec = (PBEKeySpec) secretKeyFactory.getKeySpec(secretKeyEntry.getSecretKey(), PBEKeySpec.class);

        return new JsonObject(new String(keySpec.getPassword()));
    }

    /**
     * Get the hashed data using the given key.
     *
     * @param key the key that will be used to retrieve the hashed data.
     * @return the hashed data and its corresponding salt.
     */
    public Data retrieve(String key) throws InvalidKeySpecException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {

        load();

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBE);
        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(key, passwordProtection);

        PBEKeySpec spec = (PBEKeySpec) secretKeyFactory.getKeySpec(entry.getSecretKey(), PBEKeySpec.class);

        return new Data(spec.getPassword());
    }

    private void load() throws CertificateException, NoSuchAlgorithmException, IOException {
        if (loaded) return;

        File keyStoreFile = new File(keyStoreFilePath);

        if (keyStoreFile.exists()) {
            keyStore.load(new FileInputStream(keyStoreFilePath), passwordProtection.getPassword());

        } else if (keyStoreFile.createNewFile()) {
            keyStore.load(null, passwordProtection.getPassword());
        }

        loaded = true;
    }

    public static class Data {
        private char[] hash;

        Data(char[] hash) {
            this.hash = hash;
        }

        public Data(byte[] hash) {
            this.hash = Base64.getEncoder().encodeToString(hash).toCharArray();
        }

        public byte[] getHashBytes() {
            return Base64.getDecoder().decode(new String(hash));
        }
    }
}
