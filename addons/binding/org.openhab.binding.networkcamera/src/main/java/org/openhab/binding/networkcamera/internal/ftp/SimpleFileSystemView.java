/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.networkcamera.internal.ftp;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple FTP file system view implementation.
 *
 *
 * @author Pauli Anttila - Initial contribution
 */
public class SimpleFileSystemView implements FileSystemView {
    private Logger logger = LoggerFactory.getLogger(SimpleFileSystemView.class);

    SimpleFtpFile file = new SimpleFtpFile();

    @Override
    public boolean changeWorkingDirectory(String arg0) throws FtpException {
        logger.trace("changeWorkingDirectory: {}", arg0);
        return true;
    }

    @Override
    public void dispose() {
        logger.trace("dispose");
    }

    @Override
    public FtpFile getFile(String arg0) throws FtpException {
        logger.trace("getFile: {}", arg0);
        return file;
    }

    @Override
    public FtpFile getHomeDirectory() throws FtpException {
        logger.trace("getHomeDirectory");
        return new SimpleFtpFile();
    }

    @Override
    public FtpFile getWorkingDirectory() throws FtpException {
        logger.trace("getWorkingDirectory");
        return new SimpleFtpFile();
    }

    @Override
    public boolean isRandomAccessible() throws FtpException {
        logger.trace("isRandomAccessible");
        return false;
    }
}
