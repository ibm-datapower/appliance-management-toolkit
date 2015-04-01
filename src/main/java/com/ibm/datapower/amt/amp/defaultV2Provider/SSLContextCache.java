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


package com.ibm.datapower.amt.amp.defaultV2Provider;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.ibm.datapower.amt.Constants;
import com.ibm.datapower.amt.Messages;
import com.ibm.datapower.amt.clientAPI.Configuration;
import com.ibm.datapower.amt.clientAPI.util.KeyStoreInfo;
import com.ibm.datapower.amt.logging.LoggerHelper;

/**
 * A convenient way to build and cache an SSL Context.  This is used to 
 * build and cache an sslSocketFactory as well as an sslServerSocketFactory.
 *
 */
public class SSLContextCache {
    private SSLContext sslContext;
    private SSLSocketFactory sslSocketFactory = null;
    private SSLServerSocketFactory sslServerSocketFactory = null;

    private static SSLContextCache instance = null;
    private static final String KEYSTORE_TYPE = "JKS";  //$NON-NLS-1$

    public static final String COPYRIGHT_2009_2013 = Constants.COPYRIGHT_2009_2013;

    protected final static String CLASS_NAME = SSLContextCache.class.getName();
    
    protected final static Logger logger = Logger.getLogger(CLASS_NAME);
    static
    {
        LoggerHelper.addLoggerToGroup(logger, "WAMT"); //$NON-NLS-1$
    }

    public static final String SYSTEM_PROPERTY_KEYSTORE_FILENAME_NAME = "javax.net.ssl.keyStore";  //$NON-NLS-1$
    public static final String SYSTEM_PROPERTY_KEYSTORE_PASSWORD_NAME = "javax.net.ssl.keyStorePassword";   //$NON-NLS-1$
    public static final String SYSTEM_PROPERTY_TRUSTSTORE_FILENAME_NAME = "javax.net.ssl.trustStore";  //$NON-NLS-1$
    public static final String SYSTEM_PROPERTY_TRUSTSTORE_PASSWORD_NAME = "javax.net.ssl.trustStorePassword";  //$NON-NLS-1$

    // this is the DataPower CA certificate from root-ca-cert.pem for 9001 to 9235
    private static final String DATAPOWER_CA_CERT_PEM = 
        "-----BEGIN CERTIFICATE-----\n" +  //$NON-NLS-1$
        "MIIDATCCAemgAwIBAgIBADANBgkqhkiG9w0BAQQFADBEMQswCQYDVQQGEwJVUzEj\n" +  //$NON-NLS-1$
        "MCEGA1UEChMaRGF0YVBvd2VyIFRlY2hub2xvZ3ksIEluYy4xEDAOBgNVBAsTB1Jv\n" +  //$NON-NLS-1$
        "b3QgQ0EwHhcNMDMwNjExMTgyMzE2WhcNMjMwNjA2MTgyMzE2WjBEMQswCQYDVQQG\n" +  //$NON-NLS-1$
        "EwJVUzEjMCEGA1UEChMaRGF0YVBvd2VyIFRlY2hub2xvZ3ksIEluYy4xEDAOBgNV\n" +  //$NON-NLS-1$
        "BAsTB1Jvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDJT5qC\n" +  //$NON-NLS-1$
        "zoOsgNBHRSPmJuMT5/1MlWH8WLkELJ02ptIbmOIHIVVWFU3AHIfEFixTjJO275vz\n" +  //$NON-NLS-1$
        "m07ih5/Nnm0OCKMTdZoMnIe+RH4TVa5GoBc4HKVTadtrpQCwafRvmiS3UTmXsx79\n" +  //$NON-NLS-1$
        "OGM8dR/g5Llw7sWGeI4HGXWwYrQkTlPwF2rtSiWFv6pZLFQAvY/sWgQhuPGxOgNg\n" +  //$NON-NLS-1$
        "gNLBzIy6gFlCrhchcxqes2RLbWOdmlDmV/3frn0E+vr6lR88dyaQCUG5BMo0hBpC\n" +  //$NON-NLS-1$
        "pjn8WTdujU+hGVH5gIG43FAm9mqfX8GBhzgpUZwORHEghwPbjqWGLDj9RV+P7NOs\n" +  //$NON-NLS-1$
        "tZjapihpP87dbRzfAgMBAAEwDQYJKoZIhvcNAQEEBQADggEBACZtUBZalAv2TLfF\n" +  //$NON-NLS-1$
        "hBA15KfYQqcq2T0C32WfluR5Psme6ICRK+1X3WODJRO2IuAul972w5qI4jSBiJom\n" +  //$NON-NLS-1$
        "fsVZUU1ibL1AiSWKH2tOJIWK7H+wiRJBmzAQzfF+WTWXG9fCJej8PaSRpcogSwJc\n" +  //$NON-NLS-1$
        "s5USpXqTN0v7twzKSA//j6mkgwWWHPOa1hZyb9fNnV3iDUnUt3nqmOT0vvUexChx\n" +  //$NON-NLS-1$
        "yhNCKtYmMVcQvSALmTPgH6mkfN5kHTvAH1wl/ehUpE98B+G0evvkF1VbmF0LY8Ha\n" +  //$NON-NLS-1$
        "k6iRvfDDTwPSJwZgH6l4dLsNK1+LBmkNnRGX/wlFpxPpEGBrEHkSV0GEdTY5sRh0\n" +  //$NON-NLS-1$
        "HRgfH9o=\n" +  //$NON-NLS-1$
        "-----END CERTIFICATE-----\n";  //$NON-NLS-1$
    
