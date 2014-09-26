/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.shark.ui;


import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.naming.NamingException;

import org.safehaus.subutai.common.util.FileUtil;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.plugin.shark.api.SharkClusterConfig;
import org.safehaus.subutai.server.ui.api.PortalModule;

import com.vaadin.ui.Component;


/**
 * @author dilshat
 */
public class SharkPortalModule implements PortalModule
{

    public static final String MODULE_IMAGE = "shark.png";
    protected static final Logger LOG = Logger.getLogger( SharkPortalModule.class.getName() );
    private final ServiceLocator serviceLocator;
    private ExecutorService executor;


    public SharkPortalModule()
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
        return SharkClusterConfig.PRODUCT_KEY;
    }


    public String getName()
    {
        return SharkClusterConfig.PRODUCT_KEY;
    }


    @Override
    public File getImage()
    {
        return FileUtil.getFile( SharkPortalModule.MODULE_IMAGE, this );
    }


    public Component createComponent()
    {
        try
        {
            return new SharkComponent( executor, serviceLocator );
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