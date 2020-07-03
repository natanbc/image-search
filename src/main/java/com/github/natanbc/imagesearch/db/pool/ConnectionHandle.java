package com.github.natanbc.imagesearch.db.pool;

import java.sql.Connection;

public class ConnectionHandle implements AutoCloseable {
    private Connection connection;
    private ConnectionPool yield;

    protected ConnectionHandle(Connection connection, ConnectionPool yield) {
        this.connection = connection;
        this.yield = yield;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public void close() {
        this.yield.yield(this.connection);
    }
}