    // this is the DataPower CA certificate from root-ca-cert.pem for 9005 and later
    private static final String DATAPOWER_CA_CERT_PEM_9005 =
    	"-----BEGIN CERTIFICATE-----\n" +  //$NON-NLS-1$
    	"MIIDdTCCAl2gAwIBAgIBATANBgkqhkiG9w0BAQUFADBMMQswCQYDVQQGEwJVUzEM\n" +  //$NON-NLS-1$
    	"MAoGA1UEChMDSUJNMR0wGwYDVQQLExRXZWJTcGhlcmUgQXBwbGlhbmNlczEQMA4G\n" +  //$NON-NLS-1$
    	"A1UEAxMHUm9vdCBDQTAgFw0wOTEwMjcyMDU1NTNaGA8yMDM4MDEwMTAwMDAwMFow\n" +  //$NON-NLS-1$
    	"TDELMAkGA1UEBhMCVVMxDDAKBgNVBAoTA0lCTTEdMBsGA1UECxMUV2ViU3BoZXJl\n" +  //$NON-NLS-1$
    	"IEFwcGxpYW5jZXMxEDAOBgNVBAMTB1Jvb3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUA\n" +  //$NON-NLS-1$
    	"A4IBDwAwggEKAoIBAQC8/v9eoz54J76rWEkYfMlMmeFpA/tXbpV4ZL3K+3pN1vqa\n" +  //$NON-NLS-1$
    	"B6cc1U/UosnK/hurO462Undk9A1Nk8MiLKH+rAKGqtku64vo7W8thFxxFbXsB0PR\n" +  //$NON-NLS-1$
    	"lwvhkNljuCFBJqf7hkMjpiG3GtFwHpeyt+3gplI+9QOdJdvYZF98R4dsjfQ9QwpJ\n" +  //$NON-NLS-1$
    	"F22/Nu77w5Kq+7bWIwAaTYsYeYDcm9Ng6yPTwBlS37mIqlqfxE1IZ2Jf9p3OcMpL\n" +  //$NON-NLS-1$
    	"5ezvZUhVbkKCtcHybKbvn9m19Nbpw0/214o+UV7MOXenLDqwLdLI7vtyg5E0VxuJ\n" +  //$NON-NLS-1$
    	"e1GakqEMH9/f5yLVrtlQAPnSm3dZ+YIXdVPOyjvrAgMBAAGjYDBeMA8GA1UdEwEB\n" +  //$NON-NLS-1$
    	"/wQFMAMBAf8wHQYDVR0OBBYEFOE2HBFDaVDeL26jnyFvVXdvqT/ZMB8GA1UdIwQY\n" +  //$NON-NLS-1$
    	"MBaAFOE2HBFDaVDeL26jnyFvVXdvqT/ZMAsGA1UdDwQEAwIBBjANBgkqhkiG9w0B\n" +  //$NON-NLS-1$
    	"AQUFAAOCAQEAXzkYaNdfp8/qMd19SauIP3w+ci4JUcS5HVXvifxQrVPV4x7OAKod\n" +  //$NON-NLS-1$
    	"EjPQ4V5TjWwzSRGHoESEW+OGdZfiqKLcNyWpBS2IUn/Uc6/OUYCm1iEImftOgqOI\n" +  //$NON-NLS-1$
    	"q9CXKjAPsfbD5xYvMieOx6+ObQuGKN8R+PXAsZHusjFnU5bZJk+kALjrh6kZ6M+X\n" +  //$NON-NLS-1$
    	"QX6wQUGFlkLhepKcqVo0joxLrha6IxMsHmdj397XJCK6D8YJJRW9ILrp8ZI3+njR\n" +  //$NON-NLS-1$
    	"o72WCi/1T4L6KX89GFBhPHZ0x8rGkkF/N8Oz++LFNRzbwmIWe1d8lZFU3niur1K1\n" +  //$NON-NLS-1$
    	"jMTjcbfrwspvIJTASxZ1LkVQL0csmqaPvg==\n" +  //$NON-NLS-1$
    	"-----END CERTIFICATE-----\n";  //$NON-NLS-1$
    
