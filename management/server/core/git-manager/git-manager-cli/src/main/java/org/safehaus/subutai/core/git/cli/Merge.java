package org.safehaus.subutai.core.git.cli;


import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.git.api.GitException;
import org.safehaus.subutai.core.git.api.GitManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import com.google.common.base.Preconditions;


/**
 * Merges current branch with specified branch
 */
@Command(scope = "git", name = "merge", description = "Merge current branch with specified branch")
public class Merge extends OsgiCommandSupport
{

    private static final Logger LOG = LoggerFactory.getLogger( Merge.class.getName() );


    @Argument(index = 0, name = "hostname", required = true, multiValued = false, description = "agent hostname")
    String hostname;
    @Argument(index = 1, name = "repoPath", required = true, multiValued = false, description = "path to git repo")
    String repoPath;
    @Argument(index = 2, name = "branch name", required = false, multiValued = false,
            description = "branch name to merge with (master = default)")
    String branchName;

    private final GitManager gitManager;
    private final AgentManager agentManager;


    public Merge( final GitManager gitManager, final AgentManager agentManager )
    {
        Preconditions.checkNotNull( gitManager, "Git Manager is null" );
        Preconditions.checkNotNull( agentManager, "Agent Manager is null" );

        this.gitManager = gitManager;
        this.agentManager = agentManager;
    }


    public void setHostname( final String hostname )
    {
        this.hostname = hostname;
    }


    public void setBranchName( final String branchName )
    {
        this.branchName = branchName;
    }


    protected Object doExecute()
    {

        Agent agent = agentManager.getAgentByHostname( hostname );
        if ( agent == null )
        {
            System.out.println( "Agent not connected" );
        }
        else
        {
            try
            {
                if ( branchName != null )
                {
                    gitManager.merge( agent, repoPath, branchName );
                }
                else
                {
                    gitManager.merge( agent, repoPath );
                }
            }
            catch ( GitException e )
            {
                LOG.error( "Error in doExecute", e );
                System.out.println( e.getMessage() );
            }
        }

        return null;
    }
}
