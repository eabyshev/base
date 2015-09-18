package io.subutai.core.registration.rest.transitional;


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import io.subutai.common.host.HostArchitecture;
import io.subutai.common.host.HostInfo;
import io.subutai.common.host.Interface;
import io.subutai.common.peer.InterfaceModel;
import io.subutai.core.registration.api.service.ContainerInfo;


/**
 * Created by talas on 9/15/15.
 */
public class ContainerInfoJson implements ContainerInfo
{
    private String id;
    private String hostname;
    private Integer vlan;
    private String templateName;
    private Set<InterfaceModel> netInterfaces = new HashSet<>();
    private HostArchitecture arch;
    private String publicKey;


    public ContainerInfoJson()
    {
    }


    public ContainerInfoJson( ContainerInfo hostInfo )
    {
        this.id = hostInfo.getId().toString();
        this.hostname = hostInfo.getHostname();
        this.templateName = hostInfo.getTemplateName();
        this.vlan = hostInfo.getVlan();
        this.arch = hostInfo.getArch();
        this.publicKey = hostInfo.getPublicKey();
        if ( arch == null )
        {
            arch = HostArchitecture.AMD64;
        }
        for ( Interface anInterface : hostInfo.getInterfaces() )
        {
            this.netInterfaces.add( new InterfaceModel( anInterface ) );
        }
    }


    @Override
    public UUID getId()
    {
        return UUID.fromString( id );
    }


    @Override
    public String getHostname()
    {
        return hostname;
    }


    @Override
    public Set<Interface> getInterfaces()
    {
        Set<Interface> result = Sets.newHashSet();
        result.addAll( this.netInterfaces );
        return result;
    }


    @Override
    public HostArchitecture getArch()
    {
        return arch;
    }


    @Override
    public String getTemplateName()
    {
        return templateName;
    }


    public void setTemplateName( final String templateName )
    {
        this.templateName = templateName;
    }


    @Override
    public Integer getVlan()
    {
        return vlan;
    }


    public void setVlan( final Integer vlan )
    {
        this.vlan = vlan;
    }


    @Override
    public String getPublicKey()
    {
        return publicKey;
    }


    public void setPublicKey( final String publicKey )
    {
        this.publicKey = publicKey;
    }


    @Override
    public int compareTo( final HostInfo o )
    {
        return hostname.compareTo( o.getHostname() );
    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ContainerInfoJson ) )
        {
            return false;
        }

        final ContainerInfoJson that = ( ContainerInfoJson ) o;

        return arch == that.arch && hostname.equals( that.hostname ) && id.equals( that.id )
                && netInterfaces.equals( that.netInterfaces );
    }


    @Override
    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + hostname.hashCode();
        result = 31 * result + netInterfaces.hashCode();
        result = 31 * result + arch.hashCode();
        return result;
    }


    @Override
    public String toString()
    {
        return "ContainerHostInfoModel{" +
                "id='" + id + '\'' +
                ", hostname='" + hostname + '\'' +
                ", netInterfaces=" + netInterfaces +
                ", arch=" + arch +
                '}';
    }
}
