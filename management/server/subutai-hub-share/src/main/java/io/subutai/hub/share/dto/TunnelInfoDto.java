package io.subutai.hub.share.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;


public class TunnelInfoDto
{

    public enum TunnelStatus
    {
        PENDING,
        READY,
        ERROR,
        DELETE
    }


    private String ip;
    private String portToOpen;
    private long ttl;

    private TunnelStatus tunnelStatus;

    private String openedPort;
    private String openedIp;

    @JsonIgnore
    private String errorLogs;


    public TunnelInfoDto()
    {
    }


    public TunnelInfoDto( String ip, long ttl, String portToOpen, TunnelStatus tunnelStatus )
    {
        this.ip = ip;
        this.ttl = ttl;
        this.portToOpen = portToOpen;
        this.tunnelStatus = tunnelStatus;
    }


    public String getIp()
    {
        return ip;
    }


    public void setIp( final String ip )
    {
        this.ip = ip;
    }


    public String getPortToOpen()
    {
        return portToOpen;
    }


    public void setPortToOpen( final String portToOpen )
    {
        this.portToOpen = portToOpen;
    }


    public String getOpenedPort()
    {
        return openedPort;
    }


    public void setOpenedPort( final String openedPort )
    {
        this.openedPort = openedPort;
    }


    public String getOpenedIp()
    {
        return openedIp;
    }


    public void setOpenedIp( final String openedIp )
    {
        this.openedIp = openedIp;
    }


    public Long getTtl()
    {
        return ttl;
    }


    public void setTtl( final Long ttl )
    {
        this.ttl = ttl;
    }


    public TunnelStatus getTunnelStatus()
    {
        return tunnelStatus;
    }


    public void setTunnelStatus( final TunnelStatus tunnelStatus )
    {
        this.tunnelStatus = tunnelStatus;
    }


    public String getErrorLogs()
    {
        return errorLogs;
    }


    public void setErrorLogs( final String errorLogs )
    {
        this.errorLogs = errorLogs;
    }
}
