/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.neo4j.driver.internal.security.SecurityPlan;
import org.neo4j.driver.internal.spi.ConnectionProvider;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Logger;
import org.neo4j.driver.v1.Logging;
import org.neo4j.driver.v1.Session;

import static java.lang.String.format;

public class InternalDriver implements Driver
{
    private final static String DRIVER_LOG_NAME = "Driver";

    private final SecurityPlan securityPlan;
    private final SessionFactory sessionFactory;
    private final ConnectionProvider connectionProvider;
    private final Logger log;

    private AtomicBoolean closed = new AtomicBoolean( false );

    InternalDriver( SecurityPlan securityPlan, SessionFactory sessionFactory, ConnectionProvider connectionProvider,
            Logging logging )
    {
        this.securityPlan = securityPlan;
        this.sessionFactory = sessionFactory;
        this.connectionProvider = connectionProvider;
        this.log = logging.getLog( DRIVER_LOG_NAME );
    }

    @Override
    public final boolean isEncrypted()
    {
        assertOpen();
        return securityPlan.requiresEncryption();
    }

    @Override
    public final Session session()
    {
        return session( AccessMode.WRITE );
    }

    @Override
    public final Session session( AccessMode mode )
    {
        assertOpen();
        Session session = newSessionWithMode( mode );
        if ( closed.get() )
        {
            // the driver is already closed and we either 1. obtain this session from the old session pool
            // or 2. we obtain this session from a new session pool
            // For 1. this closeResources will take no effect as everything is already closed.
            // For 2. this closeResources will close the new connection pool just created to ensure no resource leak.
            closeResources();
            throw driverCloseException();
        }
        return session;
    }

    @Override
    public final void close()
    {
        if ( closed.compareAndSet( false, true ) )
        {
            closeResources();
        }
    }

    public final ConnectionProvider getConnectionProvider()
    {
        return connectionProvider;
    }

    private Session newSessionWithMode( AccessMode mode )
    {
        return sessionFactory.newInstance( mode );
    }

    private void closeResources()
    {
        try
        {
            // todo: driver can just close session factory here...
            connectionProvider.close();
        }
        catch ( Exception ex )
        {
            log.error( format( "~~ [ERROR] %s", ex.getMessage() ), ex );
        }
    }

    private void assertOpen()
    {
        if ( closed.get() )
        {
            throw driverCloseException();
        }
    }

    private static RuntimeException driverCloseException()
    {
        return new IllegalStateException( "This driver instance has already been closed" );
    }
}
