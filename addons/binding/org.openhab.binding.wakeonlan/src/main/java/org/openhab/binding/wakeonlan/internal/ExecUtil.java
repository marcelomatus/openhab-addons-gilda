/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * @author Ganesh Ingle <ganesh.ingle@asvilabs.com>
 */

package org.openhab.binding.wakeonlan.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some common methods to execute commands on command line.
 *
 * This class is adapted from ExecUtil class in smarthome core / execute binding
 *
 * @author Pauli Anttila - Initial contribution
 * @author Kai Kreuzer - added exception logging
 * @author Ganesh Ingle - return both exit code and stdout in a map
 */
public class ExecUtil {

    public static final String DELIMITER = "@@";

    /**
     * <p>
     * Executes <code>commandLine</code>. Sometimes (especially observed on MacOS) the commandLine isn't executed
     * properly. In that cases another exec-method is to be used. To accomplish this please use the special delimiter '
     * <code>@@</code>'. If <code>commandLine</code> contains this delimiter it is split into a String[] array and the
     * special exec-method is used.
     * </p>
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     * </p>
     *
     * @param commandLine
     *                        the command line to execute
     * @see http://www.peterfriese.de/running-applescript-from-java/
     */
    public static void executeCommandLine(String commandLine) {
        Logger logger = LoggerFactory.getLogger(ExecUtil.class);
        try {
            if (commandLine.contains(DELIMITER)) {
                String[] cmdArray = commandLine.split(DELIMITER);
                Runtime.getRuntime().exec(cmdArray);
                logger.info("executed commandLine '{}'", Arrays.asList(cmdArray));
            } else {
                Runtime.getRuntime().exec(commandLine);
                logger.info("executed commandLine '{}'", commandLine);
            }
        } catch (IOException e) {
            logger.error("couldn't execute commandLine '" + commandLine + "'", e);
        }
    }

    /**
     * <p>
     * Executes <code>commandLine</code>. Sometimes (especially observed on MacOS) the commandLine isn't executed
     * properly. In that cases another exec-method is to be used. To accomplish this please use the special delimiter '
     * <code>@@</code>'. If <code>commandLine</code> contains this delimiter it is split into a String[] array and the
     * special exec-method is used.
     * </p>
     * <p>
     * A possible {@link IOException} gets logged but no further processing is done.
     * </p>
     *
     * @param commandLine
     *                          the command line to execute
     * @param timeoutMillis
     *                          timeout for execution in milliseconds
     * @param logger        Logger to log to. If null, no logging is done.
     * @return a map of exitcode and response data from executed command line
     * @throws InterruptedException if timeout occures
     * @throws IOException          if command couldn't be found
     * @throws ExecuteException     if there is error during command line execution
     */
    public static Map<Integer, String> executeCommandLineAndWaitResponse(String commandLine, int timeoutMillis,
            Logger logger) throws InterruptedException, ExecuteException, IOException {
        String retval = null;

        CommandLine cmdLine = null;

        if (commandLine.contains(DELIMITER)) {
            String[] cmdArray = commandLine.split(DELIMITER);
            cmdLine = new CommandLine(cmdArray[0]);

            for (int i = 1; i < cmdArray.length; i++) {
                cmdLine.addArgument(cmdArray[i], false);
            }
        } else {
            cmdLine = CommandLine.parse(commandLine);
        }

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMillis);
        Executor executor = new DefaultExecutor();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(stdout);

        executor.setExitValues(null);
        executor.setStreamHandler(streamHandler);
        executor.setWatchdog(watchdog);

        try {
            executor.execute(cmdLine, resultHandler);
            if (logger != null) {
                logger.debug("executed commandLine '{}'", commandLine);
            }
        } catch (ExecuteException e) {
            if (logger != null) {
                logger.warn("couldn't execute commandLine '" + commandLine + "'", e);
            }
            throw e;
        } catch (IOException e) {
            if (logger != null) {
                logger.warn("couldn't execute commandLine '" + commandLine + "'", e);
            }
            throw e;
        }

        // some time later the result handler callback was invoked so we
        // can safely request the exit code
        int exitCode = -1;
        try {
            resultHandler.waitFor();
            exitCode = resultHandler.getExitValue();
            retval = StringUtils.chomp(stdout.toString());
            if (resultHandler.getException() != null) {
                if (logger != null) {
                    logger.warn(resultHandler.getException().getMessage());
                }
                throw resultHandler.getException();
            } else {
                if (logger != null) {
                    logger.debug("exit code '{}', result '{}'", exitCode, retval);
                }
            }
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.warn("Timeout occured when executing commandLine '" + commandLine + "'", e);
            }
            throw e;
        }
        Map<Integer, String> retCodeStdout = new HashMap<>();
        retCodeStdout.put(exitCode, retval);
        return retCodeStdout;
    }

}
