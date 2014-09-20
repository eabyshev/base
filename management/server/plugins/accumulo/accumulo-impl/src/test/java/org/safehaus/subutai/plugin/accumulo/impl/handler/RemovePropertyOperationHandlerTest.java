package org.safehaus.subutai.plugin.accumulo.impl.handler;


import org.junit.Test;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.tracker.ProductOperationState;
import org.safehaus.subutai.plugin.accumulo.impl.handler.mock.AccumuloImplMock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


public class RemovePropertyOperationHandlerTest
{
    @Test
    public void testWithoutCluster()
    {
        AbstractOperationHandler operationHandler =
                new RemovePropertyOperationHandler( new AccumuloImplMock(), "test-cluster", "test-property" );

        operationHandler.run();

        assertTrue( operationHandler.getProductOperation().getLog().contains( "not exist" ) );
        assertEquals( operationHandler.getProductOperation().getState(), ProductOperationState.FAILED );
    }
}
