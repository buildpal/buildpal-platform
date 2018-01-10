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

package io.buildpal.oci;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.nio.ByteBuffer;

public class DockerStreamerSansHeader {
    private static final Logger logger = LoggerFactory.getLogger(DockerStreamerSansHeader.class);

    @FunctionalInterface
    public interface StreamHandler {
        void onData(Buffer data);
    }

    private StreamHandler streamHandler;
    private Buffer mainBuffer;
    private int msgSize;

    public DockerStreamerSansHeader(StreamHandler streamHandler) {
        this.streamHandler = streamHandler;

        mainBuffer = Buffer.buffer();
        msgSize = 0;
    }

    public void write(Buffer buffer) {
        mainBuffer.appendBuffer(buffer);
        scan();
    }

    public void end() {
        scan();

        if (mainBuffer.length() > 0) {
            logger.warn("Docker stream overflow. Closing the stream anyway.");
        }

        mainBuffer = null;
        streamHandler = null;
    }

    private void scan() {
        if (msgSize > 0) {
            writeData();

        } else {
            stripHeader();
        }
    }

    private void writeData() {
        if (mainBuffer.length() >= msgSize) {
            streamHandler.onData(mainBuffer.getBuffer(0, msgSize));

            mainBuffer = mainBuffer.slice(msgSize, mainBuffer.length()).copy();
            msgSize = 0;
        }

        if (mainBuffer.length() > 0) {
            stripHeader();
        }
    }

    private void stripHeader() {
        if (msgSize == 0 && mainBuffer.length() >= 8) {
            // Docker adds a header to differentiate b/w stdout and stderr streams when tty is disabled
            // Let's remove that header here.
            byte[] header = mainBuffer.getBytes(0, 8);

            msgSize = ByteBuffer.wrap(header, 4, 4).getInt();
            mainBuffer = mainBuffer.slice(8, mainBuffer.length()).copy();

            if (mainBuffer.length() > 0) {
                scan();
            }
        }
    }
}

