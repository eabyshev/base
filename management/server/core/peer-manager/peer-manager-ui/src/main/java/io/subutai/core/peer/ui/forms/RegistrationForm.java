package io.subutai.core.peer.ui.forms;


import java.util.Iterator;

import javax.swing.JInternalFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.subutai.common.environment.Environment;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.Peer;
import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.peer.RegistrationData;
import io.subutai.common.peer.RegistrationStatus;
import io.subutai.common.peer.ResourceHost;
import io.subutai.core.peer.ui.PeerManagerPortalModule;
import io.subutai.server.ui.component.ConfirmationDialog;


/**
 * Registration process should be handled in save manner so no middleware attacks occur. In order to get there peers
 * need to exchange with public keys. This will create ssl layer by encrypting all traffic passing through their
 * connection. So first initial handshake will be one direction, to pass keys through encrypted channel and register
 * them in peers' trust stores. These newly saved keys will be used further for safe communication, with bidirectional
 * authentication.
 *
 *
 * TODO here still exists some issues concerned via registration/reject/approve requests. Some of them must pass through
 * secure channel such as unregister process. Which already must be in bidirectional auth completed stage.
 */

//TODO: move rest calls to RemotePeer
public class RegistrationForm extends CustomComponent
{

    private static final Logger LOG = LoggerFactory.getLogger( PeerRegisterForm.class.getName() );
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    //    private AbsoluteLayout mainLayout;
    private Table peersTable;
    private Button showPeersButton;
    private Button doRequestButton;
    private TextField hostField;
    private TextField keyPhraseField;

    private Table requestsTable;

    private PeerManagerPortalModule module;
    private ApproveWindow approveWindow;


    /**
     * The constructor should first build the main layout, set the composition root and then do any custom
     * initialization. <p/> The constructor will not be automatically regenerated by the visual editor.
     */
    public RegistrationForm( final PeerManagerPortalModule module )
    {
        VerticalLayout layout = buildLayout();
        setCompositionRoot( layout );

        approveWindow = new ApproveWindow( module.getPeerManager() );
        this.module = module;
        updateRequestsTable();
        //        showPeersButton.click();
    }


    private VerticalLayout buildLayout()
    {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing( true );
        content.setMargin( true );
        content.setStyleName( "default" );
        content.setSizeFull();

        // peerRegistration
        final Label peerRegistration = new Label();
        peerRegistration.setImmediate( false );
        peerRegistration.setValue( "Peer registration" );
        content.addComponent( peerRegistration );

        FormLayout fl = new FormLayout();

        fl.addComponent( new Label( "Host" ) );
        // hostField
        hostField = new TextField();
        hostField.setImmediate( false );
        hostField.setMaxLength( 45 );
        fl.addComponent( hostField );

        //        content.addComponent( fl );

        //        fl = new FormLayout(  );
        fl.addComponent( new Label( "Secret keyphrase" ) );
        // secretKeyphrase
        keyPhraseField = new TextField();
        keyPhraseField.setImmediate( false );
        keyPhraseField.setMaxLength( 45 );
        fl.addComponent( keyPhraseField );
        content.addComponent( fl );

        // doRequestButton
        doRequestButton = createRegisterButton();
        content.addComponent( doRequestButton );
        content.addComponent( createRefreshButton() );

        // requestsTable
        requestsTable = new Table();

        requestsTable.setCaption( "List of remote peers" );

        requestsTable.setImmediate( true );
        //        requestsTable.setHeight( "283px" );
        requestsTable.setSizeFull();
        requestsTable.addContainerProperty( "ID", String.class, "UNKNOWN" );
        requestsTable.addContainerProperty( "Name", String.class, null );
        requestsTable.addContainerProperty( "Host", String.class, null );
        requestsTable.addContainerProperty( "Key phrase", String.class, null );
        requestsTable.addContainerProperty( "Status", RegistrationStatus.class, null );
        requestsTable.addContainerProperty( "Action", RequestActionsComponent.class, null );

        content.addComponent( requestsTable );

        return content;
    }


