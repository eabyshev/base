package io.subutai.core.strategy.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;

import io.subutai.common.peer.ContainerSize;
import io.subutai.common.quota.ContainerQuota;
import io.subutai.common.resource.HostResources;
import io.subutai.common.resource.PeerResources;


/**
 * Round robin allocator class
 */
public class RoundRobinAllocator extends PeerResources
{
    private List<AllocatedContainer> containers = new ArrayList<>();

    private Iterator<HostResources> iterator;


    public RoundRobinAllocator( final PeerResources peerResources )
    {
        super( peerResources.getPeerId(), peerResources.getEnvironmentLimit(), peerResources.getContainerLimit(),
                peerResources.getNetworkLimit(), peerResources.getHostResources() );

        iterator = Iterators.cycle( getHostResources() );
    }


    public boolean allocate( final String containerName, final String templateId, final ContainerSize size,
                             ContainerQuota containerQuota )
    {
        HostResources hostResources = iterator.next();
        AllocatedContainer container =
                new AllocatedContainer( containerName, templateId, size, getPeerId(), hostResources.getHostId() );
        containers.add( container );

        return true;
    }


    public List<AllocatedContainer> getContainers()
    {
        return containers;
    }
}