    private static final String DATAPOWER_SSL_CA_CERT_PEM =
    	"-----BEGIN CERTIFICATE-----\n" +  //$NON-NLS-1$
    	"MIIDgzCCAmugAwIBAgIBBDANBgkqhkiG9w0BAQUFADBMMQswCQYDVQQGEwJVUzEM\n" +  //$NON-NLS-1$
    	"MAoGA1UEChMDSUJNMR0wGwYDVQQLExRXZWJTcGhlcmUgQXBwbGlhbmNlczEQMA4G\n" +  //$NON-NLS-1$
    	"A1UEAxMHUm9vdCBDQTAeFw0wOTEwMjcyMTExMTJaFw0zODAxMDEwMDAwMDBaMFwx\n" +  //$NON-NLS-1$
    	"CzAJBgNVBAYTAlVTMQwwCgYDVQQKEwNJQk0xJzAlBgNVBAsTHldlYlNwaGVyZSBE\n" +  //$NON-NLS-1$
    	"YXRhUG93ZXIgQXBwbGlhbmNlczEWMBQGA1UEAxMNU1NMIFNlcnZlciBDQTCCASIw\n" +  //$NON-NLS-1$
    	"DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKJ6/JJr/Z6mi5sg+BxsTiEMSQ4l\n" +  //$NON-NLS-1$
    	"aFvlj/RapXULUUXzT/BnYNeMPJr0byAgA/7rlEuWN52Df3bs0oyBF7a5gyfkSaX/\n" +  //$NON-NLS-1$
    	"asM75jV/V28NltLayQe+MOfE0jGwMp6fZn0vyxKfRaxHG/e9T7ro/lWGg+kvMNw8\n" +  //$NON-NLS-1$
    	"131RLV6pjpp2NepJgO/wqdLmlMjjdwC4k2P4Ps35H9yyrlzbNnW2k5BU+qajtaF+\n" +  //$NON-NLS-1$
    	"GpHqu81l5o+roujokWSLm4o7aYl7TVaoIgbT/PVDVgVR14xM1uaU3n5vxgWqYKPA\n" +  //$NON-NLS-1$
    	"gcfHrWqiSm5GLb+dAiqIGNH2hRk3s9gQZU6bEQK4vgk1iE4QfIeG1zZwsSsCAwEA\n" +  //$NON-NLS-1$
    	"AaNgMF4wHQYDVR0OBBYEFIK+led+TJngYkRxKaeaR1odcWvJMB8GA1UdIwQYMBaA\n" +  //$NON-NLS-1$
    	"FOE2HBFDaVDeL26jnyFvVXdvqT/ZMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQD\n" +  //$NON-NLS-1$
    	"AgEGMA0GCSqGSIb3DQEBBQUAA4IBAQBAxZwuVlFun4Dz2FWfBKxTh/DK8vkemY0S\n" +  //$NON-NLS-1$
    	"zdYNq0Y8H1KWNZu0jYFAjz/WGIhighsbSdhmO4gQUFYYGERs/AtFafXB90oqWxYH\n" +  //$NON-NLS-1$
    	"EEkT/rb2flHlPspWIf485sOeodaxnl7gIjWUi5ffVHMx1ItmmMztgZvmJUF5Plld\n" +  //$NON-NLS-1$
    	"hgayUrsThR51vAt7ZLEL2DSYTlwzfuSi1idEyA8ghb2t0ZTNeNGBvwSDLO0Wgl6F\n" +  //$NON-NLS-1$
    	"D2Kw91cx1HX/WOPc3OG7f48EwA8064W5TkF9zbhvfDLdazyPp/Zhc6HUlrnV6p6Q\n" +  //$NON-NLS-1$
    	"WlmIuyXWy04956CTCziAwoeLveYO/b867tz1kLqPKPJp8LrWYpeB\n" +  //$NON-NLS-1$
    	"-----END CERTIFICATE-----\n";
    
    // This is a default private key to be added, used only when no other 
    // server private key exists.
    private static final String TOOLKITV2_PRIV_KEY = 
        "-----BEGIN PRIVATE KEY-----\n" +  //$NON-NLS-1$
        "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2\n" +  //$NON-NLS-1$
        "USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4\n" +  //$NON-NLS-1$
        "O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmC\n" +  //$NON-NLS-1$
        "ouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCB\n" +  //$NON-NLS-1$
        "gLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhR\n" +  //$NON-NLS-1$
        "kImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEFgIUSGNkBA3RmA4+x/xff0nsREV1UQA=\n" +  //$NON-NLS-1$
        "-----END PRIVATE KEY-----\n";  //$NON-NLS-1$
    
