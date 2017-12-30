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

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.buildpal.core.util.VertxUtils.future;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static io.buildpal.core.config.Constants.DOT;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class FileUtils {

    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static final String NEW_LINE = "\n";
    public static final String SLASH = "/";
    public static final String DOT_SLASH = "./%s";
    public static final String COLON = ":";

    private static class Holder<T> {
        public T value;
    }

    public static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private Path firstMatchedFile;

        public Finder(String pattern) {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        public Path getFirstMatchedFile() {
            return firstMatchedFile;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            return find(file) ? TERMINATE : CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex) {
            logger.error(ex);
            return CONTINUE;
        }

        private boolean find(Path file) {
            Path name = file.getFileName();

            if (name != null && matcher.matches(name)) {
                firstMatchedFile = file;
                return true;
            }

            return false;
        }
    }

    public static Path find(File dir, String pattern) throws Exception {
        Finder finder = new Finder(pattern);
        Files.walkFileTree(dir.toPath(), finder);

        return finder.getFirstMatchedFile();
    }

    public static Path tryFind(File dir, String pattern) {
        try {
            return find(dir, pattern);
        } catch (Exception ex) {
            logger.error("Unable to find pattern: " + pattern + " in dir: " + dir, ex);
            return null;
        }
    }

    public static String slashify(String path) {
        if (path.lastIndexOf(SLASH) != path.length() - 1) {
            path += SLASH;
        }

        return path;
    }

    public static String unslashify(String path) {
        if (path.endsWith(SLASH)) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    public static boolean copy(File src, File dest) throws Exception {
        final Holder<Boolean> copied = new Holder<>();

        try (Stream<Path> stream = Files.walk(src.toPath())) {

            Iterator<Path> iterator = stream.iterator();

            while (iterator.hasNext()) {
                Path sourcePath = iterator.next();
                Path destPath = dest.toPath().resolve(src.toPath().relativize(sourcePath));

                try {
                    Files.copy(sourcePath, destPath);
                    copied.value = true;

                } catch (FileAlreadyExistsException ex) {
                    // It's okay. Proceed.
                    copied.value = true;

                } catch (Exception ex) {
                    logger.error("Unable to copy file/folder: " + sourcePath, ex);
                    copied.value = false;
                    break;
                }
            }
        }

        return copied.value;
    }

    public static void chmod757(String path) throws IOException {

        //using PosixFilePermission to set file permissions 757.
        Set<PosixFilePermission> perms = new HashSet<>();

        //add owners permission
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        //add others permissions
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);

        Files.setPosixFilePermissions(Paths.get(path), perms);
    }

    public static boolean chmod(String path, boolean recursive,
                                boolean readable, boolean writable, boolean executable) throws IOException {

        File file = new File(path);

        if (file.isDirectory() && recursive) {

            try (Stream<Path> stream = Files.walk(file.toPath())) {

                Iterator<Path> iterator = stream.iterator();

                while (iterator.hasNext()) {
                    File visitFile = iterator.next().toFile();

                    if (!visitFile.setReadable(readable, false)
                            || !visitFile.setWritable(writable, false)
                            || !visitFile.setExecutable(executable, false)) {
                        logger.error("Unable to set permissions on file: " + visitFile);
                        return false;
                    }
                }
            }

            return true;

        } else {
            return file.setReadable(readable, false)
                    && file.setWritable(writable, false)
                    && file.setExecutable(executable, false);
        }
    }

    public static String baseName(String path) {
        File file = new File(path);
        String baseName = file.getName();

        if (baseName.indexOf(DOT) > 0) {
            baseName = baseName.substring(0, baseName.lastIndexOf(DOT));
        }

        return baseName;
    }

    public static String extension(Path path) {
        String name = path.toFile().getName();
        int dotIndex = name.lastIndexOf(DOT);

        if (dotIndex > 0) {
            return name.substring(dotIndex);
        }

        return EMPTY;
    }

    public static void createIfAbsent(FileSystem fs, String path, Handler<AsyncResult<Void>> handler) {
        Future<Void> future = future(handler);

        Future<Boolean> checkFuture = Future.future();
        checkFuture
                .compose(exists -> createFuture(fs, exists, path))
                .compose(h -> future.complete(), future);

        fs.exists(path, checkFuture.completer());
    }

    public static void deleteIfPresent(FileSystem fs, String path, Handler<AsyncResult<Void>> handler) {
        Future<Void> future = future(handler);

        Future<Boolean> checkFuture = Future.future();
        checkFuture
                .compose(exists -> deleteFuture(fs, exists, path))
                .compose(h -> future.complete(), future);

        fs.exists(path, checkFuture.completer());
    }

    public static void readFiles(FileSystem fs, String path, String filter,
                                 Handler<AsyncResult<Map<String, String>>> handler) {

        Future<Map<String, String>> resultFuture = future(handler);
        Map<String, String> result = new HashMap<>();
        Future<List<String>> readDirFuture = Future.future();

        readDirFuture.compose(files -> {

            final List<Future> readFutures = new ArrayList<>(files.size());

            files.forEach(file -> {

                Future<Buffer> readFuture = Future.future();
                readFutures.add(readFuture);

                fs.readFile(file, readFuture.completer());
            });

            CompositeFuture.join(readFutures).setHandler(h -> {

                if (h.succeeded()) {
                    for (int f=0; f<files.size(); f++) {
                        result.put(baseName(files.get(f)), h.result().resultAt(f).toString());
                    }

                    resultFuture.complete(result);

                } else {
                    resultFuture.fail(h.cause());
                }
            });

        }, resultFuture);

        fs.readDir(path, filter, readDirFuture.completer());
    }

    public static List<String> readResource(String resourcePath) throws IOException {
        InputStream inputStream = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> result = new ArrayList<>();

            for (;;) {
                String line = reader.readLine();

                if (line == null) break;

                result.add(line);
            }

            return result;
        }
    }

    private static Future<Void> createFuture(FileSystem fs, boolean exists, String path) {
        Future<Void> createFuture = Future.future();

        if (exists) {
            createFuture.complete();
        } else {
            fs.mkdirs(path, createFuture.completer());
        }

        return createFuture;
    }

    private static Future<Void> deleteFuture(FileSystem fs, boolean exists, String path) {
        Future<Void> deleteFuture = Future.future();

        if (exists) {
            fs.deleteRecursive(path, true, deleteFuture.completer());
        } else {
            deleteFuture.complete();
        }

        return deleteFuture;
    }
}
