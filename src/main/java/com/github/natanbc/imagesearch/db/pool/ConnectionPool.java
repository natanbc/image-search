package com.github.natanbc.imagesearch.db.pool;

import java.sql.Connection;

public interface ConnectionPool extends AutoCloseable {
    /** Takes one connection from the queue or waits for one to become available. */
    ConnectionHandle take() throws InterruptedException;

    /** Returns a connection object to the back of the queue. */
    void yield(Connection c);
}
