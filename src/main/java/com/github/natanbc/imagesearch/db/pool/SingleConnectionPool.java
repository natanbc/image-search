package com.github.natanbc.imagesearch.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

/** SQLite locks the whole file state using a file lock lol. */
public class SingleConnectionPool extends MultiConnectionPool {
    public SingleConnectionPool(Connection connection) throws SQLException {
        super((i) -> connection, 1);
    }
}
