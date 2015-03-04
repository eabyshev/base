package org.safehaus.subutai.core.identity.ssl;


import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.safehaus.subutai.core.identity.ssl.crypto.keystore.KeyStoreData;
import org.safehaus.subutai.core.identity.ssl.crypto.keystore.KeyStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.validator.KeyStores;


/**
 * Created by talas on 3/3/15.
 */
public class CustomTrustManager implements X509TrustManager
{

    private static final Logger log = LoggerFactory.getLogger( CustomTrustManager.class );

    private KeyStoreManager trustStoreManager;
    private KeyStoreData trustStoreData;
    private String trustStorePath;
    private String trustStorePassword;


    public CustomTrustManager( final String trustStorePath, final String trustStorePassword )
    {
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
        this.trustStoreManager = new KeyStoreManager();
        this.trustStoreData = new KeyStoreData();
        this.trustStoreData.setupTrustStorePx2();
    }


    @Override
    public void checkClientTrusted( final X509Certificate[] chain, final String authType ) throws CertificateException
    {
        log.warn( "##############   checkClientTrusted" );
    }


    @Override
    public void checkServerTrusted( final X509Certificate[] chain, final String authType ) throws CertificateException
    {
        log.warn( "#################   checkServerTrusted" );
    }


    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        log.warn( "##################   checkServerTrusted" );
        KeyStore trustStore = trustStoreManager.load( trustStoreData );
        X509Certificate trustCertificates[] = new X509Certificate[KeyStores.getTrustedCerts( trustStore ).size()];
        return KeyStores.getTrustedCerts( trustStore ).toArray( trustCertificates );
        //        return new X509Certificate[0];
    }
}
