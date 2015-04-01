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



package com.ibm.datapower.amt.clientAPI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.amp.defaultCommon.CustomURLConnection;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A simple representation of a Binary Large OBject (BLOB). Blobs are used in the 
 * manager to create and hold binary firmware images, DomainVersion images, and 
 * DeploymentPolicyVersion images, however the firmware image is the only Blob that will
 * be exposed via the client API. Blobs for domains and deployment policies are handled 
 * inside the manager and not exposed via the client API. See {@link URLSource} 
 * for more information about how to work with Domain and DeploymentPolicy images in 
 * the clientAPI. 
 * <p>
 * This class can be instantiated via byte array, URL, or a File. Normally it would
 * seem that a byte array would be easiest, but in the case of a very large
 * binary object (i.e., multiple megabytes), a File or URL are nicer because they
 * lack the need to be loaded into memory at once and thus have a large
 * memory footprint. For that reason, the {@link #Blob(File)} constructor or 
 * {@link #Blob(URL)} constructor is
 * preferred over the {@link #Blob(byte[])} constructor, and it should be used
 * any time the content is available via a URL or a file. However, this class does provide
 * the ability to convert bidirectionally between a byte array and InputStream
 * no matter how it was originally instantiated. But beware that a conversion of
 * a large File to a byte array could be expensive because of the memory
 * footprint that the return value would occupy, so try to keep that use to a
 * minimum or at least quickly discard all references to that byte array return
 * value.
 * <p>
 * If you are an implementer of the dataAPI interface, you will be handed Blobs
 * which were constructed with byte arrays. This is because AMP gets the
 * DomainVersion blobs from the device via SOAP messages,
 * so they haven't been persisted in a local file at that point. When you
 * persist a DomainVersion blob to your repository, it is
 * HIGHLY recommended that you first invoke {@link #hasBytes()} to see if it was
 * instantiated with a byte array or a File object. If it was instantiated with
 * a byte array and your dataAPI implementation persists Blobs to a file, you
 * should serialize the Blob contents to a file in your repository, create a new
 * Blob instance using {@link #Blob(File)} with the repository file you just
 * created, and use that File-based Blob in the StoredDomainVersion 
 * object instead of the original byte-array-based Blob.
 * Then discard references to the original Blob with the byte array. After
 * garbage collection then the Blob with a byte array should no longer be in
 * memory. If you fail to follow this method, then every 
 * DomainVersion blob will be in memory in their entirety. With regard to
 * FirmwareVersion, hopefully the UI will create those Blobs via
 * {@link #Blob(File)} so you won't have this issue with that class, but verify
 * that the UI is doing it that way by invoking {@link #hasBytes()}.
 * <p>
 * @version SCM ID: $Id: Blob.java,v 1.5 2010/09/02 16:24:52 wjong Exp $
 */
public class Blob {
	// comment
    private volatile File file = null;
    private volatile URL url = null;
    private byte[] bytes = null;
    
    private volatile String filenameExtension = null;

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    
    protected static final String CLASS_NAME = Blob.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    /**
     * Create a new blob object from a URL.
     * 
     * @param url
     *            a URL representing the location of the blob source
     *            
     *            The URL location is not read during the constructor, it is 
     *            instead read by the consumer of {@link #getInputStream()},  
     *            or read into memory if you call {@link #getByteArray()}. So
     *            it is expected that this URL parameter should exist and be
     *            available for reading during the lifetime of this Blob object.
     *            If that is not possible, then you need to use the constructor
     *            {@link #Blob(byte[])}. Supported URL schemes are file:, http:,
     *            and https:
     */
    public Blob(URL url) {
        final String METHOD_NAME = "Blob(URL)"; //$NON-NLS-1$
        this.url = url;
        
        String fileName = url.getFile();
        int index = fileName.lastIndexOf('.');
        filenameExtension = fileName.substring(index+1, fileName.length());
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                "Creating Blob from input stream: " + url.toString()); //$NON-NLS-1$
    }
    
    /**
     * Create a new blob object from a file.
     * 
     * @param file
     *            a reference to the file that contains the binary data. The
     *            File contents are not read during the constructor, but are
     *            read by the consumer of {@link #getInputStream()}, or they
     *            are read into memory if you call {@link #getByteArray()}. So
     *            it is expected that this File parameter should exist and be
     *            available for reading during the lifetime of this Blob object.
     *            If that is not possible, then you need to use the constructor
     *            {@link #Blob(byte[])}.
     */
    public Blob(File file) {
        final String METHOD_NAME = "Blob(File)"; //$NON-NLS-1$
        this.file = file;
        
       	String fileName = file.getName();

       	int index = fileName.lastIndexOf('.');
       	filenameExtension = fileName.substring(index+1, fileName.length());
        
        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                "Creating Blob from file: " + file.getAbsolutePath()); //$NON-NLS-1$
    }
    
    /**
     * Create a new blob object from a byte array. We recommend against using
     * this, especially for the long term, because it means that the entire byte
     * array will be resident in memory for the lifetime of this object. It is
     * preferred that you use the {@link #Blob(File)} constructor so that this
     * object does not trigger large memory usage.
     * 
     * @param bytes
     *            the byte array that contains the binary data. This class will
     *            reference this byte array and not copy it, so beware of making
     *            changes to your array after you use it as an argument for this
     *            constructor.
     */
    public Blob(byte[] bytes) {
        final String METHOD_NAME = "Blob(byte[])"; //$NON-NLS-1$
        String message = "First 20 bytes of Blob: "; //$NON-NLS-1$
        if ( bytes != null ) {
        	this.bytes = bytes.clone();
	        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME,
	                "Creating Blob from byte array of length " + bytes.length); //$NON-NLS-1$
	        
	        StringBuffer buf = new StringBuffer(message);
	        for (int i=0; i<20 && i<bytes.length; i++) {
	            int a = bytes[i];
	            buf.append(Integer.toHexString(a & 0xff) + " "); //message += Integer.toHexString(a & 0xff) + " "; //$NON-NLS-1$
	        }
	        message = buf.toString();
        }
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, message);
    }
    
    /**
     * Query the object to see if it already has its contents stored as a
     * byte[]. Since doing a conversion from a File or Base64 to a byte[] could
     * potentially be very expensive (depending on the size of the File or
     * Base64), you may want to use this method to avoid invoking this
     * conversion if you are concerned about consuming heap memory.
     * <p>
     * This method needs to be public so it can be invoked by the dataAPI.
     * 
     * @return true if the Blob already has loaded a byte array, false if the
     *         Blob contents are stored in a file.
     */
    public boolean hasBytes() {
        boolean result = false;
        if (this.bytes != null) {
            result = true;
        }
        return(result);
    }
    
    /**
     * A check to verify that the content can be read when needed.
     * 
     * @return true if the content can be read, false otherwise. This is used
     *         mostly when a Blob was constructed from a File. This method
     *         checks to make sure the File exists and that the File is
     *         readable. If this Blob was constructed from a byte array 
     *         it always returns true.
     */
    public boolean canRead() {
        final String METHOD_NAME = "canRead"; //$NON-NLS-1$
        boolean result = false;
        if (this.bytes != null) {
            result = true;
        } else if (this.file != null) {
            if (this.file.exists() && this.file.canRead()) {
                result = true;
            }
        } else if (this.url != null) {
            long lastModified = 0;
        	try {
				URLConnection urlConn = this.url.openConnection();
				lastModified = urlConn.getHeaderFieldDate("Last-Modified", 0); //$NON-NLS-1$
			} catch (IOException e) {
				//It is unlikely that this exception will ever happen 
				//So eat the exception and the method keep lastModified at 0.
				//We will treat 0 as unreadable.
			}
			
			if (lastModified == 0) {
	            result = false;
			} else {
	            result = true;
			}
        } else {
            // this should not happen
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
            		Messages.getString("wamt.clientAPI.Blob.internalErr",this.toString())); //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Get the contents of the blob as a byte array.
     * <p>
     * If the Blob was initially constructed via a byte array, it will just
     * return a reference to that same byte array.
     * <p>
     * If the Blob was initially constructed via a File, it will open the file,
     * allocate a byte array large enough to hold the contents, read the file
     * into that byte array, and return a reference to the byte array. Doing
     * that may be quite expensive if the file is large. This method does not
     * cause this object to have a reference to the byte array, so as soon as
     * the caller is done with the return value it may be eligible for garbage
     * collection.
     * <p>
     * This method needs to be public so it can be invoked by the dataAPI.
     * 
     * @return a byte array representation of the binary data
     * @throws IOException
     *             there was a problem reading the file while converting it to a
     *             byte array.
     */
    public byte[] getByteArray() throws IOException {
        final String METHOD_NAME = "getByteArray"; //$NON-NLS-1$
        byte[] result = null;
        if (this.bytes != null) {
            result = this.bytes;
        } else if ((this.file != null) || (this.url != null)) {
            // need to load it
        	if (this.file != null) {
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
	                    "Creating byte array in Blob from file: " + this.file.getAbsolutePath()); //$NON-NLS-1$
        	} else {
	            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
	                    "Creating byte array in Blob from url" + this.url.toString()); //$NON-NLS-1$
        	}

        	InputStream inputStream = this.getInputStream();
            
            // read in 4k chunks until we determine the size
            int totalBytes = 0;
            int bytesRead = 0; 
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer,0,4096)) > 0) {
            	totalBytes += bytesRead;
            }
            inputStream.close();
            
            inputStream = this.getInputStream();
            
            // Read the blob into a byte array now that we know the size
            result = new byte[totalBytes];
            int increment = 4096;
            if (totalBytes < increment) {
            	increment = totalBytes;
            }
            int bufferIndex = 0;
            while ((bytesRead = inputStream.read(result,bufferIndex,increment)) > 0) {
            	bufferIndex += bytesRead;
                if ((totalBytes - bufferIndex) < 4096) {
                	increment = totalBytes - bufferIndex;
                }
            }
            
            inputStream.close();
        } else {
            // this should not happen
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("wamt.clientAPI.Blob.internalErr",this.toString())); //$NON-NLS-1$
        }

        if ( result != null )
        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME,
        			"Returning byte array, length: " + result.length); //$NON-NLS-1$
        String message = "First 20 bytes of Blob: "; //$NON-NLS-1$
        StringBuffer buf = new StringBuffer(message);
        for (int i=0; i<20 && i<result.length; i++) {
            int a = result[i];
            buf.append(Integer.toHexString(a & 0xff) + " "); //$NON-NLS-1$             
        }
        message = buf.toString();
        logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, message);

        return(result);
    }
    
    /**
     * Get the contents of the blob as an InputStream.
     * <p>
     * If the Blob was initially constructed via a File, this will open the file
     * and return the InputStream pointing at the beginning of the file. It is
     * up to the caller to close the InputStream.
     * <p>
     * If the Blob was initially constructed via a byte array, this will wrap
     * the byte array with an InputStream. No file is actually created, and the
     * byte array is not copied. For good measure, the caller should close the
     * InputStream.
     * <p>
     * This method needs to be public so it can be invoked by the dataAPI.
     * 
     * @return an InputStream representation of the binary data
     * @throws IOException
     *             there was a problem reading the file from the disk.
     */
    public InputStream getInputStream() throws IOException {
        final String METHOD_NAME = "getInputStream"; //$NON-NLS-1$
        InputStream result = null;
        if (this.file != null) {
            result = new FileInputStream(this.file);
        } else if (this.url != null) {
			CustomURLConnection customURLConn =  new CustomURLConnection(this.url);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Connect timeout is set to " + customURLConn.getConnectLimit());
            URLConnection connection = customURLConn.openConnection();
            if ( connection == null ) {
                String message = Messages.getString("wamt.clientAPI.Blob.connectionErr",this.url.getHost());
                IOException e = new IOException(message);
                logger.throwing(CLASS_NAME, METHOD_NAME,e);
                throw e;
            }
            
            HttpURLConnection httpConnection = (HttpURLConnection) connection;			
			if ( httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK ) {
				result = url.openStream();
			}
			else {
				// fix for 13324: Firmware images reported as not real, when using URL behind some form of authentication        		
				throw (new IOException(Integer.toString(httpConnection.getResponseCode()) + " - " + httpConnection.getResponseMessage()));
			}
        } else if (this.bytes != null) {
            result = new ByteArrayInputStream(this.bytes);
        } else {
            // this should not happen
            logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME,
                    Messages.getString("wamt.clientAPI.Blob.internalErr",this.toString())); //$NON-NLS-1$
        }
        return(result);
    }
    
    /**
     * Return the filename extension of the file client given when creating
     * this blob object. If a byte array is used to create the blob
     * the method will return null since there is no filename involved.  
     * 
     * @return a string of filename extension of the source file of the blob
     */
    public String getFilenameExtension(){
    	return filenameExtension;
    }
    
    /**
     * Create a new Blob object for Base64-encode firmware images, so that they
     * need to be encoded only once when initially added to the repository
     * instead of on each setFirmware request. This one-time encoding is a
     * performance/resource optimization.
     * <p>
     * Be aware that the contents of the new Blob (return value) are stored in
     * memory as a byte array rather than as a file. So you may want to persist
     * the contents to disk ASAP and create another Blob using
     * {@link #Blob(File)} so that the contents are held on disk instead of in
     * memory. Or you can make the return value eligible for garbage collection
     * ASAP.
     * 
     * @return a new Blob that is a Base64-encoded version of the existing Blob
     * @throws IOException
     *             there was a problem reading the original Blob
     */
    //TODO - TCB - this seems inefficient, a blob created from a byte[] may get 
    //             converted to an inputstream, back to byte[] then to a base64
    //             encoded byte[]. Perhaps this method should be "smarter" ?
    public Blob getBase64Encoded() throws IOException {
        // figure out how big the content is
        InputStream inputStream = this.getInputStream();

        // read in 4k chunks until we determine the size
        int size = 0;
        int bytesRead = 0; 
        byte[] buffer = new byte[4096];
        while ((bytesRead = inputStream.read(buffer,0,4096)) > 0) {
        	size += bytesRead;
        }
        inputStream.close();
        
        // allocate memory for the content and read it in
        byte[] content = new byte[size];
        inputStream = this.getInputStream();
        int increment = 4096;
        if (size < increment) {
        	increment = size;
        }
        int bufferIndex = 0;
        while ((bytesRead = inputStream.read(content, bufferIndex, increment)) > 0) {
        	bufferIndex += bytesRead;
            if ((size - bufferIndex) < 4096) {
            	increment = size - bufferIndex;
            }
        }
        inputStream.close();
        
        // Base64.encode();
        byte[] encodedContent = Base64.encodeBase64(content);
        // release the reference to "content"
        content = null;
        
        Blob result = new Blob(encodedContent);
        return(result);
    }
    
    /**
     * Get a String representation of this Blob for the purpose of debugging or
     * tracing.
     * 
     * @return a String representation of this Blob for the purpose of debugging
     *         or tracing. Only metadata will be included, not the binary
     *         contents
     */
    public String toString() {
        String result = ""; //$NON-NLS-1$
        if (this.bytes != null) {
            result = this.bytes.length + " bytes (original)"; //$NON-NLS-1$
        } else if (this.file != null) {
            result = this.file.length() + " bytes (file " + this.file.getAbsolutePath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else if (this.url != null) {
            result = " ?? bytes (url " + this.url.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            // this should not happen
        }
        return(result);
    }
    
    /**
     * Write the contents of the Blob to a file. This will put it on disk
     * independent of it having been constructed from a byte array or a file.
     * This is used primarily for generating trace/debug data.
     * 
     * @param filename the name of the file to create to hold the contents of
     *        the Blob.
     * @throws IOException there was a problem writing the file to disk.
     */
    public void toFile(String filename) throws IOException {
    	InputStream inputStream = this.getInputStream();
	    FileOutputStream outputStream = new FileOutputStream(filename);
	    try {    
	        // try writing in 4k chunks
	        int bytesRead = 0; 
	        byte[] buffer = new byte[4096];
	        while ((bytesRead = inputStream.read(buffer,0,4096)) > 0) {
	            outputStream.write(buffer, 0, bytesRead);
	        }
	        
	        outputStream.close();
	        inputStream.close();
    	} finally {    		
    		outputStream = null;    		
    	}
    }
    
    long getSize(){
    	if(file != null){
    		return file.length();
    	}else if (url != null){
    		return -1;
    	}else if(bytes != null){
    		return (long)(bytes.length);
    	}else{
    		return -1;
    	}
    }
}
