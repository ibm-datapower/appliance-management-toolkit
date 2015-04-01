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

package com.ibm.datapower.amt.amp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.DeviceType;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.ModelType;
import com.ibm.datapower.amt.StringCollection;
import com.ibm.datapower.amt.clientAPI.Blob;
import com.ibm.datapower.amt.clientAPI.Firmware;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * This class provides utilities which can be used for parsing information needed
 * by the other classes in this package. More information can be found in each
 * method javadoc below. The <code>get*Firmware*</code> methods parse the
 * headers of a firmware file to extract metadata.
 * <p>
 */
//* <p>
public class Utilities {
    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;
    

    protected static final String CLASS_NAME = Utilities.class.getName();
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());
    }
    
    private static String XC10_Dev_Firmware_Cert_9005[] = {
    	"MIIDGjCCAgKgAwIBAgIBBTANBgkqhkiG9w0BAQUFADBLMQswCQYDVQQGEwJVUzEj",
    	"MCEGA1UEChMaRGF0YVBvd2VyIFRlY2hub2xvZ3ksIEluYy4xFzAVBgNVBAsTDkRl",
    	"dmVsb3BtZW50IENBMB4XDTEwMDEwNTIyNDUyMVoXDTEzMDYwNzIyNDUyMVowaTEL",
    	"MAkGA1UEBhMCVVMxIzAhBgNVBAoTGkRhdGFQb3dlciBUZWNobm9sb2d5LCBJbmMu",
    	"MRQwEgYDVQQLEwtFbmdpbmVlcmluZzEfMB0GA1UEAxMWSU5URVJOQUwgVVNFIE9O",
    	"TFkgKDA1KTCB3zANBgkqhkiG9w0BAQEFAAOBzQAwgckCgcEA1gDiKz4pkZY7W5ow",
    	"h42NoBP7e9FUO4JPt0ZJ7tVcRHMH/LYWAAmk70ZWIfUPuGAhlgWoBHCnG68tcrNb",
    	"hjYMOw5JBt0kv6ndiVOFuq52U6Xrf2Zdv2h/AqKOAldyGx2cJuj/i5so5YyK6b69",
    	"mdYh1VP0dX+5ka4cyLxCUgMxjA5k5meIaXU/PNTYJjhZOZ8x44ABf/XHl/vZYky0",
    	"0lTEunY4juIf6g426uCguMgggrzWFQbSeTgvaA+5rSSEu7C3AgMBAAGjLzAtMAkG",
    	"A1UdEwQCMAAwCwYDVR0PBAQDAgbAMBMGA1UdJQQMMAoGCCsGAQUFBwMDMA0GCSqG",
    	"SIb3DQEBBQUAA4IBAQA9DcNblVcUkinbgFUdr/PSRNumW7nlcaOUZuA9rNoEn1cY",
    	"0CfDc3T1bf9728ZUkuFkIpJaTyBE/JVPL6Pd91FPUNARsli8ZvogGcwD9kl8Kwcr",
    	"uoLUAXiJPi6iUIba1bICJk/zI2uZW/qJsh7NhAIrn/17ZGKOLL6isolTSz+m4GsN",
    	"0/4YhE9TUxvkp/cnwUz9lkim05/n/3dPB1B3tTu5gIX17lz4SbOMofk87cogs5h+",
    	"lCImftXY5ZZGSfIaA4RRCqobWNu4x0D0nhJMR0ecItsv/kyHmIDQ8xAca/gz3DQJ",
    	"rYfYbyyiCrB7IIXwZdyd8i3fdr1xFdkslv4NoL5e"
    };
    private static String XC10_Dev_Firmware_Cert_9004[] = {};
    private static String XC10_Rel_Firmware_Cert_9005[] = {
    	"MIIDozCCAougAwIBAgIBPDANBgkqhkiG9w0BAQUFADBiMQswCQYDVQQGEwJVUzEM",
    	"MAoGA1UEChMDSUJNMScwJQYDVQQLEx5XZWJTcGhlcmUgRGF0YVBvd2VyIEFwcGxp",
    	"YW5jZXMxHDAaBgNVBAMTE0N1c3RvbWVyIFJlbGVhc2UgQ0EwHhcNMTEwNTEyMTc1",
    	"OTU2WhcNMzgwMTAxMDAwMDAwWjBsMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJN",
    	"MScwJQYDVQQLEx5XZWJTcGhlcmUgRGF0YVBvd2VyIEFwcGxpYW5jZXMxJjAkBgNV",
    	"BAMTHTkwMDUgWEMxMCBGaXJtd2FyZSBFbmNyeXB0aW9uMIIBIjANBgkqhkiG9w0B",
    	"AQEFAAOCAQ8AMIIBCgKCAQEApznwiVGRc4QX1zD1J/Jwf35k01yN2/+gtl2Geuxz",
    	"rYFtC5jz0aLgBDwjcybgpqotcfXPDYdGlKLobIr9bXN3dzXrcVmTeG2nAmFEgjpF",
    	"bT//oaj+Q/1Mx0l9tu8Itfw0BRH8IFgL3+BODUj0AjdH6nDGzrXgM9g6cUs1DYsg",
    	"Jp/YFJPLgAv9bPL8C3eRLFUvF6nD9Ue7FbHBQLMmhEcMLLeAmOwukxISyHxn9Lx+",
    	"T4AuNN4ZubgQP2gRGd1rATTJFBuRVFr2tT6BblW8zxbtgkl+DXule+F7Gb5CSyfM",
    	"NZjRwEMpMG8p/XKHeQ5rD8gvc1esMYLDfYh/eihJC4yIjwIDAQABo1owWDAdBgNV",
    	"HQ4EFgQUkanaV/9rCLIhBK/PrBIXToWXP0cwHwYDVR0jBBgwFoAUCHtuCclEmUu6",
    	"P2LLkd28gjzEIZ0wCQYDVR0TBAIwADALBgNVHQ8EBAMCBDAwDQYJKoZIhvcNAQEF",
    	"BQADggEBAChEt9fkQHkUcB6heIld+j+Am5y7Y2ftSMnWM9CDstWecuExjyFlyj1s",
    	"pbhJ8PwbgyWFm5lG1Y1bDlFsS/ft/iOH1lgqaPLM4H4F6YwBqzeUSEKzJAehcu7E",
    	"msmfIgjysGBMqJ5WN/6DgWoPegtdWDMhMEbv80ykgH5mzKxoIxt08Tx7UDpkqiWD",
    	"Q81XUixnfAra6NDAaDAokpM1FutlNQDJvPCi/uibZednRil1Dgu0X8wi49hnAP+U",
    	"J1sZLdj73ARpeq+mN7qCbXjzl/lpxx9viN83glDnYMBqt27jjmMmkkkeBl1gbcga",
    	"IFaGF3yo58yo8FrAW2pFTSczbzQAlXs="
    };
    private static String XC10_Rel_Firmware_Cert_9004[] = {
    	"MIIDWjCCAkKgAwIBAgIBDzANBgkqhkiG9w0BAQUFADBiMQswCQYDVQQGEwJVUzEM",
    	"MAoGA1UEChMDSUJNMScwJQYDVQQLEx5XZWJTcGhlcmUgRGF0YVBvd2VyIEFwcGxp",
    	"YW5jZXMxHDAaBgNVBAMTE0N1c3RvbWVyIFJlbGVhc2UgQ0EwHhcNMTAwNDE5MDky",
    	"NzI1WhcNMzgwMTAxMDAwMDAwWjBnMQswCQYDVQQGEwJVUzEMMAoGA1UEChMDSUJN",
    	"MScwJQYDVQQLEx5XZWJTcGhlcmUgRGF0YVBvd2VyIEFwcGxpYW5jZXMxITAfBgNV",
    	"BAMTGFhDMTAgRmlybXdhcmUgRW5jcnlwdGlvbjCB3zANBgkqhkiG9w0BAQEFAAOB",
    	"zQAwgckCgcEAtqJ/+1ICpymAsbtCTX77ZAXoZA1qYjmWLPlZ2KA0S2Pi4vIOGIOO",
    	"zVgAMsP9xG0v0IFNd1n94+VK+fZggpyAV6CaSCpFrZWB5uK0lAz9a7C8iwOzpoCy",
    	"p/WYicGkVQxUoETl+x8TTCwQV6aAfLgpOJfhW72yJxO5IYN7V+W2Vf8JHMO6RLux",
    	"QnxzJTQ7sjubxaFKyIgcrcvdEYpReld1iuRrgMO7iUv4rUj/fmRtgCB4BZVR5YSN",
    	"TaCevSyybKNxAgMBAAGjWjBYMB0GA1UdDgQWBBTUQumvXm5jFo8egfUaoX8bhQyJ",
    	"vjAfBgNVHSMEGDAWgBQIe24JyUSZS7o/YsuR3byCPMQhnTAJBgNVHRMEAjAAMAsG",
    	"A1UdDwQEAwIEMDANBgkqhkiG9w0BAQUFAAOCAQEAbwpA4jWKsiY6ELP1hZKxwse/",
    	"uFxfbypMBQfWNpZu3pO7htSiZruWUws/o7mBg85lhp7t7FoGalmcPhvtgMoZYyJb",
    	"onV60KiSiCu61o+XVi4d7MnLd9ImuvzmeNsCdFrhe4IVjmoGZ44W0lU0qeZ+qC3A",
    	"UsnIAkHGosDvEvKmLKSpUhL0zW3/jhlcOFP41AkNId74VvcKrl8T2koG1piLjZMp",
    	"BCZr5Uf32F0t3Gk278Z4QR8AkZyANvByi4Lj2YxweXj3CNGbPMFPxzIoY3UVyZ6n",
    	"OijC4dWux/D35KdfN0Q+nQUqOjPFpqEq71BOSUhjv0m6GhDdKRyKF0O+x6yYkQ=="
    };

    private Utilities() {
        // Should not call this constructor.
        // Just use the static methods below.
    }

    /**
	 * Get the scrypt version of this firmware file. At the time of this
	 * writing, both scrypt1 and scrypt2 files are being published by the
	 * appliance manufacturer. However, the manager supports only scrypt2 because
	 * scrypt1 doesn't have headers that we need. The only valid value for this
	 * should be "2.0", although we would like this to be forward compatible.
	 * 
	 * @param firmwareImage
	 *            a reference to the firmware file blob
	 * @return a String representation of the scrypt version, i.e.,
	 *         <code>"2.0"</code>
	 * @throws IOException
	 *             a problem occurred while reading the firmware file
	 * @throws AMPException
	 *             a problem occurred while parsing the firmware file
	 */
    public static String getFirmwareScryptVersion(Blob firmwareImage) throws IOException, AMPException {
        final String METHOD_NAME = "getFirmwareScryptVersion"; //$NON-NLS-1$
    	String scryptVersion = Utilities.getMetaTagFromBlob("version", firmwareImage); //$NON-NLS-1$
        if (scryptVersion == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }

    	return(scryptVersion);
    }

    /**
     * Get the manufacturer's level (i.e., "3.5.0.9") that is embedded in a
     * firmware file.
     * 
     * @param firmwareImage a reference to the firmware file blob
     * @return a String representation of the firmware level, i.e.,
     *         <code>3.5.0.9</code>
     * @throws IOException a problem occurred while reading the firmware file
     * @throws AMPException a problem occurred while parsing the firmware file
     */
    public static String getFirmwareLevel(Blob firmwareImage) throws IOException, AMPException {
        
        final String METHOD_NAME = "getFirmwareLevel"; //$NON-NLS-1$
        String versionComposite = Utilities.getMetaTagFromBlob("firmwareRev", firmwareImage); //$NON-NLS-1$
        // fix for 59172        
        if (versionComposite == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        // version comes after the first period
        int periodIndex = versionComposite.indexOf("."); //$NON-NLS-1$
        
        String version = null;
        
        if(periodIndex == 4 || periodIndex == 3){
        	// The level takes the form OOOO.A.B.C.D like XI50.4.0.2.3
        	// It's used by DataPower devices 
        	version = versionComposite.substring(periodIndex + 1);
        }else if(periodIndex == 1){
        	// The level takes the form A.B.C.D like 2.0.0.0
        	// It's probably a XC10
        	
        	if("2.0".equals(versionComposite)){
        		// due to inconsistency in XC10 image header
        		// we have to use <buildDate> to guess the actual firmware level
        		String buildDate = Utilities.getMetaTagFromBlob("buildDate", firmwareImage); //$NON-NLS-1$
        		
        		if("2011/06/11 00:04:24".equals(buildDate)){
        			version = "2.0.0.1";
        		}else if("2011/06/11 00:17:47".equals(buildDate)){
        			version = "2.0.0.1";
        		}else if("2011/11/28 20:30:37".equals(buildDate)){
        			version = "2.0.0.2";
        		}else if("2011/11/28 20:40:14".equals(buildDate)){
        			version = "2.0.0.2";
        		}else if("2012/01/31 04:07:32".equals(buildDate)){
        			version = "2.0.0.3";
        		}else if("2012/01/31 04:17:29".equals(buildDate)){
        			version = "2.0.0.3";
        		}
        	}else{
        	   	version = versionComposite;
        	}        	
        }else{
        	// Something's wrong
        }        
        
        return(version);
    }

    /**
     * Get the manufacturer's build date that is embedded in a firmware file.
     * 
     * @param firmwareImage a reference to the firmware file blob
     * @return a timestamp of the firmware build
     * @throws IOException a problem occurred while reading the firmware file
     */
    public static Date getFirmwareManufactureDate(Blob firmwareImage) throws IOException {
        Date date = null;
        String dateString = Utilities.getMetaTagFromBlob("buildDate", firmwareImage); //$NON-NLS-1$
        // no fix for 59172 - ok to be null here, since it is handled.
        DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            date = null;
        }
        return(date);
    }
    
    /**
     * Get the deviceType that this firmware image is for.
     * 
     * @param firmwareImage a reference to the firmware file blob
     * @return the type of device that this firmware is for
     * @throws IOException a problem occurred while reading the firmware file
     * @throws AMPException a problem occurred while parsing the firmware file
     * @see Firmware#isCompatibleWith(com.ibm.datapower.amt.clientAPI.Device)
     */
    public static DeviceType getFirmwareDeviceType(Blob firmwareImage) throws IOException, AMPException {
        final String METHOD_NAME = "getFirmwareDeviceType"; //$NON-NLS-1$
        DeviceType deviceType = null;
        String productString = Utilities.getMetaTagFromBlob("product", firmwareImage); //$NON-NLS-1$
        // fix for 59172        
        if (productString == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        // Can find the colon delimiter, it could be a XC10
        if(productString.indexOf(':') == -1){
        	deviceType = DeviceType.fromString("XC10");
            return(deviceType);
        }
        
        String[] elements = productString.split(":"); //$NON-NLS-1$

        String deviceString = null;
        if (elements.length > 0) {
            deviceString = elements[0];
            
            // Defect 9949, 9965
            // Special handling for 9005 2U models
            if("9005".equals(elements[1])){
            	if("XI50".equalsIgnoreCase(deviceString)){
            		deviceString = "XI52";
            	}else if ("XB60".equalsIgnoreCase(deviceString)){
            		deviceString = "XB62";
            	}
            }
            // Defect 13467:XI52 5795 Virtual appliance firmware shows as XI50
            // To handle the virtual model 
            else if ( "virtual".equals(elements[1]) ) { 
            	if ( "XI50".equalsIgnoreCase(deviceString) )
            		deviceString = "XI52";
            }
        } else {
            deviceString = "XS40"; //$NON-NLS-1$
            String defDevTypeName = DeviceType.fromString(deviceString).getDisplayName();
            String message = Messages.getString("wamt.amp.Utilities.prodNotParsed",defDevTypeName); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
        }
        deviceType = DeviceType.fromString(deviceString);
        return(deviceType);
    }
    
    /**
     * Get the modelType that this firmware image is for.
     * @param firmwareImage a reference to the firmware file blob
     * @return the modelType that this firmware is for
     * @throws IOException a problem occurred while reading the firmware file
     * @throws AMPException a problem occurred while parsing the firmware file
     * @see Firmware#isCompatibleWith(com.ibm.datapower.amt.clientAPI.Device)
     */
    public static ModelType getFirmwareModelType(Blob firmwareImage) throws IOException, AMPException {
        final String METHOD_NAME = "getFirmwareModelType"; //$NON-NLS-1$
        ModelType modelType = null;
        String productString = Utilities.getMetaTagFromBlob("product", firmwareImage); //$NON-NLS-1$
        // fix for 59172 
        if (productString == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        // Can find the colon delimiter, it could be a XC10
        if(productString.indexOf(':') == -1){
        	// trying to infer model type from filename ext, if there is one
        	
            String cert[] = getCertificateFromBlob(firmwareImage);
            
            if(Arrays.equals(cert, XC10_Rel_Firmware_Cert_9005)){
            	return ModelType.fromString("9005");
            }else if(Arrays.equals(cert, XC10_Rel_Firmware_Cert_9004)){
            	return ModelType.fromString("9003");
            }else if(Arrays.equals(cert, XC10_Dev_Firmware_Cert_9005)){
            	return ModelType.fromString("9005");
            }else if(Arrays.equals(cert, XC10_Dev_Firmware_Cert_9004)){
            	return ModelType.fromString("9003");
            }
        	/*
        	if(fileExt != null){
        		if(fileExt.equals("scrypt2")){
        			return ModelType.fromString("9003");
        		}else if (fileExt.equals("scrypt3")){
        			return ModelType.fromString("9005");
        		}
        	}*/     	
        	
            return null;
        }
        
        String[] elements = productString.split(":"); //$NON-NLS-1$
        if (elements.length > 1) {
            String modelString = elements[1];
            modelType = ModelType.fromString(modelString);
        } else {
            String message = Messages.getString("wamt.amp.Utilities.prodNotParsed1"); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            modelType = null;
        }
        return(modelType);
    }

    /**
     * Get the list of strict features (libraries) that are included in this firmware image.
     * For a description of what a strict feature is, refer to the javadoc for getIncompatibilityReason().
     * @param firmwareImage a reference to the firmware file blob
     * @return a list of strict features that are included in this firmware image
     * @throws IOException a problem occurred while reading the firmware file
     * @throws AMPException a problem occurred while parsing the firmware file
     * @see com.ibm.datapower.amt.clientAPI.Firmware#isCompatibleWith(com.ibm.datapower.amt.clientAPI.Device)
     */
    public static StringCollection getStrictFirmwareFeatures(Blob firmwareImage) throws IOException, AMPException {
        final String METHOD_NAME = "getStrictFirmwareFeatures"; //$NON-NLS-1$
        StringCollection result = null;
        String featuresString = null;
        String productString = Utilities.getMetaTagFromBlob("product", firmwareImage); //$NON-NLS-1$
        // fix for 59172 
        if (productString == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        // Can find the colon delimiter, it could be a XC10
        if(productString.indexOf(':') == -1){
            return new StringCollection();
        }
        
        String[] elements = productString.split(":"); //$NON-NLS-1$
        if (elements.length > 1) {
            if (elements.length > 3) {
                featuresString = elements[3];
            } else {
                String message = Messages.getString("wamt.amp.Utilities.notEnufElts"); //$NON-NLS-1$
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, message);
                featuresString = ""; //$NON-NLS-1$
            }
            String[] featuresWithVersion = null;
            if (featuresString.length() == 0) {
                featuresWithVersion = new String[0];
            } else {
                featuresWithVersion = featuresString.split(","); //$NON-NLS-1$
            }
            Vector featureNameCollection = new Vector();
            for (int i=0; i<featuresWithVersion.length; i++) {
                int versionDelimiterIndex = featuresWithVersion[i].indexOf('=');
                String featureName = null;
                String featureVersion = null;
                if (versionDelimiterIndex > 0) {
                    featureName = featuresWithVersion[i].substring(0, versionDelimiterIndex);
                    featureVersion = featuresWithVersion[i].substring(versionDelimiterIndex+1);
                    featureVersion = featureVersion.replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    featureName = featuresWithVersion[i];
                    featureVersion = null;
                }
                // Defect 9966
                featureNameCollection.add(featureName+(featureVersion==null? "" : "_"+featureVersion));
            }
            String[] featureNameArray = (String[]) featureNameCollection.toArray(new String[featureNameCollection.size()]);
            result = new StringCollection(featureNameArray);
            
        } else {
            // this looks like an old firmware file with unsupported headers
            String message = Messages.getString("wamt.amp.Utilities.prodNotParsed1"); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            result = null;
        }
        return(result);
    }

    /**
     * Get the list of non-strict features (libraries) that are included in this firmware image.
     * For a description of what a non-strict feature is, refer to the javadoc for getIncompatibilityReason().
     * 
     * @param firmwareImage a reference to the firmware file blob
     * @return a list of non-strict features that are included in the firmware file
     * @throws IOException a problem occurred while reading the firmware file
     * @throws AMPException a problem occurred while parsing the firmware file
     * @see com.ibm.datapower.amt.clientAPI.Firmware#isCompatibleWith(com.ibm.datapower.amt.clientAPI.Device)
     */
    public static StringCollection getNonStrictFirmwareFeatures(Blob firmwareImage) throws IOException, AMPException {
        final String METHOD_NAME = "getNonStrictFirmwareFeatures"; //$NON-NLS-1$
        StringCollection result = null;
        String featuresString = null;
        String productString = Utilities.getMetaTagFromBlob("product", firmwareImage); //$NON-NLS-1$
        // fix for 59172 
        if (productString == null){
        	String message = Messages.getString("wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            AMPException e = new AMPException(message,"wamt.amp.Utilities.invalidImg"); //$NON-NLS-1$
            logger.throwing(CLASS_NAME, METHOD_NAME, e);
            throw e;
        }
        
        // Can find the delimiter colon, it could be a XC10
        if(productString.indexOf(':') == -1){
            return new StringCollection();
        }
        
        String[] elements = productString.split(":"); //$NON-NLS-1$
        if (elements.length > 2) {
            featuresString = elements[2];
            
            String[] featuresWithVersion = null;
            if (featuresString.length() == 0) {
                featuresWithVersion = new String[0];
            } else {
                featuresWithVersion = featuresString.split(","); //$NON-NLS-1$
            }
            Vector featureNameCollection = new Vector();
            for (int i=0; i<featuresWithVersion.length; i++) {
                int versionDelimiterIndex = featuresWithVersion[i].indexOf('=');
                String featureName = null;
                String featureVersion = null;
                if (versionDelimiterIndex > 0) {
                    featureName = featuresWithVersion[i].substring(0, versionDelimiterIndex);
                    featureVersion = featuresWithVersion[i].substring(versionDelimiterIndex+1);
                    featureVersion = featureVersion.replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    featureName = featuresWithVersion[i];
                    featureVersion = null;
                }
                // Defect 9966
                featureNameCollection.add(featureName+(featureVersion==null? "" : "_"+featureVersion));
            }
            String[] featureNameArray = (String[]) featureNameCollection.toArray(new String[featureNameCollection.size()]);
            result = new StringCollection(featureNameArray);

        } else {
            String message = Messages.getString("wamt.amp.Utilities.prodNotParsed1"); //$NON-NLS-1$
            logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, message);
            result = null;
        }
        return(result);
    }
    
    private static String[] getCertificateFromBlob(Blob blob){
    	InputStream inputStream = null;
    	InputStreamReader inputStreamReader = null;
    	BufferedReader bufferedReader = null;
    	String line = null;
    	boolean found = false;
    	
    	ArrayList<String> lines = new ArrayList<String>();
    	
		try {
			inputStream = blob.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(inputStreamReader);
			
			// Search for begin line
			while(bufferedReader.ready() == true){
				line = bufferedReader.readLine();
				
				if(line != null && line.trim().equals("-----BEGIN CERTIFICATE-----")){
					found = true;
					break;
				}    		
			}
			
			if(!found){
				return new String[]{};
			}
			
			found = false;
			// Go on to read the certificate content
			while(bufferedReader.ready() == true){
				line = bufferedReader.readLine();
				
				if ( line != null ) {
					if(line.trim().equals("-----END CERTIFICATE-----")){
						found = true;
						break;
					}else{
						lines.add(line.trim());
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if(bufferedReader != null){
					bufferedReader.close();
				}
				if(inputStreamReader != null){
					inputStreamReader.close();
				}
				if(inputStream != null){
					inputStream.close();
				}
			} catch (IOException e) {
				// Ignore
			}			
		}
    	
    	if(found){
    		return lines.toArray(new String[]{});
    	}else{
    		return new String[]{};
    	}
    }

    private static String getMetaTagFromBlob(String tagname, Blob blob) throws IOException {
        String contents = null;
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        LineNumberReader lineNumberReader = null;
        try {
            String openTag = "<" + tagname + ">"; //$NON-NLS-1$ //$NON-NLS-2$
            String closeTag = "</" + tagname + ">"; //$NON-NLS-1$ //$NON-NLS-2$
            inputStream = blob.getInputStream();
            //fix for 59189
            //inputStreamReader = new InputStreamReader(inputStream);
            inputStreamReader = new InputStreamReader(inputStream, "ISO-8859-1"); //$NON-NLS-1$
            bufferedReader = new BufferedReader(inputStreamReader);
            lineNumberReader = new LineNumberReader(bufferedReader);
            if ( lineNumberReader != null ) {
	            while (lineNumberReader.ready() && (contents == null)) {
	                String line = lineNumberReader.readLine();
	                if ( line != null ) {
		                if (line.indexOf("-----BEGIN ") > -1) { //$NON-NLS-1$
		                    // missed it, abort
		                    break;
		                }
		                int openTagIndex = line.indexOf(openTag); 
		                if (openTagIndex > -1) {
		                    // this line. Look for close tag
		                    int closeTagIndex = line.lastIndexOf(closeTag);
		                    int beginIndex = openTagIndex + openTag.length();
		                    int endIndex = closeTagIndex;
		                    contents = line.substring(beginIndex, endIndex);
		                }
	                }
	            }
            }
        } finally {
            try {
                if (lineNumberReader != null) {
                    lineNumberReader.close();
                } else if (bufferedReader != null) {
                    bufferedReader.close();
                } else if (inputStreamReader != null) {
                    inputStreamReader.close(); 
                } else if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                // just doing a close, eat it
            }
        }
        return(contents);
    }
    
	public static String getLocalIPAddress(String ipInterface) throws SocketException {
		final String METHOD_NAME = "selectIPAddress"; //$NON-NLS-1$
		
		String ipAddrString = null;
		Enumeration<NetworkInterface> interfaceEnum = null;
		
		if (ipInterface != null && (ipInterface.length() >0)) {
			logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Looking for interface " + ipInterface); //$NON-NLS-1$
			Map<String, String> interfaceAddrsMap = new HashMap<String, String>();
			interfaceEnum = NetworkInterface.getNetworkInterfaces();
			// Walk all the interfaces and get an ipAddr for each one to put in the Map.
			// Print them all out before selecting one.
			while (interfaceEnum.hasMoreElements()) {
				NetworkInterface networkInterface = (NetworkInterface) interfaceEnum.nextElement();
            
				String thisInterfaceName = networkInterface.getName();
				String thisInterfaceDisplayName = networkInterface.getDisplayName();
            
				Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
				while (addressEnum.hasMoreElements()) {
					InetAddress address = addressEnum.nextElement();
					//fix 59099
					if (address instanceof Inet4Address){
						if (!address.isLoopbackAddress() && !address.isMulticastAddress()) {
							String thisAddressString = address.getHostAddress();
							interfaceAddrsMap.put(thisInterfaceName, thisAddressString);
						}
					}
					else if (address instanceof Inet6Address){
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found an IPv6 address "+ address.getHostAddress() +  //$NON-NLS-1$ 
								" on interface " + thisInterfaceName + " (" + thisInterfaceDisplayName + ")");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						/*
						if (!address.isLoopbackAddress() && !address.isMulticastAddress()) {
							String thisAddressString = address.getHostAddress();
							interfaceAddrsMap.put(thisInterfaceName, thisAddressString);
						}*/
					}
					else {
						// Not IPv4 nor IPv6
						logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
								"Found an Address - not IPv4 or IPv6 = " + address.getHostAddress()); //$NON-NLS-1$
					}
				}
			}
        
			if (interfaceAddrsMap.containsKey(ipInterface)) {
				ipAddrString = (String) interfaceAddrsMap.get(ipInterface);
				logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Will use " + ipAddrString + //$NON-NLS-1$ 
						" from requested interface " + ipInterface); //$NON-NLS-1$
			} else {
				logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Did not find requested interface " + ipInterface); //$NON-NLS-1$
			}
	        if(ipInterface != null && (ipInterface.length() >0)){
	        	return ipAddrString;
	        }
		}
        
        ipAddrString = null;
        
        // either no interface was requested or couldn't find one on requested interface. Just get any valid IP.
        interfaceEnum = NetworkInterface.getNetworkInterfaces();
        while (interfaceEnum.hasMoreElements() && (ipAddrString==null || (ipAddrString.length() < 1))) {
            NetworkInterface networkInterface = (NetworkInterface) interfaceEnum.nextElement();
            Enumeration<InetAddress> addressEnum = networkInterface.getInetAddresses();
            
            while (addressEnum.hasMoreElements() && (ipAddrString == null || (ipAddrString.length() < 1))) {
                InetAddress address = (InetAddress) addressEnum.nextElement();
                //fix 59099
                if (address instanceof Inet4Address){
                    if (!address.isLoopbackAddress() && !address.isMulticastAddress()) {
                        ipAddrString = address.getHostAddress();
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found an IPv4 address " + //$NON-NLS-1$
                        		ipAddrString + " on interface " + networkInterface.getName()); //$NON-NLS-1$
                    }
                }
                else if (address instanceof Inet6Address){
                	//ipAddrString = address.getHostAddress();
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found an IPv6 address "+ address.getHostAddress() +  //$NON-NLS-1$ 
                    		" on interface " + networkInterface.getName());  //$NON-NLS-1$  //$NON-NLS-2$
                }
                else {
                    logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, 
                    		"Found an Address - not IPv4 or IPv6 = " + address.getHostAddress()); //$NON-NLS-1$
                }
            }
        }
        
		return ipAddrString;
	}
	
	/**
	 * Get WAMT repository directory
	 * @return the path of WAMT repository
	 * @throws SocketException
	 */
	public static String getRepositoryDirectory() {
		String DEFAULT_WAMTCONFIG_HOME_PROP = "user.home";
	    String DEFAULT_WAMTCONFIG_REPOSITORY_DIR = "WAMTRepository";
		
	    // Fix the PMR #13822 to get the repository directory via the "RepositoryDirectory" property on the Credential object
	    String repository_dir = System.getProperty("WAMT_REPOS_HOME");
		String WAMTReposPath = System.getenv("WAMT_REPOS_HOME");
		
		if (repository_dir != null){
			WAMTReposPath = repository_dir;
		}
		else if (WAMTReposPath == null ){	        			
			// Use user.home directory if environment variable is not configured
			WAMTReposPath = System.getProperty(DEFAULT_WAMTCONFIG_HOME_PROP) + File.separator +
								DEFAULT_WAMTCONFIG_REPOSITORY_DIR + File.separator;
		}
		if (!WAMTReposPath.endsWith(File.separator)){  			    		  
			WAMTReposPath = WAMTReposPath + File.separator;		      		  
		}
		return WAMTReposPath;
	}
}
