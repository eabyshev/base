package io.subutai.core.peer.impl;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.subutai.common.dao.DaoManager;
import io.subutai.common.exception.NetworkException;
import io.subutai.common.host.HostInterface;
import io.subutai.common.host.NullHostInterface;
import io.subutai.common.network.SocketUtil;
import io.subutai.common.peer.Encrypted;
import io.subutai.common.peer.HostNotFoundException;
import io.subutai.common.peer.LocalPeer;
import io.subutai.common.peer.Peer;
import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerId;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.peer.PeerNotRegisteredException;
import io.subutai.common.peer.PeerPolicy;
import io.subutai.common.peer.RegistrationData;
import io.subutai.common.peer.RegistrationStatus;
import io.subutai.common.peer.RemotePeer;
import io.subutai.common.resource.PeerGroupResources;
import io.subutai.common.resource.PeerResources;
import io.subutai.common.security.crypto.pgp.PGPKeyUtil;
import io.subutai.common.security.objects.TokenType;
import io.subutai.common.security.relation.RelationManager;
import io.subutai.common.security.relation.model.Relation;
import io.subutai.common.security.relation.model.RelationInfoMeta;
import io.subutai.common.security.relation.model.RelationMeta;
import io.subutai.common.security.relation.model.RelationStatus;
import io.subutai.common.settings.Common;
import io.subutai.common.util.SecurityUtilities;
import io.subutai.core.identity.api.IdentityManager;
import io.subutai.core.identity.api.model.User;
import io.subutai.core.identity.api.model.UserToken;
import io.subutai.core.messenger.api.Messenger;
import io.subutai.core.peer.api.PeerAction;
import io.subutai.core.peer.api.PeerActionListener;
import io.subutai.core.peer.api.PeerActionResponse;
import io.subutai.core.peer.api.PeerActionType;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.core.peer.api.RegistrationClient;
import io.subutai.core.peer.impl.command.CommandResponseListener;
import io.subutai.core.peer.impl.dao.PeerDataService;
import io.subutai.core.peer.impl.entity.PeerData;
import io.subutai.core.peer.impl.request.MessageResponseListener;
import io.subutai.core.security.api.SecurityManager;


/**
 * PeerManager implementation
 */
@PermitAll
public class PeerManagerImpl implements PeerManager
{
    private static final Logger LOG = LoggerFactory.getLogger( PeerManagerImpl.class );
    final int MAX_CONTAINER_LIMIT = 20;
    final int MAX_ENVIRONMENT_LIMIT = 20;
    protected PeerDataService peerDataService;
    private final LocalPeer localPeer;
    protected Messenger messenger;
    protected CommandResponseListener commandResponseListener;
    private MessageResponseListener messageResponseListener;
    private DaoManager daoManager;
    private SecurityManager securityManager;
    private Object provider;
    private Map<String, RegistrationData> registrationRequests = new ConcurrentHashMap<>();
    private List<PeerActionListener> peerActionListeners = new CopyOnWriteArrayList<>();
    private IdentityManager identityManager;
    private Map<String, Peer> peers = new ConcurrentHashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private String localPeerId;
    private RegistrationClient registrationClient;
    protected ScheduledExecutorService localIpSetter;
    private RelationManager relationManager;


    public PeerManagerImpl( final Messenger messenger, LocalPeer localPeer, DaoManager daoManager,
                            MessageResponseListener messageResponseListener, SecurityManager securityManager,
                            IdentityManager identityManager, Object provider )
    {
        Preconditions.checkNotNull( messenger );
        Preconditions.checkNotNull( localPeer );
        Preconditions.checkNotNull( daoManager );
        Preconditions.checkNotNull( messageResponseListener );
        Preconditions.checkNotNull( securityManager );
        Preconditions.checkNotNull( identityManager );
        Preconditions.checkNotNull( provider );

        this.messenger = messenger;
        this.localPeer = localPeer;
        this.daoManager = daoManager;
        this.messageResponseListener = messageResponseListener;
        this.securityManager = securityManager;
        this.identityManager = identityManager;
        this.provider = provider;
        commandResponseListener = new CommandResponseListener();
        localPeer.addRequestListener( commandResponseListener );
        registrationClient = new RegistrationClientImpl( provider );
    }


    public void init()
    {
        try
        {
            this.peerDataService = new PeerDataService( daoManager.getEntityManagerFactory() );

            localPeerId = securityManager.getKeyManager().getPeerId();

            PeerData localPeerData = peerDataService.find( localPeerId );

            if ( localPeerData == null )
            {
                PeerInfo localPeerInfo = localPeer.getPeerInfo();

                PeerPolicy policy = getDefaultPeerPolicy( localPeerId );

                PeerData peerData =
                        new PeerData( localPeerInfo.getId(), toJson( localPeerInfo ), "", toJson( policy ), 1 );

                updatePeerData( peerData );
            }

            for ( PeerData peerData : this.peerDataService.getAll() )
            {
                Peer peer = constructPeerPojo( peerData );
                updatePeerInCache( peer );
            }

            localIpSetter = Executors.newSingleThreadScheduledExecutor();
            localIpSetter.scheduleWithFixedDelay( new IpDetectionTask(), 1, 5, TimeUnit.SECONDS );
        }
        catch ( Exception e )
        {
            LOG.error( "Could not initialize peer manager", e );
        }
    }


