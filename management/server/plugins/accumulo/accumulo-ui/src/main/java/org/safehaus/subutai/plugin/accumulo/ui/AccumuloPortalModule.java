/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.accumulo.ui;


import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.naming.NamingException;

import org.safehaus.subutai.common.util.FileUtil;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import org.safehaus.subutai.server.ui.api.PortalModule;

import com.vaadin.ui.Component;


/**
 * @author dilshat
 */
public class AccumuloPortalModule implements PortalModule
{
    public static final String MODULE_IMAGE = "accumulo.png";
    protected static final Logger LOG = Logger.getLogger( AccumuloPortalModule.class.getName() );
    private final ServiceLocator serviceLocator;
    private ExecutorService executor;


    public AccumuloPortalModule()
    {
        this.serviceLocator = new ServiceLocator();
    }


    public void init()
    {
        executor = Executors.newCachedThreadPool();
    }


    public void destroy()
    {
        executor.shutdown();
    }


    @Override
    public String getId()
    {
        return AccumuloClusterConfig.PRODUCT_KEY;
    }


    public String getName()
    {
        return AccumuloClusterConfig.PRODUCT_KEY;
    }


    @Override
    public File getImage()
    {
        return FileUtil.getFile( AccumuloPortalModule.MODULE_IMAGE, this );
    }


    public Component createComponent()
    {
        try
        {
            return new AccumuloComponent( executor, serviceLocator );
        }
        catch ( NamingException e )
        {
            LOG.severe( e.getMessage() );
        }

        return null;
    }


    @Override
    public Boolean isCorePlugin()
    {
        return false;
    }
}