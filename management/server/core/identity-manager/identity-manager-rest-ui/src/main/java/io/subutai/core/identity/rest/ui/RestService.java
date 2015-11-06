package io.subutai.core.identity.rest.ui;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface RestService
{
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getUsers();

    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response setUser( @FormParam( "username" ) String username,
                             @FormParam( "full_name" ) String fullName,
                             @FormParam( "password" ) String password,
                             @FormParam( "email" ) String email,
                             @FormParam( "roles" ) String roles,
                             @FormParam( "user_id" ) Long userId );

    @DELETE
    @Path( "/{userId}" )
    public Response deleteUser( @PathParam( "userId" ) Long userId );

    @GET
    @Path( "roles" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getRoles();

    @POST
    @Path( "roles" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response saveRole( @FormParam( "rolename" ) String rolename,
                             @FormParam( "modules" ) String modulesJson,
                             @FormParam( "endpoint" ) String endpointJson,
                             @FormParam( "cli_commands" ) String cliCommandsJson );

    @DELETE
    @Path( "roles/{roleId}" )
    public Response deleteRole( @PathParam( "roleId" ) Long roleName );

    @GET
    @Path( "permissions" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getPermissions();
}