    private Button createRefreshButton()
    {
        showPeersButton = new Button();
        showPeersButton.setCaption( "Refresh" );
        showPeersButton.setImmediate( false );

        showPeersButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                updateRequestsTable();
            }
        } );

        return showPeersButton;
    }


    private void updateRequestsTable()
    {

        requestsTable.removeAllItems();
        for ( RegistrationData registrationData : module.getPeerManager().getRegistrationRequests() )
        {
            LOG.debug( registrationData.getPeerInfo().getIp() );
            RequestActionsComponent.RequestActionListener listener = new RequestActionsComponent.RequestActionListener()

            {
                @Override
                public void OnPositiveButtonTrigger( final RegistrationData request,
                                                     RequestActionsComponent.RequestUpdateViewListener
                                                             updateViewListener )
                {
                    positiveActionTrigger( request, updateViewListener );
                }


                @Override
                public void OnNegativeButtonTrigger( final RegistrationData request,
                                                     RequestActionsComponent.RequestUpdateViewListener
                                                             updateViewListener )
                {
                    negativeActionTrigger( request, updateViewListener );
                }
            };
            RequestActionsComponent actionsComponent =
                    new RequestActionsComponent( module, registrationData, listener );
            requestsTable.addItem( new Object[] {
                    registrationData.getPeerInfo().getId(), registrationData.getPeerInfo().getName(),
                    registrationData.getPeerInfo().getIp(), registrationData.getKeyPhrase(),
                    registrationData.getStatus(), actionsComponent
            }, registrationData.getPeerInfo().getId() );
        }
    }


    private void positiveActionTrigger( final RegistrationData request,
                                        final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        switch ( request.getStatus() )
        {
            case REQUESTED:
                approvePeerRegistration( request, updateViewListener );
                break;
            case APPROVED:
                unregister( request, updateViewListener );
                break;
            default:
                throw new IllegalStateException( request.getStatus().name(), new Throwable( "Invalid case." ) );
        }
    }


    private void negativeActionTrigger( final RegistrationData request,
                                        final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        PeerInfo selfPeer = module.getPeerManager().getLocalPeerInfo();
        switch ( request.getStatus() )
        {
            case REQUESTED:
                rejectRegistration( request, updateViewListener );
                break;
            case WAIT:
                cancelRegistration( request, updateViewListener );
                break;
            default:
                throw new TypeNotPresentException( request.getStatus().name(), new Throwable( "Invalid case." ) );
        }
    }


    /* *************************************************************
     *
     */
    private void sendRegistrationRequest( final String destinationHost, final String keyPhrase )
    {
        try
        {
            module.getPeerManager().doRegistrationRequest( destinationHost, keyPhrase );
        }
        catch ( PeerException e )
        {
            Notification.show( e.getMessage(), Notification.Type.WARNING_MESSAGE );
        }
    }


    private void unregisterMeFromRemote( final RegistrationData request,
                                         final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        int relationExists = 0;
        relationExists = checkEnvironmentExistence( request.getPeerInfo(), relationExists );

        relationExists = isRemotePeerContainersHost( request.getPeerInfo(), relationExists );

        if ( relationExists != 0 )
        {
            String msg;
            switch ( relationExists )
            {
                case 1:
                    msg = "Please destroy all cross peer environments, before you proceed!!!";
                    break;
                case 2:
                    msg = "You cannot unregister Peer, because you are a carrier of Peer's resources!!!"
                            + " Contact with Peer to migrate all his data.";
                    break;
                default:
                    msg = "Cannot break peer relationship.";
            }
            ConfirmationDialog alert = new ConfirmationDialog( msg, "Ok", "" );
            alert.getOk().addClickListener( new Button.ClickListener()
            {
                @Override
                public void buttonClick( Button.ClickEvent clickEvent )
                {
                }
            } );

            getUI().addWindow( alert.getAlert() );
        }
        else
        {
            unregisterPeerRequestThread( request, updateViewListener );
        }
    }


    private int checkEnvironmentExistence( final PeerInfo remotePeerInfo, final int relationExist )
    {
        int relationExists = relationExist;
        for ( final Iterator<Environment> itEnv = module.getEnvironmentManager().getEnvironments().iterator();
              itEnv.hasNext() && relationExists == 0; )
        {
            Environment environment = itEnv.next();
            for ( final Iterator<Peer> itPeer = environment.getPeers().iterator();
                  itPeer.hasNext() && relationExists == 0; )
            {
                Peer peer = itPeer.next();
                if ( peer.getPeerInfo().equals( remotePeerInfo ) )
                {
                    relationExists = 1;
                }
            }
        }
        return relationExists;
    }


    private int isRemotePeerContainersHost( final PeerInfo remotePeerInfo, final int relationExist )
    {
        int relationExists = relationExist;
        for ( final Iterator<ResourceHost> itResource =
              module.getPeerManager().getLocalPeer().getResourceHosts().iterator();
              itResource.hasNext() && relationExists == 0; )
        {
            ResourceHost resourceHost = itResource.next();
            for ( final Iterator<ContainerHost> itContainer = resourceHost.getContainerHosts().iterator();
                  itContainer.hasNext() && relationExists == 0; )
            {
                ContainerHost containerHost = itContainer.next();

                if ( containerHost.getInitiatorPeerId().equals( remotePeerInfo.getId() ) )
                {
                    relationExists = 2;
                }
            }
        }
        return relationExists;
    }


    private void unregisterPeerRequestThread( final RegistrationData request,
                                              final RequestActionsComponent.RequestUpdateViewListener
                                                      updateViewListener )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                unregister( request, updateViewListener );
                updateRequestsTable();
            }
        } ).start();
    }


    private void unregister( final RegistrationData request,
                             final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        try
        {
            module.getPeerManager().doUnregisterRequest( request );
            updateRequestsTable();
        }
        catch ( PeerException e )
        {
            Notification.show( e.getMessage(), Notification.Type.WARNING_MESSAGE );
        }
    }


    private void approvePeerRegistration( final RegistrationData request,
                                          final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                approveRegistrationRequest( request, updateViewListener );
                updateRequestsTable();
            }
        } ).start();
    }


    private void approveRegistrationRequest( final RegistrationData request,
                                             final RequestActionsComponent.RequestUpdateViewListener
                                                     updateViewListener )
    {
        try
        {
            module.getPeerManager().doApproveRequest( keyPhraseField.getValue(), request );
        }
        catch ( Exception e )
        {
            Notification.show( e.getMessage(), Notification.Type.WARNING_MESSAGE );
        }
    }


    /**
     * Peer request rejection intented to be handled before they exchange with keys
     *
     * @param request - registration request
     * @param updateViewListener - used to update peers table with relevant buttons captions
     */
    private void rejectRegistration( final RegistrationData request,
                                     final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    module.getPeerManager().doRejectRequest( request );
                }
                catch ( PeerException e )
                {
                    Notification.show( e.getMessage(), Notification.Type.WARNING_MESSAGE );
                }
                updateRequestsTable();
            }
        } ).start();
    }


    private void cancelRegistration( final RegistrationData request,
                                     final RequestActionsComponent.RequestUpdateViewListener updateViewListener )
    {
        new Thread( new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    module.getPeerManager().doCancelRequest( request );
                }
                catch ( PeerException e )
                {
                    Notification.show( e.getMessage(), Notification.Type.WARNING_MESSAGE );
                }
                updateRequestsTable();
            }
        } ).start();
    }


    /**
     * Send peer registration request for further handshakes.
     *
     * @return - vaadin button with request initializing click listener
     */
    private Button createRegisterButton()
    {
        doRequestButton = new Button();
        doRequestButton.setCaption( "Register" );
        doRequestButton.setImmediate( true );

        doRequestButton.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent clickEvent )
            {
                getUI().access( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if ( Strings.isNullOrEmpty( hostField.getValue() ) || Strings
                                .isNullOrEmpty( keyPhraseField.getValue() ) )
                        {
                            Notification.show( "Please specify host and key phrase." );
                        }
                        else
                        {
                            sendRegistrationRequest( hostField.getValue(), keyPhraseField.getValue() );
                        }
                        updateRequestsTable();
                    }
                } );
            }
        } );

        return doRequestButton;
    }
}