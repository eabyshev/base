package io.subutai.core.environment.api.exception;


/**
 * Thrown if security error occurred in environment manager
 */
public class EnvironmentSecurityException extends RuntimeException
{
    public EnvironmentSecurityException( final String message )
    {
        super( message );
    }
}
