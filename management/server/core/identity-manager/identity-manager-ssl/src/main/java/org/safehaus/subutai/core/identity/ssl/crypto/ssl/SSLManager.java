package org.safehaus.subutai.core.identity.ssl.crypto.ssl;


import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.safehaus.subutai.core.identity.ssl.crypto.keystore.KeyStoreData;


public class SSLManager
{
    private KeyStore keyStore = null;
    private KeyStore trustStore = null;
    ;
    private KeyStoreData keyStoreData = null;
    private KeyStoreData trustStoreData = null;


    public SSLManager( KeyStore keyStore, KeyStoreData keyStoreData, KeyStore trustStore, KeyStoreData trustStoreData )
    {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.keyStoreData = keyStoreData;
        this.trustStoreData = trustStoreData;
    }


    public KeyManager[] getClientKeyManagers()
    {

        KeyManager[] keyManagers = null;
        KeyManagerFactory keyManagerFactory = null;

        try
        {
            keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
            keyManagerFactory.init( keyStore, keyStoreData.getPassword().toCharArray() );
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return keyManagers;
    }


    public TrustManager[] getClientTrustManagers()
    {
        TrustManager[] trustManagers = null;
        TrustManagerFactory trustManagerFactory = null;

        try
        {
            trustManagerFactory = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
            trustManagerFactory.init( trustStore );
            trustManagers = trustManagerFactory.getTrustManagers();
            trustStoreData.getPassword();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return trustManagers;
    }


    public TrustManager[] getClientFullTrustManagers()
    {
        return new TrustManager[] { new NaiveTrustManager() };
    }
}
