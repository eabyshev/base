package io.subutai.core.peer.impl.entity;


import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.subutai.common.host.Interface;


@Entity
@Table( name = "interface" )
@Access( AccessType.FIELD )
@XmlRootElement
public class HostInterface implements Interface, Serializable
{
    @Id
    @JsonIgnore
    private String id;

    @Column( name = "name", nullable = false )
    private String interfaceName;

    @Column( name = "ip", nullable = false )
    private String ip;
    @Column( name = "mac", nullable = false )
    private String mac;

    @ManyToOne
    @JoinColumn( name = "host_id" )
    @JsonIgnore
    private AbstractSubutaiHost host;


    protected HostInterface()
    {
    }


    public HostInterface( final Interface s )
    {
        this.id = s.getMac()+"-"+s.getIp();
        this.interfaceName = s.getInterfaceName();
        this.ip = s.getIp().replace( "addr:", "" );
        this.mac = s.getMac();
    }


    public String getId()
    {
        return id;
    }


    public void setId( final String id )
    {
        this.id = id;
    }


    @Override
    public String getInterfaceName()
    {
        return interfaceName;
    }


    public void setInterfaceName( final String interfaceName )
    {
        this.interfaceName = interfaceName;
    }


    @Override
    public String getIp()
    {
        return ip;
    }


    public void setIp( final String ip )
    {
        this.ip = ip;
    }


    @Override
    public String getMac()
    {
        return mac;
    }


    public void setMac( final String mac )
    {
        this.mac = mac;
    }


    public AbstractSubutaiHost getHost()
    {
        return host;
    }


    public void setHost( final AbstractSubutaiHost host )
    {
        this.host = host;
    }
}

