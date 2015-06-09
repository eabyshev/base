package org.safehaus.subutai.pluginmanager.api;


import java.util.List;
import java.util.Set;
import java.util.UUID;



public interface PluginManager
{
    public UUID installPlugin( String pluginName );

    public UUID removePlugin( String pluginName );

    public UUID upgradePlugin( String pluginName );

    public Set<PluginInfo> getInstalledPlugins();

    public Set<PluginInfo> getAvailablePlugins();

    public Set<String> getAvailablePluginNames();

    public List<String> getAvaileblePluginVersions();

    public List<String> getInstalledPluginVersions();

    public Set<String> getInstalledPluginNames();

    public String getPluginVersion( String pluginName );

    public boolean isUpgradeAvailable( String pluginName );

    public String getProductKey();

    public boolean isInstalled( String p );

    public boolean operationSuccessful( OperationType operationType);
}