    private static final String TOOLKITV2_CERT = 
        "-----BEGIN CERTIFICATE-----\n" +  //$NON-NLS-1$
        "MIIDCDCCAsWgAwIBAgIERU+OiDALBgcqhkjOOAQDBQAwZzELMAkGA1UEBhMCVVMxCzAJBgNVBAgT\n" +  //$NON-NLS-1$
        "Ak5DMRAwDgYDVQQHEwdSYWxlaWdoMQwwCgYDVQQKEwNJQk0xEjAQBgNVBAsTCURhdGFQb3dlcjEX\n" +  //$NON-NLS-1$
        "MBUGA1UEAxMORGF0YVBvd2VyIFVzZXIwHhcNMDYxMTA2MTkzNTM2WhcNMzEwNjI4MTkzNTM2WjBn\n" +  //$NON-NLS-1$
        "MQswCQYDVQQGEwJVUzELMAkGA1UECBMCTkMxEDAOBgNVBAcTB1JhbGVpZ2gxDDAKBgNVBAoTA0lC\n" +  //$NON-NLS-1$
        "TTESMBAGA1UECxMJRGF0YVBvd2VyMRcwFQYDVQQDEw5EYXRhUG93ZXIgVXNlcjCCAbcwggEsBgcq\n" +  //$NON-NLS-1$
        "hkjOOAQBMIIBHwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADDHj+AtlEmaUVdQCJR+1k9jVj6\n" +  //$NON-NLS-1$
        "v8X1ujD2y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7qdf+t8Yb+DtX58aophUPBPuD9tPF\n" +  //$NON-NLS-1$
        "HsMCNVQTWhaRMvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8VIwvMspK5gqLrhAvwWBz1AoGBAPfh\n" +  //$NON-NLS-1$
        "oIXWmz3ey7yrXDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrUWU/mcQcQgYC0SRZxI+hMKBYTt88J\n" +  //$NON-NLS-1$
        "MozIpuE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEkO8yk8b6oUZCJqIPf4VrlnwaSi2Ze\n" +  //$NON-NLS-1$
        "gHtVJWQBTDv+z0kqA4GEAAKBgG+Z93B4w+bqi0Uo9lRwand66B9aOcdGZIPnHQb1Wo2swROoZExL\n" +  //$NON-NLS-1$
        "LK9Q7DUkOO5baqR0u1m42VVBoXwIoJtIKz06WhaVuIso/mlpX1etvesmqPSJaOYn6qXYWElGi+br\n" +  //$NON-NLS-1$
        "jDAnO8cnJ87FEl414yaSn9KEPqgDiIezWWLPnv9J1xd7MAsGByqGSM44BAMFAAMwADAtAhUAkjkS\n" +  //$NON-NLS-1$
        "/EZdNyvhWaIANmXs7wZS/GMCFCWjQiVmTmEuYkrDe2MoOG/6zoBf\n" +  //$NON-NLS-1$
        "-----END CERTIFICATE-----\n";  //$NON-NLS-1$

    /**
     * Constructor.
     */
    public SSLContextCache() {
        final String METHOD_NAME = "SSLContextCache";  //$NON-NLS-1$
        try {
            sslContext = createCustomSSLContext();
            sslSocketFactory = sslContext.getSocketFactory();
            sslServerSocketFactory = sslContext.getServerSocketFactory();
        }
        catch (Exception e) {
             logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.exSSLCache"), e);  //$NON-NLS-1$
        }
    }

    public static SSLContextCache getInstance () {
        if (instance == null)
            instance = new SSLContextCache();
        return instance;
    }


    /**
     * Get the cached sslSocketFactory.
     * 
     * @return an SSLSocketFactory to be used with
     *         {@link HttpsURLConnection#setSSLSocketFactory(javax.net.ssl.SSLSocketFactory)}
     * @throws IOException
     */
    public SSLSocketFactory getCustomSSLSocketFactory() throws IOException {
        return(sslSocketFactory);
    }

    public SSLServerSocketFactory getCustomSSLServerSocketFactory() throws IOException {
        return(sslServerSocketFactory);
    }

