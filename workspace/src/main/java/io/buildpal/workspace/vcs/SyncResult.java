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

package io.buildpal.workspace.vcs;

public class SyncResult {
    private boolean success;
    private String metadata;
    private Exception exception;

    public SyncResult(boolean success, String metadata) {
        this(success, metadata, null);
    }

    public SyncResult(boolean success, String metadata, Exception exception) {
        this.success = success;
        this.metadata = metadata;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMetadata() {
        return metadata;
    }

    public Exception getException() {
        return exception;
    }

    public static SyncResult success() {
        return new SyncResult(true, null);
    }

    public static SyncResult success(String metadata) {
        return new SyncResult(true, metadata);
    }

    public static SyncResult fail(Exception ex) {
        return new SyncResult(false, null, ex);
    }
}
