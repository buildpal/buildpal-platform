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

package io.buildpal.core.process;

import io.buildpal.core.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds and executes an external process.
 */
public class ExternalProcess {

    private String runFromPath;
    private String commandName;
    private List<String> commandArgs;
    private File workingDirectory;

    public ExternalProcess() {
        commandArgs = new ArrayList<>();
    }

    public String getRunFromPath() {
        return StringUtils.isNotEmpty(runFromPath) ? runFromPath : StringUtils.EMPTY;
    }

    /**
     * Specify the directory in which a given script or command is available.
     * A {@code null} path will be ignored.
     *
     * @param runFromPath the directory path in which a given script or command is to be found.
     * @return {@link ExternalProcess}
     */
    public ExternalProcess runFrom(String runFromPath) {
        if (StringUtils.isNotBlank(runFromPath)) {
            File directory = new File(runFromPath);

            if (!directory.exists() || !directory.isDirectory()) {
                throw new IllegalArgumentException("Directory in which the script or command resides should be valid: "
                        + runFromPath);
            }

            this.runFromPath = FileUtils.slashify(runFromPath);
        }

        return this;
    }

    /**
     * Specify the external command to run.
     * The command name will <b>NOT</b> be validated.
     * The command extension if any (.exe or .sh) should be specified as part of the name.
     *
     * @param commandName the name of the command to execute.
     * @return {@link ExternalProcess}
     */
    public ExternalProcess withCommand(String commandName) {
        if (StringUtils.isBlank(commandName)) {
            throw new IllegalArgumentException("Command name must be specified.");
        }

        this.commandName = commandName;
        return this;
    }

    /**
     * Specify an argument that will be passed to the external command or script.
     *
     * @param arg an argument to the command or script.
     * @return {@link ExternalProcess}
     */
    public ExternalProcess withArg(String arg) {
        if (StringUtils.isBlank(arg)) {
            throw new IllegalArgumentException("Argument cannot be empty.");
        }

        commandArgs.add(arg);
        return this;
    }

    /**
     * Specify the working directory for the script or command.
     * If the value is {@code null}, the working directory of the current
     * java process {@code user.dir} will be used.
     *
     * @param workingDirectory the working directory used by the script or command.
     * @return {@link ExternalProcess}
     */
    public ExternalProcess inDirectory(String workingDirectory) {
        if (StringUtils.isNotBlank(workingDirectory)) {
            File directory = new File(workingDirectory);

            if (!directory.exists() || !directory.isDirectory()) {
                throw new IllegalArgumentException("A valid working directory has to be specified: "
                        + workingDirectory);
            }

            this.workingDirectory = directory;
        }

        return this;
    }

    /**
     * Starts an external process based on the provided configuration.
     *
     * @return the exit code of the executed external process.
     * @throws IOException exception thrown by the process if there are any I/O issues.
     * @throws InterruptedException exception thrown when a waiting process is interrupted by another thread.
     */
    public int run() throws IOException, InterruptedException {
        return run(0, null);
    }

    /**
     * Starts an external process based on the provided configuration and waits for it to complete
     * within the stipulated time.
     *
     * @param timeout the maximum time to wait for the external process.
     * @param unit the time unit of the {@code timeout} argument.
     * @return the exit code of the executed external process.
     * @throws IOException exception thrown by the process if there are any I/O issues.
     * @throws InterruptedException exception thrown when a waiting process is interrupted by another thread.
     */
    public int run(long timeout, TimeUnit unit) throws IOException, InterruptedException {
        Process process = build();
        int result;

        if (timeout > 0) {
            result = process.waitFor(timeout, unit) ? process.exitValue() : -1;
        }
        else {
            result = process.waitFor();
        }

        if (result == -1) {
            process.destroyForcibly();
        }
        else {
            process.destroy();
        }

        return result;
    }

    /**
     * Starts a process based on the provided options.
     *
     * @return {@link Process}
     * @throws IOException exception thrown from starting the process if there's any I/O issue.
     */
    private Process build() throws IOException {
        List<String> command = new ArrayList<>();
        command.add(getRunFromPath() + commandName);
        command.addAll(commandArgs);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(workingDirectory)
                .inheritIO();

        return pb.start();
    }
}