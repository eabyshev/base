package io.subutai.common.protocol;


import java.util.Set;

import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.subutai.common.environment.RhP2pIp;


/**
 * P2P config
 */
public class P2PConfig
{
    @JsonProperty( "peerId" )
    private String peerId;
    @JsonProperty( "hash" )
    private String hash;
    @JsonProperty( "address" )
    private String address;
    @JsonProperty( "secretKey" )
    private String secretKey;
    @JsonProperty( "environmentId" )
    private String environmentId;
    @JsonProperty( "secretKeyTtlSec" )
    private long secretKeyTtlSec;
    @JsonProperty( "rhP2pIps" )
    private Set<RhP2pIp> rhP2pIps = Sets.newHashSet();


    @Deprecated
    public P2PConfig( @JsonProperty( "peerId" ) final String peerId,
                      @JsonProperty( "environmentId" ) final String environmentId,
                      @JsonProperty( "hash" ) final String hash, @JsonProperty( "address" ) final String address,
                      @JsonProperty( "secretKey" ) final String secretKey,
                      @JsonProperty( "secretKeyTtlSec" ) final long secretKeyTtlSec )
    {
        this.peerId = peerId;
        this.environmentId = environmentId;
        this.hash = hash;
        this.address = address;
        this.secretKey = secretKey;
        this.secretKeyTtlSec = secretKeyTtlSec;
    }


    public P2PConfig( @JsonProperty( "peerId" ) final String peerId,
                      @JsonProperty( "environmentId" ) final String environmentId,
                      @JsonProperty( "hash" ) final String hash, @JsonProperty( "secretKey" ) final String secretKey,
                      @JsonProperty( "secretKeyTtlSec" ) final long secretKeyTtlSec )
    {
        this.peerId = peerId;
        this.environmentId = environmentId;
        this.hash = hash;
        this.secretKey = secretKey;
        this.secretKeyTtlSec = secretKeyTtlSec;
    }


    public void addRhP2pIp( RhP2pIp rhP2pIp )
    {
        Preconditions.checkNotNull( rhP2pIp );

        rhP2pIps.add( rhP2pIp );
    }


    public Set<RhP2pIp> getRhP2pIps()
    {
        return rhP2pIps;
    }


    public long getSecretKeyTtlSec()
    {
        return secretKeyTtlSec;
    }


    public String getPeerId()
    {
        return peerId;
    }


    public void setPeerId( final String peerId )
    {
        this.peerId = peerId;
    }


    public String getHash()
    {
        return hash;
    }


    public String getAddress()
    {
        return address;
    }


    public void setAddress( final String address )
    {
        this.address = address;
    }


    public String getSecretKey()
    {
        return secretKey;
    }


    public String getEnvironmentId()
    {
        return environmentId;
    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final P2PConfig p2PConfig = ( P2PConfig ) o;

        if ( hash != null ? !hash.equals( p2PConfig.hash ) : p2PConfig.hash != null )
        {
            return false;
        }
        if ( peerId != null ? !peerId.equals( p2PConfig.peerId ) : p2PConfig.peerId != null )
        {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode()
    {
        int result = peerId != null ? peerId.hashCode() : 0;
        result = 31 * result + ( hash != null ? hash.hashCode() : 0 );
        return result;
    }
}
