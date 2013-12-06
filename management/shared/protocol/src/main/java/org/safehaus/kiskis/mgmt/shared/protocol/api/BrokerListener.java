package org.safehaus.kiskis.mgmt.shared.protocol.api;


import org.safehaus.kiskis.mgmt.shared.protocol.Response;


/**
 * Used by Management Server components query and manage the managed Agents
 */
public interface BrokerListener {
    /**
     * For Communication Bundle
     *
     * @param response
     * @return
     */
    public void onResponse(Response response);

}
