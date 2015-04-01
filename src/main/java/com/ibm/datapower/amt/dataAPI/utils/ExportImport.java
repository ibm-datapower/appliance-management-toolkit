/**
 * Copyright 2014 IBM Corp.
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
 **/

package com.ibm.datapower.amt.dataAPI.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.dataAPI.DatastoreException;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * Utility class to provide export/import support for config files used by
 * implementers of com.ibm.datapower.amt.dataAPI
 * 
 * <p>
 * 
 * 
 * @version SCM ID: $Id: ExportImport.java,v 1.4 2010/09/02 16:24:53 wjong Exp $
 */
// * Created on Ju1 1, 2007
public class ExportImport {

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected static final String CLASS_NAME = ExportImport.class.getName();

    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }

    /**
     * The name of the config file in the export
     */
    static final String CONFIG_FILE_NAME_IN_ZIPFILE = "DPManager.xml";

    static final String EXPORT_COMMENT_PREFIX = "DPManager export file";

    static final int IO_BUFFER_SIZE = 1048576 * 16; // 16MB

    /**
     * Creates an output stream containing a zip of the passed config file and
     * versions.
     * 
     * @param os
     *            Output stream containing the zip. Closed at exit
     * @param configFile
     *            The DPManger config file. Saved as DPManager.xml in the zip
     * @param versions
     *            An array of version files. Each file is saved uncompressed in
     *            the zip.
     * @throws IOException
     */
    public static final void exportFiles(OutputStream os, File configFile,
            File[] versions) throws IOException {
        final String METHOD_NAME = "exportFiles";
        Object[] args = { os, configFile, versions };
        logger.entering(CLASS_NAME, METHOD_NAME, args);
        ZipOutputStream zipOS = null;
        FileInputStream configFileIS = null, versionIS = null;
        try {
            zipOS = new ZipOutputStream(os);
            String comment = EXPORT_COMMENT_PREFIX
                    + " "
                    + DateFormat.getDateInstance(DateFormat.LONG).format(
                            new Date());
            zipOS.setComment(comment);

            // put configfile in the zip
            // gets IOException naming missing file if file ! exist
            configFileIS = new FileInputStream(configFile);
            // String cfName = configFile.getName();
            logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "exporting "
                    + configFile.getAbsolutePath() + " as "
                    + CONFIG_FILE_NAME_IN_ZIPFILE);
            ZipEntry cfZipEntry = new ZipEntry(CONFIG_FILE_NAME_IN_ZIPFILE);
            cfZipEntry.setTime(configFile.lastModified()); // default is now
            zipOS.putNextEntry(cfZipEntry);
            copyStream(configFileIS, zipOS);
            configFileIS.close();
            configFileIS = null;
            zipOS.closeEntry();

            // put versions in the zip file{
            for (int i = 0; i < versions.length; i++) {
                File version = versions[i];
                // gets IOException naming missing file if file ! exist
                versionIS = new FileInputStream(version);
                String vName = version.getName();
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "exporting "
                        + version.getAbsolutePath() + " as " + vName);
                ZipEntry vZipEntry = new ZipEntry(vName);
                vZipEntry.setTime(version.lastModified()); // default is now
                /*
                 * domains & settings versions are compressed already. firmware
                 * version are base64. Let's not bother compressing versions. If
                 * you don't take the defaults you have to set length and crc
                 */
                vZipEntry.setMethod(ZipEntry.STORED);
                vZipEntry.setCompressedSize(version.length());
                vZipEntry.setSize(version.length());
                vZipEntry.setCrc(getCRC(versionIS).getValue());
                versionIS.close();

                versionIS = new FileInputStream(version); // reopen file
                zipOS.putNextEntry(vZipEntry);
                copyStream(versionIS, zipOS);
                zipOS.closeEntry();
                versionIS.close();
                versionIS = null;
            }
        } finally {
            // close streams in case something went wrong
            if (zipOS != null)
                try {
                    zipOS.close();
                } catch (Throwable t) {
                }
            if (configFileIS != null)
                try {
                    configFileIS.close();
                } catch (Throwable t) {
                }
            if (versionIS != null)
                try {
                    versionIS.close();
                } catch (Throwable t) {
                }
        }
        //logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    /**
     * Validates an import stream
     * 
     * @param isZip
     *            The input stream for a previous export.
     * @return whether or not the import stream is valid 
     * @throws IOException
     * @throws DatastoreException
     */
    public static final boolean isValidImport(ZipInputStream isZip) throws IOException, DatastoreException {
        final String METHOD_NAME = "isValidImport";
        Object[] args = { isZip };
        logger.entering(CLASS_NAME, METHOD_NAME, args);
        
        boolean retVal = false;
        try {
            ZipEntry cfZipEntry = isZip.getNextEntry();
            // protect against null & check to make sure the name matches what
            // we used
            if ((cfZipEntry != null)
                    && (cfZipEntry.getName()
                            .equals(CONFIG_FILE_NAME_IN_ZIPFILE))) {
            	retVal = true;
            }
        } finally {
            if (isZip != null)
                try {
                    isZip.close();
                } catch (Throwable t) {
                }
        }
        //logger.exiting(CLASS_NAME, METHOD_NAME, retVal);
        return retVal;
    }

    /**
     * Creates an input stream containing the DPManger config file from a
     * previous export. The versions from the export are written to the
     * versionDir directory.
     * 
     * @param isZip
     *            The input stream for the previous export.
     * @param versionsDir
     *            The directory where the version will be stored.
     * @return The temporary file containing the config file from the previous
     *         export. The temporary file will be deleted when the JVM exits.
     * @throws IOException
     * @throws DatastoreException
     */
    public static final File importFiles(ZipInputStream isZip,
            File versionsDir) throws IOException, DatastoreException {
        final String METHOD_NAME = "importFiles";
        Object[] args = { isZip, versionsDir };
        logger.entering(CLASS_NAME, METHOD_NAME, args);
        FileOutputStream configFileOS = null, versionOS = null;
        File retVal = null;
        try {
            ZipEntry cfZipEntry = isZip.getNextEntry();
            // protect against null & check to make sure the name matches what
            // we used
            File tmpConfigFile = null;
            
            if ((cfZipEntry != null)
                    && (cfZipEntry.getName()
                            .equals(CONFIG_FILE_NAME_IN_ZIPFILE))) {
            	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "import processing entry="+cfZipEntry.getName());
                tmpConfigFile = File.createTempFile(
                        CONFIG_FILE_NAME_IN_ZIPFILE, ".tmp");
                tmpConfigFile.deleteOnExit();
                configFileOS = new FileOutputStream(tmpConfigFile);
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "copying config file to "
                                + tmpConfigFile.getAbsolutePath());
                copyStream(isZip, configFileOS);
                configFileOS.close();
                configFileOS = null;
                isZip.closeEntry();
            } else {
//                DatastoreException e = new DatastoreException(
//                        "Input stream does not appear to be a valid export");
            	String message = Messages.getString("wamt.dataAPI.ExportImport.badImport"); //$NON-NLS-1$
                logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
                DatastoreException e = new DatastoreException(message,"wamt.dataAPI.ExportImport.badImport",null); //$NON-NLS-1$
                logger.throwing(CLASS_NAME, METHOD_NAME, e);
                throw (e);
            }
            ZipEntry vZipEntry;
            while ((vZipEntry = isZip.getNextEntry()) != null) {
                String vName = vZipEntry.getName();
                File version = new File(versionsDir, vName);
                versionOS = new FileOutputStream(version);
                logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
                        "copying version " + vName + "to "
                                + version.getAbsolutePath());
                copyStream(isZip, versionOS);
                versionOS.close();
                versionOS = null;
                if ( !version.setLastModified(vZipEntry.getTime()) ) {
                	logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Sets the last-modified time of the file or directory failed.");
                }
            }
            retVal = tmpConfigFile;
        } finally {
            if (isZip != null)
                try {
                    isZip.close();
                } catch (Throwable t) {
                }
            if (configFileOS != null)
                try {
                    configFileOS.close();
                } catch (Throwable t) {
                }
            if (versionOS != null)
                try {
                    versionOS.close();
                } catch (Throwable t) {
                }
        }
        //logger.exiting(CLASS_NAME, METHOD_NAME, args);
        return retVal;
    }

    /**
     * Copy an InputStream to an OutputStream
     * 
     * @param is
     * @param os
     * @throws IOException
     */
    private static CRC32 getCRC(InputStream is) throws IOException {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int readLength;
        CRC32 crc = new CRC32();
        while ((readLength = is.read(buffer)) > -1) {
            crc.update(buffer, 0, readLength);
        }
        return crc;
    }

    /**
     * Copy an InputStream to an OutputStream
     * 
     * @param is
     * @param os
     * @throws IOException
     */
    private static void copyStream(InputStream is, OutputStream os)
            throws IOException {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int readLength;
        while ((readLength = is.read(buffer)) > -1) {
            os.write(buffer, 0, readLength);
        }
    }

}
