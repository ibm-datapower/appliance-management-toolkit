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
package com.ibm.datapower.amt.clientAPI.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.amp.Utilities;
import com.ibm.datapower.amt.clientAPI.Manager;
import com.ibm.datapower.amt.logging.LoggerHelper;

public class KeyStoreInfo {
	public static final String COPYRIGHT_2013 = Constants.COPYRIGHT_2013;
    protected static final String CLASS_NAME = KeyStoreInfo.class.getName();
    
    private static final String DEFAULT_KEY_STORE_FILE = "dpcacerts";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "WAMTCertPass";
    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    
    private static String WAMTReposPath = Utilities.getRepositoryDirectory();
    
    static {
        LoggerHelper.addLoggerToGroup(logger, Manager.getLoggerGroupName());    
    }
    
    public static final String keyStoreFile = WAMTReposPath + DEFAULT_KEY_STORE_FILE;  //$NON-NLS-1$
    public static final String keyStorePassword = DEFAULT_KEY_STORE_PASSWORD;  //$NON-NLS-1$
    
    /**
     * Try to get the root certificate in certificate array
     * @param certificateToSearch the certificate is to search the top certificate of the certificate chain
     * @param certificates a certificate array to be searched
     * @return the top certificate of the certificate chain
     * @throws KeyStoreException
     */
    public static Certificate getRootCert(X509Certificate certificateToSearch, X509Certificate[] certificates) 
    throws KeyStoreException {
    	final String METHOD_NAME = "getRootCert"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME, 
        		new Object[]{certificateToSearch.getSubjectDN().getName() + "\n", certificateToSearch.getIssuerDN().getName()});

    	X509Certificate result = certificateToSearch;

        int iFound = 0;
        int iSize = certificates.length;
        while (true) {
            String issuerName = result.getIssuerDN().getName();
            printCert("The certificate to search\n", "", result);
            
	        for ( iFound=0; iFound < iSize; iFound++ ) {
	        	if ( result == certificates[iFound] )
	        		continue;
	        	// Not the certificate to be searched itself        		
		        if ( certificates[iFound].getSubjectDN().getName().equals(issuerName) ) {
		        	// found it, reset the result to find the top certificate
		        	result = certificates[iFound];
		        	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Try to find issuer of " + certificates[iFound].getIssuerDN().getName());
		        	break;
	        	}
	        }
	        if ( iFound == iSize ) { 
	        	break;
	        }
        }
    	
        logger.exiting(CLASS_NAME, METHOD_NAME, result.getSubjectDN().getName() + "\n" + result.getIssuerDN().getName());
    	return result;
    }
    
        
    /**
     * Add certificate in keyStoreFile to the dpKeyStore when the root chain of certificate is in the keyStore
     * @param keyStoreFile the trusted store file which contain the certificate to be added to dpKeyStore 
     * @param keyStorePassword the password of trusted store file
     * @param dpKeyStore the truest store to be added
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static void addKeyStore(File keyStoreFile, String keyStorePassword, KeyStore dpKeyStore) 
    throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	final String METHOD_NAME = "addKeyStore"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        
    	if (!keyStoreFile.exists()) {
            return;
        } else if (!keyStoreFile.canRead()) {
            return;
        }
    	
    	// Create the list for all dp certificates
    	String[] dpAlias = {"DataPowerCACert", "DataPowerCACert9005", "DataPowerSSLCACert"};
    	List<Certificate> dpCertList = new ArrayList<Certificate>();
    	for (int i=0; i < dpAlias.length; i++ ) {
    		X509Certificate dpCertificate = (X509Certificate) dpKeyStore.getCertificate(dpAlias[i]);
    		if ( dpCertificate != null ) {
	    		dpCertList.add(dpCertificate);	    		
	    		printCert("Certificate of dp\n", dpAlias[i], dpCertificate);
    		}
    	}
    	
    	// Open keyStore file
    	logger.logp(Level.FINEST, CLASS_NAME, METHOD_NAME, "Loading KeyStore " + keyStoreFile.getAbsolutePath() + "...");//$NON-NLS-1$    	
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream in = null; 
        	
        try {        	
        	in = new FileInputStream(keyStoreFile);
        	keyStore.load(in, keyStorePassword.toCharArray());
        } finally {
	    	if ( in != null  ) 
	    		in.close();        	        
	    }
        
        // Create certificate array in keyStore
        int sizeOfKeyStore = keyStore.size();
    	X509Certificate[] certs = new X509Certificate[sizeOfKeyStore];
    	
    	Enumeration<String> aliases = keyStore.aliases();
    	int index = 0;
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            if (keyStore.isCertificateEntry(alias)) {
            	X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            	certs[index] = certificate;
            	printCert("Certificate in keyStore\n", alias, certs[index]);
		        index++;
	        }
        }
        
        // add the certificate to the dpKeyStore
        aliases = keyStore.aliases();    	
        while ( aliases.hasMoreElements() ) {
            String alias = (String) aliases.nextElement();
            if (keyStore.isCertificateEntry(alias)) {
            	X509Certificate certificateToAdd = (X509Certificate) keyStore.getCertificate(alias);
            	// Try to find the top certificate of certificate in certificate chain
            	X509Certificate topCertificate = (X509Certificate) getRootCert(certificateToAdd, certs);            	
            	printCert("The top certificate is:\n", "", topCertificate);
            	if ( dpCertList.contains(topCertificate) && (topCertificate != certificateToAdd) ) {
            		// If the top certificate is in the dpCertList which mean the certificateToAdd is issues by dpCertList
            		// Add it to the dpKeyStore
            	    dpKeyStore.setCertificateEntry(alias, certificateToAdd);
            	}
            }
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
    
    public static void printCert(X509Certificate certificate) {
    	printCert("", "", certificate);
    }    
    
    public static void printCert(String preMsg, String alias, X509Certificate certificate) {
    	Date beginning = certificate.getNotBefore();
	    Date end = certificate.getNotAfter();
	    int constraints = certificate.getBasicConstraints();
	    
	    String certificateInfo = "";
	    if ( alias != null && alias.length() > 0 )
	    	 certificateInfo += alias +"\n";
	    certificateInfo += "  Subject: " + certificate.getSubjectDN().getName() + "\n";   //$NON-NLS-1$ //$NON-NLS-2$
	    certificateInfo += "  Issuer:  " + certificate.getIssuerDN().getName() + "\n";   //$NON-NLS-1$ //$NON-NLS-2$
	    certificateInfo += "  Valid:   " + beginning + " - " + end + "\n";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	    certificateInfo += "  Basic constraints: " + constraints;   //$NON-NLS-1$ //$NON-NLS-2$
	    logger.logp(Level.FINEST, CLASS_NAME, "printCert", preMsg+certificateInfo);//$NON-NLS-1$
    }
    
    public static void main(String[] args) throws Exception {
        File newTrustStoreFile = new File(keyStoreFile);
        if ( !newTrustStoreFile.exists() ) {
        	logger.logp(Level.FINE, CLASS_NAME, "", 
        			"The new certificate file: " + keyStoreFile + " does not exist, should be created later");  //$NON-NLS-1$
        }
        else {// Add it to the key store file list
        	// check the certificate if the root chain is in the masterStore
        	KeyStore masterStoreX509Trusts = KeyStore.getInstance(KeyStore.getDefaultType());
        	masterStoreX509Trusts.load(null, null);        	
        	KeyStoreInfo.addKeyStore(newTrustStoreFile, keyStorePassword, masterStoreX509Trusts);            	
        }
    }
}
