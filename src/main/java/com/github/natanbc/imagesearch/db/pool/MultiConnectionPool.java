package com.github.natanbc.imagesearch.db.pool;

import com.github.natanbc.imagesearch.db.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;

public class MultiConnectionPool implements ConnectionPool {
    /* Underlying data structure. */
    protected ArrayBlockingQueue<Connection> connections;

    public MultiConnectionPool(Connector connector, int count) throws SQLException {
        if(count < 1) throw new IllegalArgumentException("The number of " +
            "connections must be equal to or greater than one");

        this.connections = new ArrayBlockingQueue<>(count);
        for(int i = 0; i < count; ++i)
            connections.add(connector.connect(i));
    }

    @Override
    public void close() throws SQLException {
        for(var a : this.connections) a.close();
    }

    public interface Connector {
        Connection connect(int index) throws SQLException;
    }

    /** Takes one connection from the queue or waits for one to become available. */
    @Override
    public ConnectionHandle take() throws InterruptedException {
        return new ConnectionHandle(this.connections.take(), this);
    }

    /** Returns a connection object to the back of the queue. */
    @Override
    public void yield(Connection c) {
        /* It is a bug to call this method with the wrong parameters. We want
         * this to throw an exception. */
        this.connections.add(c);
    }
}
