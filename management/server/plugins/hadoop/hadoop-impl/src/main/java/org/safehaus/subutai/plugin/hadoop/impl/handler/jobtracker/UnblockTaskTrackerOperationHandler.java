package org.safehaus.subutai.plugin.hadoop.impl.handler.jobtracker;


import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hadoop.impl.HadoopImpl;
import org.safehaus.subutai.plugin.hadoop.impl.common.Commands;


public class UnblockTaskTrackerOperationHandler extends AbstractOperationHandler<HadoopImpl>
{

    private String lxcHostName;


    public UnblockTaskTrackerOperationHandler( HadoopImpl manager, String clusterName, String lxcHostName )
    {
        super( manager, clusterName );
        this.lxcHostName = lxcHostName;
        productOperation = manager.getTracker().createProductOperation( HadoopClusterConfig.PRODUCT_KEY,
                String.format( "Unblocking TaskTracker in %s", clusterName ) );
    }


    @Override
    public void run()
    {
        HadoopClusterConfig hadoopClusterConfig = manager.getCluster( clusterName );

        if ( hadoopClusterConfig == null )
        {
            productOperation.addLogFailed( String.format( "Installation with name %s does not exist", clusterName ) );
            return;
        }

        if ( hadoopClusterConfig.getJobTracker() == null )
        {
            productOperation.addLogFailed( String.format( "TaskTracker on %s does not exist", clusterName ) );
            return;
        }

        Agent node = manager.getAgentManager().getAgentByHostname( lxcHostName );
        if ( node == null )
        {
            productOperation.addLogFailed( "TaskTracker is not connected" );
            return;
        }

        Command removeCommand = Commands.getSetTaskTrackerCommand( hadoopClusterConfig, node );
        manager.getCommandRunner().runCommand( removeCommand );
        logCommand( removeCommand, productOperation );

        Command includeCommand = Commands.getExcludeTaskTrackerCommand( hadoopClusterConfig, node );
        manager.getCommandRunner().runCommand( includeCommand );
        logCommand( includeCommand, productOperation );

        Command refreshCommand = Commands.getStartTaskTrackerCommand( node );
        manager.getCommandRunner().runCommand( refreshCommand );
        logCommand( refreshCommand, productOperation );

        hadoopClusterConfig.getBlockedAgents().remove( node );
        manager.getPluginDAO()
               .saveInfo( HadoopClusterConfig.PRODUCT_KEY, hadoopClusterConfig.getClusterName(), hadoopClusterConfig );
        productOperation.addLog( "Cluster info saved to DB" );
    }


    private void logCommand( Command command, ProductOperation po )
    {
        if ( command.hasSucceeded() )
        {
            po.addLogDone( String.format( "Task's operation %s finished", command.getDescription() ) );
        }
        else if ( command.hasCompleted() )
        {
            po.addLogFailed( String.format( "Task's operation %s failed", command.getDescription() ) );
        }
        else
        {
            po.addLogFailed( String.format( "Task's operation %s timeout", command.getDescription() ) );
        }
    }
}