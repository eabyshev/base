package org.safehaus.subutai.plugin.hadoop.impl.common;


import java.util.ArrayList;
import java.util.List;

import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;


/**
 * Created with IntelliJ IDEA. User: daralbaev Date: 1/31/14 Time: 10:08 PM
 */
public class InstallHadoopOperation
{
    private final HadoopClusterConfig hadoopClusterConfig;
    private List<Command> commandList;


    public InstallHadoopOperation( HadoopClusterConfig hadoopClusterConfig )
    {

        this.hadoopClusterConfig = hadoopClusterConfig;
        commandList = new ArrayList<>();

        //		commandList.add(Commands.getInstallCommand(config));
        commandList.add( Commands.getClearMastersCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getClearSlavesCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getSetMastersCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getAddSecondaryNamenodeCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getSetDataNodeCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getSetTaskTrackerCommand( hadoopClusterConfig ) );
        commandList.add( Commands.getFormatNameNodeCommand( hadoopClusterConfig ) );
    }


    public List<Command> getCommandList()
    {
        return commandList;
    }
}