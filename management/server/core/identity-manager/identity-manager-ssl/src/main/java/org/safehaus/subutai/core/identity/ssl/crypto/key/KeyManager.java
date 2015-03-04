package org.safehaus.subutai.core.identity.ssl.crypto.key;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class KeyManager
{

    private KeyPairGenerator keyPairGen;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyGenerator keyGen;
    private SecretKey secretKey;
    private Signature signature;


    /**
     * ************************************************************************* **********************************
     */
    public KeyPairGenerator prepareKeyPairGeneration( KeyPairType keyPairType, int keySize )
    {

        try
        {
            keyPairGen = KeyPairGenerator.getInstance( keyPairType.jce() );
            keyPairGen.initialize( keySize );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return keyPairGen;
    }


    /**
     * ************************************************************************* **********************************
     */
    public KeyPair generateKeyPair( KeyPairGenerator keyPairGen )
    {
        try
        {
            keyPair = keyPairGen.genKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return keyPair;
    }


    /**
     * ************************************************************************* **********************************
     */
    public PrivateKey generatePrivateKeyPair( KeyPair keyPair )
    {
        try
        {
            privateKey = keyPair.getPrivate();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return privateKey;
    }


    /**
     * ************************************************************************* **********************************
     */
    public PublicKey generatePublicKeyPair( KeyPair keyPair )
    {
        try
        {
            publicKey = keyPair.getPublic();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return publicKey;
    }


    /**
     * ************************************************************************* **********************************
     */
    public byte[] generatePrivateKeyPairBytes( KeyPair keyPair )
    {
        byte[] privateKeyBytes = null;

        try
        {
            privateKeyBytes = privateKey.getEncoded();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return privateKeyBytes;
    }


    /**
     * ************************************************************************* **********************************
     */
    public byte[] generatePublicKeyPairBytes( KeyPair keyPair )
    {
        byte[] publicKeyBytes = null;

        try
        {
            publicKeyBytes = publicKey.getEncoded();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return publicKeyBytes;
    }


    /**
     * ************************************************************************* **********************************
     */
    public void prepareSecretKeyGeneration( SecretKeyType secretKeyType )
    {
        try
        {
            keyGen = KeyGenerator.getInstance( secretKeyType.jce() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            e.printStackTrace();
        }
    }


    /**
     * ************************************************************************* **********************************
     */
    public SecretKey generateSecretKey( KeyGenerator keyGen )
    {
        try
        {
            this.keyGen = keyGen;
            secretKey = this.keyGen.generateKey();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return secretKey;
    }


    /**
     * ************************************************************************* **********************************
     */
    public byte[] signData( SignatureType signatureType, KeyPair keyPair, String fileName )
    {
        byte[] signedData = null;

        try
        {
            signature = Signature.getInstance( signatureType.jce() );
            signature.initSign( keyPair.getPrivate() );

            //*************Read data to be signed ****************************

            FileInputStream fis = new FileInputStream( fileName );
            BufferedInputStream bufin = new BufferedInputStream( fis );

            byte[] buffer = new byte[1024];
            int len;

            while ( ( len = bufin.read( buffer ) ) >= 0 )
            {
                signature.update( buffer, 0, len );
            }

            bufin.close();

            //*****************************************************************

            signedData = signature.sign();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return signedData;
    }


    /**
     * ************************************************************************* **********************************
     */
    public boolean verfifySignedData( SignatureType signatureType, KeyPair keyPair, String fileName, byte[] sign )
    {
        boolean verified = false;

        try
        {
            signature = Signature.getInstance( signatureType.jce() );
            signature.initSign( keyPair.getPrivate() );

            //*************Read data to be signed ****************************

            FileInputStream fis = new FileInputStream( fileName );
            BufferedInputStream bufin = new BufferedInputStream( fis );

            byte[] buffer = new byte[1024];
            int len;

            while ( ( len = bufin.read( buffer ) ) >= 0 )
            {
                signature.update( buffer, 0, len );
            }

            bufin.close();

            //*****************************************************************

            verified = signature.verify( sign );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return verified;
    }


    /**
     * ** Getter Setter Methods ***********************************************************************
     * **********************************
     */
    public KeyPairGenerator getKeyGen()
    {
        return keyPairGen;
    }


    public void setKeyGen( KeyPairGenerator keyPairGen )
    {
        this.keyPairGen = keyPairGen;
    }


    public KeyPair getKeypair()
    {
        return keyPair;
    }


    public void setKeypair( KeyPair keypair )
    {
        this.keyPair = keypair;
    }


    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }


    public void setPrivateKey( PrivateKey privateKey )
    {
        this.privateKey = privateKey;
    }


    public PublicKey getPublicKey()
    {
        return publicKey;
    }


    public void setPublicKey( PublicKey publicKey )
    {
        this.publicKey = publicKey;
    }


    public KeyPairGenerator getKeyPairGen()
    {
        return keyPairGen;
    }


    public void setKeyPairGen( KeyPairGenerator keyPairGen )
    {
        this.keyPairGen = keyPairGen;
    }


    public KeyPair getKeyPair()
    {
        return keyPair;
    }


    public void setKeyPair( KeyPair keyPair )
    {
        this.keyPair = keyPair;
    }


    public SecretKey getSecretKey()
    {
        return secretKey;
    }


    public void setSecretKey( SecretKey secretKey )
    {
        this.secretKey = secretKey;
    }


    public void setKeyGen( KeyGenerator keyGen )
    {
        this.keyGen = keyGen;
    }


    public Signature getSignature()
    {
        return signature;
    }


    public void setSignature( Signature signature )
    {
        this.signature = signature;
    }
}