    /**
     * We can choose which keys and certificates we want to trust on a
     * per-connection basis. This is something we need to do for AMP
     * connections, because the DataPower device may have an SSL server
     * certificate that is not signed by a well-known CA. So at runtime we will
     * add another trusted CA to our SSLContext. But in doing so we overwrite
     * the default SSLContext, so we'll need to get those keys and certs
     * manually too ($JAVA_HOME/lib/security/cacerts).
     * 
     * @return an SSLContext to be used
     */
    private SSLContext createCustomSSLContext() throws IOException, GeneralSecurityException {
        // Yes, this is a long messy method. IBMJSSE2 adds some complications too.
        
        // the following "final" values are defined in the IBM JSSE Reference Guide.
        // They assume use of the IBM implementation of JSSE and not the Sun implementation.
        final String METHOD_NAME = "createCustomSSLContext";  //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);

        final String sslContextProtocol = "TLS";  //$NON-NLS-1$
        final String keyManagerFactoryAlgorithm = "IbmX509";  //$NON-NLS-1$
        final String trustManagerFactoryAlgorithm = "IbmPKIX";  //$NON-NLS-1$
        final String keyStoreDefaultPassword = "changeit";  //$NON-NLS-1$
        
        // is easier if they are a resizable List instead of a fixed-length array
        List<File> keyStoreFilesList = new ArrayList<File>();
        List<String> keyStorePasswordsList = new ArrayList<String>();

        // if a keystore of keys was specified via properties, add it to the List
        String propertyKeyStore = System.getProperty(SYSTEM_PROPERTY_KEYSTORE_FILENAME_NAME);
        File propertyKeyStoreFile = null;
        String propertyKeyStorePassword = System.getProperty(SYSTEM_PROPERTY_KEYSTORE_PASSWORD_NAME);
        if (propertyKeyStorePassword == null) {
            // the property can define it as an empty string, but null means it is not defined
            propertyKeyStorePassword = keyStoreDefaultPassword;
        }
        if (propertyKeyStore != null) {
            propertyKeyStoreFile = new File(propertyKeyStore);
            keyStoreFilesList.add(propertyKeyStoreFile);
            keyStorePasswordsList.add(propertyKeyStorePassword);
        } else {
            // we'll catch the jssecacerts and cacerts file below
        }
        
        // follow the rules for getting to the truststore as defined in the JSSE Reference Guide
        String propertyTrustStore = System.getProperty(SYSTEM_PROPERTY_TRUSTSTORE_FILENAME_NAME);
        File propertyTrustStoreFile = null;
        String propertyTrustStorePassword = System.getProperty(SYSTEM_PROPERTY_TRUSTSTORE_PASSWORD_NAME);
        if (propertyTrustStorePassword == null) {
            // the property can define it as an empty string, but null means it is not defined
            propertyTrustStorePassword = keyStoreDefaultPassword;
        }
        if (propertyTrustStore != null) {
            propertyTrustStoreFile = new File(propertyTrustStore);
            keyStoreFilesList.add(propertyTrustStoreFile);
            keyStorePasswordsList.add(propertyTrustStorePassword);
        } else {
            propertyTrustStore = System.getProperty("java.home") + File.separator +  //$NON-NLS-1$
                "lib" + File.separator + "security" + File.separator + "jssecacerts";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            propertyTrustStoreFile = new File(propertyTrustStore);
            // these would be JSSE-specific certs, cacerts file below can do code signing too
            if (propertyTrustStoreFile.exists()) {
                keyStoreFilesList.add(propertyTrustStoreFile);
                keyStorePasswordsList.add(propertyTrustStorePassword);
            } else {
                propertyTrustStore = System.getProperty("java.home") + File.separator +  //$NON-NLS-1$
                    "lib" + File.separator + "security" + File.separator + "cacerts";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                propertyTrustStoreFile = new File(propertyTrustStore);
                // if it doesn't exist, that is OK
                keyStoreFilesList.add(propertyTrustStoreFile);
                keyStorePasswordsList.add(propertyTrustStorePassword);
            }
        }

        propertyTrustStore = Configuration.get(Configuration.KEY_TRUSTSTORE_FILENAME);
        propertyTrustStorePassword = Configuration.get(Configuration.KEY_TRUSTSTORE_PASSWORD);
        if (propertyTrustStore != null) {
            propertyTrustStoreFile = new File(propertyTrustStore);
            keyStoreFilesList.add(propertyTrustStoreFile);
            keyStorePasswordsList.add(propertyTrustStorePassword);
        }
        
        /*
         * A KeyManagerFactory and TrustManagerFactory do not use singletons.
         * Each call to KeyManagerFactory.getInstance() and
         * TrustManagerFactory.getInstance() will create a new instance, that
         * was not what I first expected. That is why we can't reuse the same
         * TrustManagerFactory when calling init() on the keystore. Doing so
         * would overwrite any previous TrustManager. (I suppose this is why
         * TrustManagerFactory has an init() method but not an add() method.) We
         * need the TrustManagerFactory to create a new TrustManager, and each
         * TrustManagerFactory can init only one TrustManager from one keystore.
         * After the TrustManager has been created by the TrustManagerFactory
         * then we can get a reference to it from the TrustManagerFactory and
         * just keep an array of those TrustManager references. We'll use that
         * array below to init the SSLContext with multiple TrustManagers. Same
         * thing applies to the KeyManagers.
         */
        
        List<KeyManager> keyManagerList = new ArrayList<KeyManager>();
        List<TrustManager> trustManagerList = new ArrayList<TrustManager>();
        
        Random random = new Random();
        
        try {

            /*
             * Apparently with IBMJSSE2 we need to put all the X.509 certs into
             * one TrustManager, additional TrustManagers of X.509 certs will be
             * ignored. That obviously would cause problems during certificate
             * chain validation, as only certs in the first TrustManager would
             * validate the chain. Using only one TrustManager means they need
             * to be in one keystore, since I can't find a way to use multiple
             * keystores in one TrustManager. So we will walk through all the
             * keystores and copy the certs into a single in-memory keystore,
             * which I will call "masterStoreX509Trusts". Then we can generate
             * our single TrustManager from that. I'm not sure if we need to do
             * the same for the keys (because we would need the key password in
             * addition to the keystore password, and we don't have the password
             * for every key). But keys would be required only for mutual SSL
             * authentication, which I don't think is likely.
             */
            
            KeyStore masterStoreX509Trusts = KeyStore.getInstance(KEYSTORE_TYPE);
            // even though we have an instance, it is not initialized. 
            // Initialize it via load() and null InputStream parameter.
            masterStoreX509Trusts.load(null, null);

            // walk through each keystore file to get the KeyManager and to copy the certs
            for (int index=0; index < keyStoreFilesList.size(); index++) {
                File keyStoreFile = (File) keyStoreFilesList.get(index);
                String keyStorePassword = (String) keyStorePasswordsList.get(index);
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Checking key store file " + keyStoreFile.getAbsolutePath());  //$NON-NLS-1$

                // skip if something is wrong with the keystore file
                if (!keyStoreFile.exists()) {
                    continue;
                } else if (!keyStoreFile.canRead()) {
                    continue;
                }

                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, "Loading key store file " + keyStoreFile.getAbsolutePath());  //$NON-NLS-1$

                // we pull both keys for the KeyManager and certs for the TrustManager from the same keystore file.

                KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                InputStream keyStoreInputStream = new FileInputStream(keyStoreFile);
                keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
                keyStoreInputStream.close();
 
                // create a KeyManager using the keys in the keystore
                KeyManagerFactory keyManagerFactory = null;
                try {
                	keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
                } catch (NoSuchAlgorithmException e) {
                    logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                                "Exception while trying to use " + keyManagerFactoryAlgorithm +   //$NON-NLS-1$
                                ", trying default " + KeyManagerFactory.getDefaultAlgorithm(), e);    //$NON-NLS-1$
                    keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                }
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
                KeyManager[] theseKeyManagers = keyManagerFactory.getKeyManagers();
                for (int i=0; i<theseKeyManagers.length; i++) {
                    keyManagerList.add(theseKeyManagers[i]);
                }

                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found " + theseKeyManagers.length + " KeyManagers");   //$NON-NLS-1$ //$NON-NLS-2$

                /*
                 * For the certs, it is somewhat similar to the keys, but we
                 * will copy all the certs into a single in-memory keystore so
                 * that there is only one keystore of X509 certs, so we have
                 * only one TrustManager for X509 CAs. Then we will create the
                 * single TrustManager when we are done walking through all the
                 * keystores.
                 */

                Enumeration aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = (String) aliases.nextElement();
                    if (keyStore.isCertificateEntry(alias)) {
                        Certificate certificate = keyStore.getCertificate(alias);
                        String newAlias = alias;
                        // avoid name collisions
                        StringBuffer buf = new StringBuffer(newAlias);
                        while (masterStoreX509Trusts.containsAlias(newAlias)) {
                        	buf.append(random.nextInt());  //newAlias = newAlias + random.nextInt();
                        	newAlias = buf.toString();
                        }
                        if (certificate instanceof X509Certificate) {
                            masterStoreX509Trusts.setCertificateEntry(newAlias, certificate);
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Added cert with alias " + alias + " to the temporary keystore");   //$NON-NLS-1$ //$NON-NLS-2$
                        } else {
                            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Certificate alias " + alias + " from " + keyStoreFile.getAbsolutePath() + " is not a X.509 certificate and will not be used as a trusted CA");    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        }
                    }
                }
            }
    