    public void destroy()
    {
        commandResponseListener.dispose();
    }


    public void setRelationManager( final RelationManager relationManager )
    {
        this.relationManager = relationManager;
    }


    public RelationManager getRelationManager()
    {
        return relationManager;
    }


    public IdentityManager getIdentityManager()
    {
        return identityManager;
    }


    public PeerPolicy getDefaultPeerPolicy( String peerId )
    {
        //TODO: make values configurable
        return new PeerPolicy( peerId, 90, 50, 90, 90, 3, 10 );
    }


    @Override
    public void registerPeerActionListener( PeerActionListener peerActionListener )
    {
        if ( peerActionListener != null )
        {
            LOG.info( "Registering peer action listener: " + peerActionListener.getName() );
            this.peerActionListeners.add( peerActionListener );
        }
    }


    @Override
    public void unregisterPeerActionListener( PeerActionListener peerActionListener )
    {
        if ( peerActionListener != null )
        {
            LOG.info( "Unregistering peer action listener: " + peerActionListener.getName() );
            this.peerActionListeners.remove( peerActionListener );
        }
    }


    private PeerActionResponses notifyPeerActionListeners( PeerAction action )
    {
        PeerActionResponses result = new PeerActionResponses();
        for ( PeerActionListener peerActionListener : peerActionListeners )
        {
            PeerActionResponse response = peerActionListener.onPeerAction( action );
            result.add( response );
        }
        return result;
    }


    private void register( final String keyPhrase, final RegistrationData registrationData ) throws PeerException
    {
        Preconditions.checkNotNull( keyPhrase, "Key phrase could not be null." );
        Preconditions.checkArgument( !keyPhrase.isEmpty(), "Key phrase could not be empty" );

        if ( !notifyPeerActionListeners( new PeerAction( PeerActionType.REGISTER ) ).succeeded() )
        {
            throw new PeerException( "Could not register peer." );
        }

        Encrypted encryptedSslCert = registrationData.getSslCert();
        try
        {
            SocketUtil.check( registrationData.getPeerInfo().getIp(), 3,
                    registrationData.getPeerInfo().getPublicSecurePort() );
            byte[] key = SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) );
            String decryptedSslCert = encryptedSslCert.decrypt( key, String.class );
            securityManager.getKeyStoreManager().importCertAsTrusted( Common.DEFAULT_PUBLIC_SECURE_PORT,
                    registrationData.getPeerInfo().getId(), decryptedSslCert );
            securityManager.getHttpContextManager().reloadKeyStore();

            PeerPolicy policy = getDefaultPeerPolicy( registrationData.getPeerInfo().getId() );

            final Integer order = getMaxOrder() + 1;

            registrationData.getPeerInfo()
                            .setName( String.format( "Peer on %s", registrationData.getPeerInfo().getIp() ) );

            PeerData peerData =
                    new PeerData( registrationData.getPeerInfo().getId(), toJson( registrationData.getPeerInfo() ),
                            keyPhrase, toJson( policy ), order );

            updatePeerData( peerData );

            Peer newPeer = constructPeerPojo( peerData );

            updatePeerInCache( newPeer );

