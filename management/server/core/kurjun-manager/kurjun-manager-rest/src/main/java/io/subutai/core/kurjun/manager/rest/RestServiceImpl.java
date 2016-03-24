package io.subutai.core.kurjun.manager.rest;


import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

import io.subutai.common.util.JsonUtil;
import io.subutai.core.kurjun.manager.api.KurjunManager;


public class RestServiceImpl implements RestService
{
    private KurjunManager kurjunManager;


    @Override
    public Response getAuthId()
    {
        return Response.status( Response.Status.OK ).entity( "35492d26-f1a5-11e5-9ce9-5e5517507c66" ).build();
    }


    @Override
    public Response getPublicKey( final String publicKey )
    {
        return null;
    }


    @Override
    public Response getSignedMessage( final String signedMsg )
    {


        return null;
    }


    public void setKurjunManager( final KurjunManager kurjunManager )
    {
        this.kurjunManager = kurjunManager;
    }
}
