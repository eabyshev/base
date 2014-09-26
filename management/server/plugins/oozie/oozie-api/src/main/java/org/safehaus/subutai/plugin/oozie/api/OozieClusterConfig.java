package org.safehaus.subutai.plugin.oozie.api;


import java.util.Set;
import java.util.UUID;

import org.doomdark.uuid.UUIDGenerator;
import org.safehaus.subutai.common.protocol.ConfigBase;
import org.safehaus.subutai.common.settings.Common;


/**
 * @author dilshat
 */
public class OozieClusterConfig implements ConfigBase
{

    public static final String PRODUCT_KEY = "Oozie";
    public static final String PRODUCT_NAME_CLIENT = "hadoopOozieClient";
    private String templateNameClient = PRODUCT_NAME_CLIENT;
    public static final String PRODUCT_NAME_SERVER = "hadoopOozieServer";
    private String templateNameServer = PRODUCT_NAME_SERVER;
    private String domainName = Common.DEFAULT_DOMAIN_NAME;
    private String hadoopClusterName;
    private UUID uuid;
    private String server;
    private Set<String> clients;
    private String clusterName = "";
    private SetupType setupType;


    public OozieClusterConfig()
    {
        this.uuid = UUID.fromString( UUIDGenerator.getInstance().generateTimeBasedUUID().toString() );
    }


    public String getTemplateNameServer()
    {
        return templateNameServer;
    }


    public void setTemplateNameServer( final String templateNameServer )
    {
        this.templateNameServer = templateNameServer;
    }


    public SetupType getSetupType()
    {
        return setupType;
    }


    public void setSetupType( final SetupType setupType )
    {
        this.setupType = setupType;
    }


    public String getHadoopClusterName()
    {
        return hadoopClusterName;
    }


    public void setHadoopClusterName( final String hadoopClusterName )
    {
        this.hadoopClusterName = hadoopClusterName;
    }


    public UUID getUuid()
    {
        return uuid;
    }


    public void setUuid( UUID uuid )
    {
        this.uuid = uuid;
    }


    public void reset()
    {
        this.server = null;
        this.clients = null;
        this.domainName = "";
    }


    public String getDomainName()
    {
        return domainName;
    }


    public void setDomainName( String domainName )
    {
        this.domainName = domainName;
    }


    public String getServer()
    {
        return server;
    }


    public void setServer( String server )
    {
        this.server = server;
    }


    public Set<String> getClients()
    {
        return clients;
    }


    public void setClients( Set<String> clients )
    {
        this.clients = clients;
    }


    public String getClusterName()
    {
        return clusterName;
    }


    public void setClusterName( String clusterName )
    {
        this.clusterName = clusterName;
    }


    @Override
    public String getProductName()
    {
        return PRODUCT_KEY;
    }


    /*public Set<String> getHadoopNodes() {
        return hadoopNodes;
    }


    public void setHadoopNodes( Set<String> hadoopNodes ) {
        this.hadoopNodes = hadoopNodes;
    }*/


    @Override
    public String toString()
    {
        return "OozieConfig{" +
                "domainName='" + domainName + '\'' +
                ", uuid=" + uuid +
                ", server='" + server + '\'' +
                ", clients=" + clients +
                //                ", hadoopNodes=" + hadoopNodes +
                ", clusterName='" + clusterName + '\'' +
                '}';
    }


    public String getTemplateNameClient()
    {
        return templateNameClient;
    }


    public void setTemplateNameClient( final String templateNameClient )
    {
        this.templateNameClient = templateNameClient;
    }


    public Set<String> getAllOozieAgents()
    {
        clients.add( server );
        return clients;
    }
}