            Encrypted encryptedPublicKey = registrationData.getPublicKey();
            String publicKey = encryptedPublicKey.decrypt( key, String.class );
            securityManager.getKeyManager()
                           .savePublicKeyRing( registrationData.getPeerInfo().getId(), ( short ) 3, publicKey );
        }
        catch ( GeneralSecurityException e )
        {
            throw new PeerException( "Invalid keyphrase or general security exception." );
        }
        catch ( NetworkException e )
        {
            throw new PeerException( e.getMessage() );
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
            throw new PeerException( "Could not register peer." );
        }
    }


    //todo review and remove if not needed (when kurjun will be removed)
    private String generateActiveUserToken() throws PeerException
    {
        try
        {
            User user = identityManager.getActiveUser();

            UserToken userToken =
                    identityManager.createUserToken( user, "", "", "", TokenType.Permanent.getId(), null );

            return userToken.getFullToken();
        }
        catch ( Exception e )
        {
            throw new PeerException( "Failed to generate active user token.", e );
        }
    }


    private <T> T fromJson( String value, Class<T> type ) throws IOException
    {
        return mapper.readValue( value, type );
    }


    private String toJson( Object value ) throws IOException
    {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString( value );
    }


    protected void updatePeerInCache( final Peer peer )
    {
        Preconditions.checkNotNull( peer, "Peer could not be null." );

        this.peers.put( peer.getId(), peer );
    }


    protected void removePeerFromCache( String id )
    {
        Peer peer = this.peers.get( id );
        if ( peer != null )
        {
            this.peers.remove( id );
        }
    }


    protected PeerData loadPeerData( final String id )
    {
        return peerDataService.find( id );
    }


    private void updatePeerData( final PeerData peerData ) throws PeerException
    {
        Preconditions.checkNotNull( peerData, "Peer data could not be null." );

        this.peerDataService.saveOrUpdate( peerData );
    }


    private void removePeerData( String id )
    {
        this.peerDataService.remove( id );
    }


    /**
     * Creates the peer instance by provided peer data
     *
     * @param peerData peer data
     *
     * @return peer instance
     */
    private Peer constructPeerPojo( final PeerData peerData ) throws PeerException
    {
        Preconditions.checkNotNull( peerData, "Peer info could not be null." );

        try
        {
            PeerInfo peerInfo = fromJson( peerData.getInfo(), PeerInfo.class );

            if ( localPeerId.equals( peerData.getId() ) )
            {
                localPeer.setPeerInfo( peerInfo );
                return localPeer;
            }

            RemotePeerImpl remotePeer =
                    new RemotePeerImpl( localPeerId, securityManager, peerInfo, messenger, commandResponseListener,
                            messageResponseListener, provider, this );

            RelationInfoMeta relationInfoMeta = new RelationInfoMeta();
            Map<String, String> traits = relationInfoMeta.getRelationTraits();
            traits.put( "receiveHeartbeats", "allow" );
            traits.put( "sendHeartbeats", "allow" );
            traits.put( "hostTemplates", "allow" );

            User peerOwner = identityManager.getUserByKeyId( identityManager.getPeerOwnerId() );
            RelationMeta relationMeta = new RelationMeta( peerOwner, localPeer, remotePeer, localPeer.getKeyId() );
            Relation relation = relationManager.buildRelation( relationInfoMeta, relationMeta );
            relation.setRelationStatus( RelationStatus.VERIFIED );
            relationManager.saveRelation( relation );

            return remotePeer;
        }
        catch ( Exception e )
        {
            throw new PeerException( "Could not create peer instance.", e );
        }
    }


    private void unregister( final RegistrationData registrationData ) throws PeerException
    {

        try
        {
            //*********Remove Security Relationship  ****************************
            securityManager.getKeyManager().removePublicKeyRing( registrationData.getPeerInfo().getId() );
            //*******************************************************************

            securityManager.getKeyStoreManager().removeCertFromTrusted( Common.DEFAULT_PUBLIC_SECURE_PORT,
                    registrationData.getPeerInfo().getId() );

            securityManager.getHttpContextManager().reloadKeyStore();
        }
        catch ( Exception e )
        {
            throw new PeerException( "Could not unregister peer.", e );
        }

        User peerOwner = identityManager.getUserByKeyId( identityManager.getPeerOwnerId() );
        Peer remotePeer = getPeer( registrationData.getPeerInfo().getId() );
        RelationMeta relationMeta = new RelationMeta( peerOwner, localPeer, remotePeer, localPeer.getKeyId() );
        relationManager.removeRelation( relationMeta );

        removePeerData( registrationData.getPeerInfo().getId() );
        removePeerFromCache( registrationData.getPeerInfo().getId() );
    }


    @Override
    public void setName( final String peerId, final String newName ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( peerId ), "Invalid peer id" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( newName ), "Invalid peer name" );


        PeerData peerData = loadPeerData( peerId );

        if ( peerData == null )
        {
            throw new PeerNotRegisteredException();
        }

        Peer peer = constructPeerPojo( peerData );

        peer.getPeerInfo().setName( newName );

        try
        {
            peerData.setInfo( toJson( peer.getPeerInfo() ) );
        }
        catch ( IOException e )
        {
            throw new PeerException( e );
        }

        //update db
        updatePeerData( peerData );

        //update cache
        updatePeerInCache( peer );
    }


    @Override
    public List<Peer> getPeers()
    {
        return new ArrayList<>( this.peers.values() );
    }


    @Override
    public Set<Peer> resolve( final Set<String> peers ) throws PeerException
    {
        Set<Peer> result = new HashSet<>();
        for ( String peerId : peers )
        {

            result.add( getPeer( peerId ) );
        }
        return result;
    }


    private List<PeerPolicy> getPolicies()
    {
        List<PeerPolicy> result = new ArrayList<>();
        for ( PeerData peerData : peerDataService.getAll() )
        {
            try
            {
                PeerPolicy peerPolicy = fromJson( peerData.getPolicy(), PeerPolicy.class );
                result.add( peerPolicy );
            }
            catch ( IOException e )
            {
                //ignore
            }
        }
        return result;
    }


    @Override
    public Peer getPeer( final String peerId ) throws PeerException
    {
        Peer result = this.peers.get( peerId );
        if ( result == null )
        {
            throw new PeerException( "Peer not found: " + peerId );
        }
        return result;
    }


    @Override
    public LocalPeer getLocalPeer()
    {
        return localPeer;
    }


    private RegistrationData getRequest( final String id )
    {
        return this.registrationRequests.get( id );
    }


    private void addRequest( final RegistrationData registrationData )
    {
        this.registrationRequests.put( registrationData.getPeerInfo().getId(), registrationData );
    }


    private void removeRequest( final String id )
    {
        this.registrationRequests.remove( id );
    }


    @RolesAllowed( { "Peer-Management|Write", "Peer-Management|Update" } )
    @Override
    public RegistrationData processRegistrationRequest( final RegistrationData registrationData ) throws PeerException
    {
        try
        {
            getRemotePeerInfo( registrationData.getPeerInfo().getPublicUrl() );
            SocketUtil.check( registrationData.getPeerInfo().getIp(), 3,
                    registrationData.getPeerInfo().getPublicSecurePort() );
        }
        catch ( PeerException e )
        {
            throw new PeerException( String.format( "Registration request rejected. Provided URL %s not accessible.",
                    registrationData.getPeerInfo().getPublicUrl() ) );
        }
        catch ( NetworkException e )
        {
            throw new PeerException( e.getMessage() );
        }

        registrationData.getPeerInfo().setName( String.format( "Peer on %s", registrationData.getPeerInfo().getIp() ) );

        addRequest( registrationData );

        return new RegistrationData( localPeer.getPeerInfo(), registrationData.getKeyPhrase(),
                RegistrationStatus.WAIT );
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void processUnregisterRequest( final RegistrationData registrationData ) throws PeerException
    {
        // Check peer consumers. This remote peer in use?
        if ( !notifyPeerActionListeners(
                new PeerAction( PeerActionType.UNREGISTER, registrationData.getPeerInfo().getId() ) ).succeeded() )
        {
            throw new PeerException( "Could not unregister peer. Peer in use." );
        }
        Peer p = getPeer( registrationData.getPeerInfo().getId() );
        if ( p == null )
        {
            LOG.warn( "Peer not found to unregister: " + registrationData.getPeerInfo().getId() );
            return;
        }

        Encrypted encryptedSslCert = registrationData.getSslCert();
        try
        {
            final String keyPhrase = loadPeerData( registrationData.getPeerInfo().getId() ).getKeyPhrase();
            byte[] decrypted =
                    encryptedSslCert.decrypt( SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) ) );
            if ( !keyPhrase.equals( new String( decrypted, "UTF-8" ) ) )
            {
                throw new PeerException( "Could not unregister peer." );
            }

            unregister( registrationData );
            removeRequest( registrationData.getPeerInfo().getId() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Could not unregister peer.", e );
        }
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void processRejectRequest( final RegistrationData registrationData ) throws PeerException
    {
        final String id = registrationData.getPeerInfo().getId();

        final RegistrationData request = getRequest( id );

        if ( request != null )
        {
            // try to decode with provided key phrase
            final String keyPhrase = request.getKeyPhrase();
            final Encrypted encryptedSslCert = registrationData.getSslCert();
            try
            {
                byte[] key = SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) );
                encryptedSslCert.decrypt( key, String.class );
                removeRequest( id );
            }
            catch ( Exception e )
            {
                LOG.error( e.getMessage(), e );
                throw new PeerException( "Can not reject registration request." );
            }
        }
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void processCancelRequest( final RegistrationData registrationData ) throws PeerException
    {
        final String id = registrationData.getPeerInfo().getId();

        final RegistrationData request = getRequest( id );

        if ( request != null )
        {
            // try to decode with provided key phrase
            final String keyPhrase = registrationData.getKeyPhrase();
            final Encrypted encryptedData = request.getSslCert();
            try
            {
                byte[] key = SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) );
                encryptedData.decrypt( key, String.class );
                removeRequest( id );
            }
            catch ( Exception e )
            {
                LOG.error( e.getMessage(), e );
                throw new PeerException( "Can not cancel registration request." );
            }
        }
    }


    @RolesAllowed( { "Peer-Management|Write", "Peer-Management|Update" } )
    @Override
    public void processApproveRequest( final RegistrationData registrationData ) throws PeerException
    {
        final PeerInfo peerInfo = registrationData.getPeerInfo();

        RegistrationData initRequest = getRequest( peerInfo.getId() );
        if ( initRequest == null )
        {
            throw new PeerException( "Registration request not found." );
        }
        register( initRequest.getKeyPhrase(), registrationData );
        removeRequest( peerInfo.getId() );
    }


    private RegistrationData buildRegistrationData( final String keyPhrase, RegistrationStatus status )
            throws PeerException
    {
        RegistrationData result = new RegistrationData( localPeer.getPeerInfo(), keyPhrase, status );
        switch ( status )
        {
            case REQUESTED:
            case APPROVED:
                String sslCert =
                        securityManager.getKeyStoreManager().exportCertificate( Common.DEFAULT_PUBLIC_SECURE_PORT, "" );

                PGPPublicKey pkey = securityManager.getKeyManager().getPublicKey( localPeerId );
                try
                {
                    byte[] key = SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) );
                    Encrypted encryptedSslCert = new Encrypted( sslCert, key );
                    result.setSslCert( encryptedSslCert );
                    String publicKey = PGPKeyUtil.exportAscii( pkey );
                    Encrypted encryptedPublicKey = new Encrypted( publicKey, key );
                    result.setPublicKey( encryptedPublicKey );
                }
                catch ( Exception e )
                {
                    LOG.warn( e.getMessage(), e );
                }
                break;
            case UNREGISTERED:
                try
                {
                    byte[] key = SecurityUtilities.generateKey( keyPhrase.getBytes( "UTF-8" ) );
                    Encrypted encryptedData = new Encrypted( keyPhrase, key );
                    result.setSslCert( encryptedData );
                }
                catch ( Exception e )
                {
                    LOG.warn( e.getMessage(), e );
                }
                break;
        }
        return result;
    }


    protected PeerInfo getRemotePeerInfo( String destinationHost ) throws PeerException
    {
        return registrationClient.getPeerInfo( destinationHost );
    }


    @RolesAllowed( { "Peer-Management|Write", "Peer-Management|Update" } )
    @Override
    public void doRegistrationRequest( final String destinationHost, final String keyPhrase ) throws PeerException
    {
        Preconditions.checkNotNull( keyPhrase );
        URL destinationUrl = checkDestinationHostConstraints( destinationHost );

        PeerInfo peerInfo = getRemotePeerInfo( destinationUrl.toString() );

        try
        {
            SocketUtil.check( peerInfo.getIp(), 3, peerInfo.getPublicSecurePort() );
        }
        catch ( NetworkException e )
        {
            throw new PeerException( e.getMessage() );
        }

        if ( getRequest( peerInfo.getId() ) != null || localPeerId.equals( peerInfo.getId() ) )
        {
            throw new PeerException( "Registration record already exists." );
        }

        try
        {
            final RegistrationData registrationData = buildRegistrationData( keyPhrase, RegistrationStatus.REQUESTED );

            registrationData.setToken( generateActiveUserToken() );
            registrationData.setKeyPhrase( "" );

            RegistrationData result = registrationClient.sendInitRequest( destinationUrl.toString(), registrationData );

            result.getPeerInfo().setName( String.format( "Peer on %s", peerInfo.getIp() ) );
            result.setKeyPhrase( keyPhrase );

            addRequest( result );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
            throw new PeerException( e.getMessage() );
        }
    }


    public void checkHostAvailability( final String destinationHost ) throws PeerException
    {
        URL url = checkDestinationHostConstraints( destinationHost );

        try
        {
            PeerInfo peerInfo = getRemotePeerInfo( url.toString() );
            SocketUtil.check( peerInfo.getIp(), 3, peerInfo.getPublicSecurePort() );
        }
        catch ( NetworkException ne )
        {
            throw new PeerException( ne.getMessage() );
        }
        catch ( Exception e )
        {
            throw new PeerException( "No response, possibly wrong address" );
        }
    }


    private URL checkDestinationHostConstraints( String destinationHost ) throws PeerException
    {
        Preconditions.checkNotNull( destinationHost );
        URL destinationUrl;
        try
        {
            destinationUrl = buildDestinationUrl( destinationHost );
        }
        catch ( MalformedURLException e )
        {
            throw new PeerException( "Invalid URL." );
        }

        if ( destinationUrl.getHost().equals( localPeer.getPeerInfo().getIp() ) && destinationUrl.getPort() == localPeer
                .getPeerInfo().getPort() )
        {
            throw new PeerException( "Could not send registration request to ourselves." );
        }

        if ( Common.LOCAL_HOST_IP.equals( localPeer.getPeerInfo().getIp() ) )
        {
            throw new PeerException( String.format( "Invalid public URL %s. Please set proper public URL.",
                    localPeer.getPeerInfo().getPublicUrl() ) );
        }

        return destinationUrl;
    }


    private URL buildDestinationUrl( final String destinationHost ) throws MalformedURLException
    {
        try
        {
            return new URL( destinationHost );
        }
        catch ( MalformedURLException e )
        {
            return new URL( String.format( "https://%s:%d/", destinationHost, Common.DEFAULT_PUBLIC_PORT ) );
        }
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void doCancelRequest( final RegistrationData request, boolean forceAction ) throws PeerException
    {

        //********forceAction ********************
        try
        {
            getRemotePeerInfo( request.getPeerInfo().getPublicUrl() );
        }
        catch ( Exception e )
        {
            if ( !forceAction )
            {
                throw new PeerException( "Remote peer is not accessible:" + e.getMessage() );
            }
            else
            {
                LOG.error( "***** Error while performing cancel operation, but proceeding (forcing) !", e );
            }
        }
        //***************************************

        try
        {
            try
            {
                registrationClient.sendCancelRequest( request.getPeerInfo().getPublicUrl(),
                        buildRegistrationData( request.getKeyPhrase(), RegistrationStatus.CANCELLED ) );
            }
            catch ( Exception e )
            {
                if ( !forceAction )
                {
                    throw new PeerException( "Remote peer is not accessible:" + e.getMessage() );
                }
                else
                {
                    LOG.error( "***** Error while performing cancel operation, but proceeding (forcing) !", e );
                }
            }

            //**********************************************
            removeRequest( request.getPeerInfo().getId() );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
            throw new PeerException( e.getMessage() );
        }
    }


    @RolesAllowed( { "Peer-Management|Write", "Peer-Management|Update" } )
    @Override
    public void doApproveRequest( final String keyPhrase, final RegistrationData request ) throws PeerException
    {
        if ( Common.LOCAL_HOST_IP.equals( localPeer.getPeerInfo().getIp() ) )
        {
            throw new PeerException( String.format( "Invalid public URL %s. Please set proper public URL.",
                    localPeer.getPeerInfo().getPublicUrl() ) );
        }

        getRemotePeerInfo( request.getPeerInfo().getPublicUrl() );

        try
        {
            RegistrationData response = buildRegistrationData( keyPhrase, RegistrationStatus.APPROVED );

            response.setToken( generateActiveUserToken() );

            registrationClient.sendApproveRequest( request.getPeerInfo().getPublicUrl(), response );

            register( keyPhrase, request );

            removeRequest( request.getPeerInfo().getId() );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
            throw new PeerException( e.getMessage() );
        }
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void doRejectRequest( final RegistrationData request, boolean forceAction ) throws PeerException
    {

        //********forceAction ********************
        try
        {
            getRemotePeerInfo( request.getPeerInfo().getPublicUrl() );
        }
        catch ( Exception e )
        {
            if ( !forceAction )
            {
                throw new PeerException( "Remote peer is not accessible:" + e.getMessage() );
            }
            else
            {
                LOG.error( "***** Error while performing reject operation, but proceeding (forcing) !", e );
            }
        }
        //***************************************

        try
        {
            final RegistrationData r = buildRegistrationData( request.getKeyPhrase(), RegistrationStatus.REJECTED );

            // return received data
            r.setSslCert( request.getSslCert() );

            try
            {
                registrationClient.sendRejectRequest( request.getPeerInfo().getPublicUrl(), r );
            }
            catch ( Exception e )
            {
                if ( !forceAction )
                {
                    throw new PeerException( "Remote peer is not accessible:" + e.getMessage() );
                }
                else
                {
                    LOG.error( "***** Error while performing reject operation, but proceeding (forcing) !", e );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
            throw new PeerException( e.getMessage() );
        }

        removeRequest( request.getPeerInfo().getId() );
    }


    @RolesAllowed( { "Peer-Management|Delete", "Peer-Management|Update" } )
    @Override
    public void doUnregisterRequest( final RegistrationData request, boolean forceAction ) throws PeerException
    {

        //********forceAction ********************
        try
        {
            getRemotePeerInfo( request.getPeerInfo().getPublicUrl() );
        }
        catch ( Exception e )
        {
            if ( !forceAction )
            {
                throw new PeerException( "Remote peer is not accessible:" + e.getMessage() );
            }
            else
            {
                LOG.error( "***** Error while performing unregister operation, but proceeding (forcing) !", e );
            }
        }

        //***************************************
        if ( !notifyPeerActionListeners( new PeerAction( PeerActionType.UNREGISTER, request.getPeerInfo().getId() ) )
                .succeeded() )
        {
            if ( !forceAction )
            {
                throw new PeerException( "Could not unregister peer. Peer in use." );
            }
            else
            {
                LOG.error( "***** Error, Peer in use, but proceeding (forcing) !" );
            }
        }

        try
        {
            RegistrationClient registrationClient = new RegistrationClientImpl( provider );
            PeerData peerData = loadPeerData( request.getPeerInfo().getId() );

            try
            {
                registrationClient.sendUnregisterRequest( request.getPeerInfo().getPublicUrl(),
                        buildRegistrationData( peerData.getKeyPhrase(), RegistrationStatus.UNREGISTERED ) );
            }
            catch ( Exception e )
            {
                if ( !forceAction )
                {
                    throw new PeerException( "Could not unregister peer. Peer in use." );
                }
                else
                {
                    LOG.error( "***** Error while performing unregister operation, but proceeding (forcing) !", e );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.warn( e.getMessage(), e );
        }

        unregister( request );
        removeRequest( request.getPeerInfo().getId() );
    }


    @Override
    public List<RegistrationData> getRegistrationRequests()
    {
        List<RegistrationData> r = new ArrayList<>( registrationRequests.values() );
        for ( Peer peer : getPeers() )
        {
            if ( !peer.getId().equals( localPeer.getId() ) )
            {
                try
                {
                    r.add( new RegistrationData( peer.getPeerInfo(), RegistrationStatus.APPROVED ) );
                }
                catch ( Exception e )
                {
                    LOG.warn( String.format( "Could not get peer info from %s. %s", peer.getId(), e.getMessage() ) );
                }
            }
        }

        return r;
    }


    @Override
    public String getRemotePeerIdByIp( final String ip ) throws PeerException
    {
        Preconditions.checkNotNull( ip );

        String result = null;

        for ( Peer peer : this.peers.values() )
        {
            if ( peer instanceof RemotePeer )
            {
                PeerInfo peerInfo = peer.getPeerInfo();
                if ( ip.equals( peerInfo.getIp() ) )
                {
                    result = peerInfo.getId();
                }
            }
        }

        if ( result == null )
        {
            throw new PeerException( "Peer not found by IP: " + ip );
        }
        return result;
    }


    @Override
    public PeerGroupResources getPeerGroupResources() throws PeerException
    {
        final List<PeerResources> resources = new ArrayList<>();
        for ( final Peer peer : getPeers() )
        {
            try
            {
                PeerResources peerResources = getPeer( peer.getId() ).getResourceLimits( new PeerId( localPeerId ) );
                resources.add( peerResources );
            }
            catch ( Exception ignore )
            {
                //ignore
            }
        }

        return new PeerGroupResources( resources );
    }


    @Override
    public PeerPolicy getAvailablePolicy()
    {
        PeerPolicy result = new PeerPolicy( getLocalPeer().getId(), 0, 0, 0, 0, 0, 0 );
        int diskUsageLimit = 100;
        int cpuUsageLimit = 100;
        int memoryUsageLimit = 100;
        int networkUsageLimit = 100;
        int containerLimit = MAX_CONTAINER_LIMIT;
        int environmentLimit = MAX_ENVIRONMENT_LIMIT;
        for ( PeerPolicy peerPolicy : getPolicies() )
        {
            if ( peerPolicy != null )
            {
                diskUsageLimit -= peerPolicy.getDiskUsageLimit();
                cpuUsageLimit -= peerPolicy.getCpuUsageLimit();
                memoryUsageLimit -= peerPolicy.getMemoryUsageLimit();
                networkUsageLimit -= peerPolicy.getNetworkUsageLimit();
                containerLimit -= peerPolicy.getContainerLimit();
                environmentLimit -= peerPolicy.getEnvironmentLimit();
            }
        }

        try
        {
            result.setDiskUsageLimit( diskUsageLimit < 0 ? 0 : diskUsageLimit );
            result.setCpuUsageLimit( cpuUsageLimit < 0 ? 0 : cpuUsageLimit );
            result.setMemoryUsageLimit( memoryUsageLimit < 0 ? 0 : memoryUsageLimit );
            result.setNetworkUsageLimit( networkUsageLimit < 0 ? 0 : networkUsageLimit );
            result.setContainerLimit( containerLimit < 0 ? 0 : containerLimit );
            result.setEnvironmentLimit( environmentLimit < 0 ? 0 : environmentLimit );
        }
        catch ( Exception e )
        {
            // ignore
        }
        return result;
    }


    @Override
    public RegistrationStatus getRegistrationStatus( String peerId )
    {
        if ( localPeerId.equals( peerId ) )
        {
            return RegistrationStatus.APPROVED;
        }
        RegistrationData r = null;

        for ( RegistrationData rd : getRegistrationRequests() )
        {
            if ( rd.getPeerInfo().getId().equals( peerId ) )
            {
                r = rd;
                break;
            }
        }
        if ( r == null )
        {
            return RegistrationStatus.NOT_REGISTERED;
        }

        return r.getStatus();
    }


    @Override
    public RegistrationStatus getRemoteRegistrationStatus( String peerId )
    {
        if ( localPeerId.equals( peerId ) )
        {
            return RegistrationStatus.APPROVED;
        }
        RegistrationData r = null;

        for ( RegistrationData rd : getRegistrationRequests() )
        {
            if ( rd.getPeerInfo().getId().equals( peerId ) )
            {
                r = rd;
                break;
            }
        }

        if ( r == null )
        {
            return RegistrationStatus.NOT_REGISTERED;
        }

        return registrationClient.getStatus( r.getPeerInfo().getPublicUrl(), localPeerId );
    }


    @Override
    public PeerPolicy getPolicy( final String peerId )
    {
        try
        {
            return fromJson( peerDataService.find( peerId ).getPolicy(), PeerPolicy.class );
        }
        catch ( IOException e )
        {
            // ignore
        }
        return null;
    }


    @Override
    public void setPolicy( String peerId, PeerPolicy peerPolicy ) throws PeerException
    {
        Peer peer = getPeer( peerId );

        if ( peer == null )
        {
            throw new PeerException( "No such registered peer: " + peerId );
        }

        PeerPolicy availablePolicy = getAvailablePolicy();
        try
        {
            Preconditions.checkArgument( peerPolicy.getContainerLimit() <= availablePolicy.getContainerLimit(),
                    "Container limit exceeded." );
            Preconditions.checkArgument( peerPolicy.getEnvironmentLimit() <= availablePolicy.getEnvironmentLimit(),
                    "Environment limit exceeded." );
            Preconditions.checkArgument( peerPolicy.getDiskUsageLimit() <= availablePolicy.getDiskUsageLimit(),
                    "Disk limit exceeded." );
            Preconditions.checkArgument( peerPolicy.getCpuUsageLimit() <= availablePolicy.getCpuUsageLimit(),
                    "CPU limit exceeded." );
            Preconditions.checkArgument( peerPolicy.getMemoryUsageLimit() <= availablePolicy.getMemoryUsageLimit(),
                    "RAM limit exceeded." );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Invalid policy: " + e.getMessage(), e );
        }


        PeerData peerData = loadPeerData( peerId );

        if ( peerData != null )
        {
            try
            {
                peerData.setPolicy( toJson( peerPolicy ) );
                updatePeerData( peerData );
            }
            catch ( IOException e )
            {
                LOG.error( e.getMessage(), e );
            }
        }
    }


    protected synchronized Integer getMaxOrder() throws PeerException
    {
        try
        {
            int result = 0;
            for ( PeerData peerData : peerDataService.getAll() )
            {
                if ( peerData.getOrder() > result )
                {
                    result = peerData.getOrder();
                }
            }
            return result;
        }
        catch ( Exception e )
        {
            throw new PeerException( "Could not get peer order." );
        }
    }


    private class PeerActionResponses extends ArrayList<PeerActionResponse>
    {
        boolean succeeded()
        {
            boolean result = true;
            for ( Iterator<PeerActionResponse> i = iterator(); i.hasNext() && result; )
            {
                PeerActionResponse r = i.next();
                if ( !r.isOk() )
                {
                    result = false;
                }
            }
            return result;
        }
    }


    @Override
    public void setPublicUrl( final String peerId, final String publicUrl, final int securePort ) throws PeerException
    {
        setPublicUrl( peerId, publicUrl, securePort, true );
    }


    private void setPublicUrl( final String peerId, final String publicUrl, final int securePort,
                               boolean manualSetting ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( peerId ) );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( publicUrl ) );
        Preconditions.checkArgument( securePort > 0 );

        synchronized ( localPeer )
        {
            PeerData peerData = peerDataService.find( peerId );

            if ( peerData == null )
            {
                throw new PeerException( "Peer not found." );
            }

            try
            {

                PeerInfo peerInfo = fromJson( peerData.getInfo(), PeerInfo.class );

                peerInfo.setPublicUrl( publicUrl.toLowerCase() );
                peerInfo.setPublicSecurePort( securePort );
                //                peerInfo.setName( String.format( "Peer %s on %s", peerId, peerInfo.getIp() ) );
                peerInfo.setManualSetting( manualSetting );

                peerData.setInfo( toJson( peerInfo ) );

                peerDataService.saveOrUpdate( peerData );

                Peer peer = constructPeerPojo( peerData );
                updatePeerInCache( peer );
            }
            catch ( Exception e )
            {
                throw new PeerException( "Error setting public url ", e );
            }
        }
    }


    private class IpDetectionTask implements Runnable
    {


        private boolean isIpValid( HostInterface hostInterface )
        {
            return hostInterface != null && !( hostInterface instanceof NullHostInterface ) && !Strings
                    .isNullOrEmpty( hostInterface.getIp().trim() );
        }


        @Override
        public void run()
        {
            synchronized ( localPeer )
            {
                try
                {
                    if ( localPeer.isInitialized() && (
                            Common.DEFAULT_PUBLIC_URL.equals( localPeer.getPeerInfo().getPublicUrl() ) || !localPeer
                                    .getPeerInfo().isManualSetting() ) )
                    {

                        HostInterface eth1 = localPeer.getManagementHost().getInterfaceByName( "eth1" );

                        if ( isIpValid( eth1 ) )
                        {
                            HostInterface wan = localPeer.getManagementHost().getInterfaceByName( "wan" );

                            if ( !wan.getIp().equals( localPeer.getPeerInfo().getIp() ) )

                            {
                                setPublicUrl( localPeerId, wan.getIp(), localPeer.getPeerInfo().getPublicSecurePort(),
                                        false );
                            }
                        }
                    }
                }
                catch ( HostNotFoundException e )
                {
                    //ignore
                }
                catch ( Exception e )
                {
                    LOG.warn( "Error updating local peer public url", e );
                }
            }
        }
    }
}

