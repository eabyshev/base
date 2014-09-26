package org.safehaus.subutai.core.communication.api;


import org.safehaus.subutai.common.protocol.SkipNull;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;


/**
 * Skips fields with null values from (de)serialization by GSON
 */
public class SkipNullsExclusionStrategy implements ExclusionStrategy
{
    @Override
    public boolean shouldSkipField( final FieldAttributes fieldAttributes )
    {
        return fieldAttributes.getAnnotation( SkipNull.class ) != null;
    }


    @Override
    public boolean shouldSkipClass( final Class<?> aClass )
    {
        return false;
    }
}