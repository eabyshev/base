package io.subutai.core.systemmanager.rest;


import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.configuration.ConfigurationException;

import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerPolicy;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.core.systemmanager.api.SystemManager;
import io.subutai.core.systemmanager.api.pojo.AdvancedSettings;
import io.subutai.core.systemmanager.api.pojo.NetworkSettings;
import io.subutai.core.systemmanager.api.pojo.PeerSettings;
import io.subutai.core.systemmanager.api.pojo.SystemInfo;


public class RestServiceImpl implements RestService
{
    private static final Logger LOG = LoggerFactory.getLogger( RestServiceImpl.class.getName() );

    private SystemManager systemManager;
    private PeerManager peerManager;


    @Override
    public Response getSubutaiInfo()
    {
        try
        {
            SystemInfo pojo = systemManager.getSystemInfo();
            String projectInfo = JsonUtil.GSON.toJson( pojo );

            return Response.status( Response.Status.OK ).entity( projectInfo ).build();
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage() );
            e.printStackTrace();
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }
    }


    @Override
    public Response setPeerSettings()
    {
        systemManager.setPeerSettings();
        return Response.status( Response.Status.OK ).build();
    }


    @Override
    public Response getPeerSettings()
    {
        PeerSettings pojo = systemManager.getPeerSettings();
        String peerSettingsInfo = JsonUtil.GSON.toJson( pojo );

        return Response.status( Response.Status.OK ).entity( peerSettingsInfo ).build();
    }


    @Override
    public Response getPeerPolicy()
    {
        PeerPolicy peerPolicy = peerManager.getPolicy( peerManager.getLocalPeer().getId() );

        return Response.status( Response.Status.OK ).entity( peerPolicy ).build();
    }


    @Override
    public Response setPeerPolicy( final String peerId, final String diskUsageLimit, final String cpuUsageLimit,
                                   final String memoryUsageLimit, final String environmentLimit,
                                   final String containerLimit )
    {
        PeerPolicy peerPolicy =
                new PeerPolicy( peerId, Integer.parseInt( diskUsageLimit ), Integer.parseInt( cpuUsageLimit ),
                        Integer.parseInt( memoryUsageLimit ), 90, Integer.parseInt( environmentLimit ),
                        Integer.parseInt( containerLimit ) );
        try
        {
            peerManager.setPolicy( peerId, peerPolicy );
        }
        catch ( PeerException e )
        {
            Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
            e.printStackTrace();
        }
        return Response.status( Response.Status.OK ).build();
    }


    @Override
    public Response getNetworkSettings()
    {
        try
        {
            NetworkSettings pojo = systemManager.getNetworkSettings();
            String networkSettingsInfo = JsonUtil.GSON.toJson( pojo );

            return Response.status( Response.Status.OK ).entity( networkSettingsInfo ).build();
        }
        catch ( ConfigurationException e )
        {
            LOG.error( e.getMessage() );
            e.printStackTrace();
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }
    }


    @Override
    public Response setNetworkSettings( final String publicUrl, final String publicSecurePort, final String startRange,
                                        final String endRange )
    {
        try
        {
            systemManager.setNetworkSettings( publicUrl, publicSecurePort, startRange, endRange );
        }
        catch ( ConfigurationException e )
        {
            LOG.error( e.getMessage() );
            e.printStackTrace();
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( e.getMessage() ).build();
        }

        return Response.status( Response.Status.OK ).build();
    }


    @Override
    public Response getAdvancedSettings()
    {
        AdvancedSettings pojo = systemManager.getAdvancedSettings();
        String advancedSettingsInfo = JsonUtil.GSON.toJson( pojo );

        return Response.status( Response.Status.OK ).entity( advancedSettingsInfo ).build();
    }


    @Override
    public Response getManagementUpdates()
    {
        SystemInfo pojo = systemManager.getManagementUpdates();
        String subutaiInfo = JsonUtil.GSON.toJson( pojo );

        return Response.status( Response.Status.OK ).entity( subutaiInfo ).build();
    }


    @Override
    public Response update()
    {
        boolean isSuccessful = systemManager.updateManagement();

        if ( isSuccessful )
        {
            return Response.status( Response.Status.OK ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
        }
    }


    public void setSystemManager( final SystemManager systemManager )
    {
        this.systemManager = systemManager;
    }


    public void setPeerManager( final PeerManager peerManager )
    {
        this.peerManager = peerManager;
    }
}