            KeyStore tempStore = KeyStore.getInstance(KEYSTORE_TYPE);
            tempStore.load(null, null);
            if ((TOOLKITV2_PRIV_KEY != null) && (TOOLKITV2_PRIV_KEY.length() > 0)) {
                // add a default server cert which is embedded in this class.
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Loading embedded TOOLKITV2_PRIV_KEY");  //$NON-NLS-1$

                addPrivateKey(tempStore, TOOLKITV2_PRIV_KEY, "WAMTV2", TOOLKITV2_CERT);  //$NON-NLS-1$
            }               
            // create a KeyManager using the keys in the temp key store
            KeyManagerFactory keyManagerFactory = null;
            try {
            	keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "Exception while trying to use " + keyManagerFactoryAlgorithm +   //$NON-NLS-1$
                            ", trying default " + KeyManagerFactory.getDefaultAlgorithm(), e);    //$NON-NLS-1$
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            }
            keyManagerFactory.init(tempStore, "WAMTV2".toCharArray());  //$NON-NLS-1$
            KeyManager[] theseKeyManagers = keyManagerFactory.getKeyManagers();
            for (int i=0; i<theseKeyManagers.length; i++) {
            	//fix for CS tracker #79265
                keyManagerList.add(0,theseKeyManagers[i]);
            }

            if ((DATAPOWER_CA_CERT_PEM != null) && (DATAPOWER_CA_CERT_PEM.length() > 0)) {
                // add the DataPower CA certificate. The PEM representation of that 
                // certificate is embedded in this class.
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Loading embedded DATAPOWER_CA_CERT_PEM");  //$NON-NLS-1$
                
                addCertificate(masterStoreX509Trusts, DATAPOWER_CA_CERT_PEM, "DataPowerCACert");  //$NON-NLS-1$
            }
            
            if ((DATAPOWER_CA_CERT_PEM_9005 != null) && (DATAPOWER_CA_CERT_PEM_9005.length() > 0)) {
                // add the DataPower CA certificate. The PEM representation of that 
                // certificate is embedded in this class.
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Loading embedded DATAPOWER_CA_CERT_PEM_9005");  //$NON-NLS-1$
                
                addCertificate(masterStoreX509Trusts, DATAPOWER_CA_CERT_PEM_9005, "DataPowerCACert9005");  //$NON-NLS-1$
            }
            
            if ((DATAPOWER_SSL_CA_CERT_PEM != null) && (DATAPOWER_SSL_CA_CERT_PEM.length() > 0)) {
                // add the DataPower CA certificate. The PEM representation of that 
                // certificate is embedded in this class.
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Loading embedded DATAPOWER_SSL_CA_CERT_PEM");  //$NON-NLS-1$
                
                addCertificate(masterStoreX509Trusts, DATAPOWER_SSL_CA_CERT_PEM, "DataPowerSSLCACert");  //$NON-NLS-1$
            }
            
            // Add new certificates sent from appliance if exists to fix the problem of certificate expiration
            File newTrustStoreFile = new File(KeyStoreInfo.keyStoreFile);
            if ( !newTrustStoreFile.exists() )
            	logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
            			"The new certificate file: " + KeyStoreInfo.keyStoreFile + " does not exist, should be created later");  //$NON-NLS-1$
            else { // Add it to the key store file list
	        	try {
	        		KeyStoreInfo.addKeyStore(newTrustStoreFile, KeyStoreInfo.keyStorePassword, masterStoreX509Trusts);
	        	} catch (IOException e) {
	        		logger.logp(Level.WARNING, CLASS_NAME, METHOD_NAME, "Failed to add certirifate to key store:" + KeyStoreInfo.keyStoreFile);
	        	}
            }
            
            // now do the usual to derive the TrustManager from the unified in-memory keystore
            TrustManagerFactory trustManagerFactory = null;
            try {
                trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                logger.logp(Level.FINE, CLASS_NAME, METHOD_NAME, 
                            "Exception while trying to use " + trustManagerFactoryAlgorithm +   //$NON-NLS-1$
                            ", trying default " + TrustManagerFactory.getDefaultAlgorithm(), e);    //$NON-NLS-1$
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            }
            trustManagerFactory.init(masterStoreX509Trusts);
            TrustManager[] theseTrustManagers = trustManagerFactory.getTrustManagers();
            for (int i=0; i<theseTrustManagers.length; i++) {
                trustManagerList.add(theseTrustManagers[i]);
            }
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Found " + theseTrustManagers.length + " TrustManagers");   //$NON-NLS-1$ //$NON-NLS-2$
            
            // convert the collections from a List to an array for SSLContext.init()
            KeyManager[] keyManagers = new KeyManager[keyManagerList.size()];
            for (int i=0; i<keyManagerList.size(); i++) {
                keyManagers[i] = (KeyManager) keyManagerList.get(i);
            }
            TrustManager[] trustManagers = new TrustManager[trustManagerList.size()];
            for (int i=0; i<trustManagerList.size(); i++) {
                trustManagers[i] = (TrustManager) trustManagerList.get(i);
                
                if (logger.isLoggable(Level.FINER)) {
                    // print some info about each CA cert
                    X509TrustManager x509TrustManager = (X509TrustManager) trustManagers[i];
                    X509Certificate[] certificates = x509TrustManager.getAcceptedIssuers();
                    for (int j=0; j<certificates.length; j++) {
                        String certificateInfo = "";  //$NON-NLS-1$
                        Principal principal = certificates[j].getSubjectDN();
                        String subjectName = principal.getName();
                        principal = certificates[j].getIssuerDN();
                        String issuerName = principal.getName();
                        Date beginning = certificates[j].getNotBefore();
                        Date end = certificates[j].getNotAfter();
                        int constraints = certificates[j].getBasicConstraints();
                        Set oids = certificates[j].getCriticalExtensionOIDs();
                        certificateInfo += "trust manager " + i + " has certificate:\n";   //$NON-NLS-1$ //$NON-NLS-2$
                        certificateInfo += "  Subject: " + subjectName + "\n";   //$NON-NLS-1$ //$NON-NLS-2$
                        certificateInfo += "  Issuer:  " + issuerName + "\n";   //$NON-NLS-1$ //$NON-NLS-2$
                        certificateInfo += "  Valid:   " + beginning + " through " + end + "\n";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        certificateInfo += "  Basic constraints: " + constraints + "\n";   //$NON-NLS-1$ //$NON-NLS-2$
                        certificateInfo += "  Critical extension oids: [";  //$NON-NLS-1$
                        if (oids != null) {
                            Iterator iterator = oids.iterator();
                            StringBuffer buf = new StringBuffer(certificateInfo);
                            while ((iterator != null) && (iterator.hasNext())) {
                                String oid = (String) iterator.next();
                                buf.append(" ");  // certificateInfo += " ";  //$NON-NLS-1$
                                buf.append(oid);  // certificateInfo += oid;                                
                            }
                            certificateInfo = buf.toString();
                        }
                        certificateInfo += " ]";  //$NON-NLS-1$
                        logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, certificateInfo);
                    }
                }
            }
            
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Creating SSLContext with " +   //$NON-NLS-1$
                        keyManagers.length + " keyManagers and " +  //$NON-NLS-1$
                        trustManagers.length + " trustManagers");  //$NON-NLS-1$
            
            sslContext = SSLContext.getInstance(sslContextProtocol);
            sslContext.init(keyManagers, trustManagers, null);
            logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Successfully obtained SSLContext");  //$NON-NLS-1$
        } catch (GeneralSecurityException caught) {
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.exCreateSSL"), caught); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return (sslContext);
    }
    
    X509Certificate readCert(String myCert) throws GeneralSecurityException,IOException {
        final String METHOD_NAME = "readCert"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        // instantiate the certificate
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509"); //$NON-NLS-1$
        InputStream certificateInputStream = new ByteArrayInputStream(myCert.getBytes());
        X509Certificate theCert = (X509Certificate) certificateFactory.generateCertificate(certificateInputStream);
        certificateInputStream.close();
        // let's check the cert
        try {
            theCert.checkValidity();
        } catch (CertificateException e) {
            // print it to System.out even if debug is not enabled, but do keep going
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.exCheckValidity",myCert), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
        return theCert;
    }

    void addCertificate(KeyStore masterStoreX509Trusts, String myCert, String myAlias) throws GeneralSecurityException, IOException {
        final String METHOD_NAME = "addCertificate"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        try {
            X509Certificate certToAdd = readCert(myCert);
            // add it to the master keyStore of certs
            String newAlias = myAlias;
            // avoid name collisions
            Random random = new Random();
            StringBuffer buf = new StringBuffer(newAlias);
            while (masterStoreX509Trusts.containsAlias(newAlias)) {
                buf.append(random.nextInt()); // newAlias = newAlias + random.nextInt();
                newAlias = buf.toString();
            }            
            masterStoreX509Trusts.setCertificateEntry(newAlias, certToAdd);
            // check that it worked, but it is not a fatal error if it didn't
            if (!masterStoreX509Trusts.isCertificateEntry(newAlias)) {
                logger.logp(Level.INFO, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.certFail",newAlias)); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
                logger.logp(Level.FINER, CLASS_NAME, METHOD_NAME, "Embedded " + newAlias + " certificate"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } catch (GeneralSecurityException e) {
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.exAddCert"), e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }

    void addPrivateKey(KeyStore myStore, String myKey, String myAlias, String myCert) throws GeneralSecurityException, IOException {
        final String METHOD_NAME = "addPrivateKey"; //$NON-NLS-1$
        logger.entering(CLASS_NAME, METHOD_NAME);
        try {
            BufferedReader br = new BufferedReader(new StringReader(myKey));
            String line;
            StringBuffer keyBuf = new StringBuffer();
            while ((line = br.readLine()) != null) {
                if (!(line.startsWith("-----BEGIN") || line.startsWith("-----END"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    keyBuf.append (line);
                    keyBuf.append("\n"); //$NON-NLS-1$
                }
            }
            br.close();


            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(Utils.decodeBase64(keyBuf.toString().getBytes()));

            KeyFactory keyFactory = KeyFactory.getInstance("DSA"); //$NON-NLS-1$
            PrivateKey privKey = keyFactory.generatePrivate(privKeySpec);

            String newAlias = myAlias;
            // avoid name collisions
            Random random = new Random();
            StringBuffer buf = new StringBuffer(newAlias);
            while (myStore.containsAlias(buf.toString())) {
            	buf.append(random.nextInt()); //newAlias = newAlias + random.nextInt();
            }
            newAlias = buf.toString();
            X509Certificate serverCert = readCert(myCert);   // cert chain

            java.security.cert.Certificate[] chain = {serverCert};
            myStore.setKeyEntry(newAlias, privKey, "WAMTV2".toCharArray(), chain); //$NON-NLS-1$
        } catch (GeneralSecurityException e) {
            logger.logp(Level.SEVERE, CLASS_NAME, METHOD_NAME, Messages.getString("wamt.amp.defaultProvider.SSLContextCache.exAddPrivKey"), e); //$NON-NLS-1$
        }
        logger.exiting(CLASS_NAME, METHOD_NAME);
    }
}




