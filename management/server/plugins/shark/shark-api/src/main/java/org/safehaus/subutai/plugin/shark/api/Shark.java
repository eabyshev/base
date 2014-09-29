package org.safehaus.subutai.plugin.shark.api;


import java.util.UUID;
import org.safehaus.subutai.common.protocol.ApiBase;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;


public interface Shark extends ApiBase<SharkClusterConfig>
{

    public UUID installCluster( SharkClusterConfig config, HadoopClusterConfig hadoopConfig );


    public UUID addNode( String clusterName, String lxcHostname );


    public UUID destroyNode( String clusterName, String lxcHostname );


    public UUID actualizeMasterIP( String clusterName );


    public ClusterSetupStrategy getClusterSetupStrategy( ProductOperation po, SharkClusterConfig config,
                                                         Environment environment );


}

