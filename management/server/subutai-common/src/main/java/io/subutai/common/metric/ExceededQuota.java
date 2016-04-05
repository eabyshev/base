package io.subutai.common.metric;


import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.host.HostId;
import io.subutai.common.quota.ContainerCpuResource;
import io.subutai.common.quota.ContainerHomeResource;
import io.subutai.common.quota.ContainerOptResource;
import io.subutai.common.quota.ContainerRamResource;
import io.subutai.common.quota.ContainerResource;
import io.subutai.common.quota.ContainerRootfsResource;
import io.subutai.common.quota.ContainerVarResource;
import io.subutai.common.resource.ByteValueResource;
import io.subutai.common.resource.ContainerResourceType;
import io.subutai.common.resource.NumericValueResource;
import io.subutai.common.resource.ResourceValue;


/**
 * Exceeded quota class
 */
public class ExceededQuota
{
    private static final Logger LOG = LoggerFactory.getLogger( ExceededQuota.class );

    @JsonProperty( "hostId" )
    protected final HostId hostId;
    @JsonProperty( "resourceType" )
    protected final ContainerResourceType containerResourceType;
    @JsonProperty( "currentValue" )
    protected final ResourceValue currentValue;
    @JsonProperty( "quotaValue" )
    protected final ResourceValue quotaValue;


    public ExceededQuota( @JsonProperty( "hostId" ) final HostId hostId,
                          @JsonProperty( "resourceType" ) final ContainerResourceType containerResourceType,
                          @JsonProperty( "currentValue" ) final ResourceValue currentValue,
                          @JsonProperty( "quotaValue" ) final ResourceValue quotaValue )
    {
        this.hostId = hostId;
        this.containerResourceType = containerResourceType;
        this.currentValue = currentValue;
        this.quotaValue = quotaValue;
    }


    public HostId getHostId()
    {
        return hostId;
    }


    public ContainerResourceType getContainerResourceType()
    {
        return containerResourceType;
    }


    public <T extends ContainerResource> T getContainerResource( final Class<T> format )
    {
        ContainerResource result = null;
        try
        {
            switch ( containerResourceType )
            {
                case CPU:
                    result = new ContainerCpuResource( ( NumericValueResource ) quotaValue );
                    break;
                case RAM:
                    result = new ContainerRamResource( ( ByteValueResource ) quotaValue );
                    break;
                case ROOTFS:
                    result = new ContainerRootfsResource( ( ByteValueResource ) quotaValue );
                    break;
                case HOME:
                    result = new ContainerHomeResource( ( ByteValueResource ) quotaValue );
                    break;
                case OPT:
                    result = new ContainerOptResource( ( ByteValueResource ) quotaValue );
                    break;
                case VAR:
                    result = new ContainerVarResource( ( ByteValueResource ) quotaValue );
                    break;
            }

            if ( result != null )
            {
                return ( T ) result;
            }
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage() );
        }

        return null;
    }


    public double getPercentage()
    {
        return ( ( ByteValueResource ) currentValue.getValue() ).doubleValue();
    }


    @SuppressWarnings( "unchecked" )
    public <T> T getQuotaValue( final Class<T> format )
    {
        try
        {
            return ( T ) quotaValue;
        }
        catch ( ClassCastException cce )
        {
            return null;
        }
    }


    @SuppressWarnings( "unchecked" )
    public <T> T getCurrentValue( final Class<T> format )
    {
        try
        {
            return ( T ) currentValue;
        }
        catch ( ClassCastException cce )
        {
            return null;
        }
    }


    @Deprecated
    public ResourceValue getQuotaValue()
    {
        return quotaValue;
    }


    @Deprecated
    public ResourceValue getCurrentValue()
    {
        return currentValue;
    }


    @JsonIgnore
    public String getDescription()
    {
        return String.format( "%s/%s", currentValue, quotaValue );
    }


    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer( "ResourceAlert{" );
        sb.append( "hostId=" ).append( hostId );
        sb.append( ", resourceType=" ).append( containerResourceType );
        sb.append( ", currentValue=" ).append( currentValue );
        sb.append( ", quotaValue=" ).append( quotaValue );
        sb.append( '}' );
        return sb.toString();
    }
}
