package com.github.natanbc.imagesearch.db;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.darkryu550.imagesearch.TaggingException;
import com.github.natanbc.imagesearch.db.pool.ConnectionPool;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Pass {
    protected ConnectionPool database;
    protected HashMap<String, Tagger> taggers;
    protected String table;
    protected String constraint;

    protected Pass(ConnectionPool database, HashMap<String, Tagger> taggers, String table, String constraint) {
        this.database = database;
        this.taggers = taggers;
        this.table = table;
        this.constraint = constraint;
    }

    /** Runs this pass of the tagger on the given executor service.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     * @throws IOException When the file for a given image could not be read.
     * @throws ExecutionException When a task running on the executor failed
     * with an exception.
     * @throws TaggingException When the tagging process for one of the images
     * has failed.
     */
    public void runOn(ExecutorService executor)
            throws SQLException, InterruptedException,
            IOException, ExecutionException, TaggingException {

        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();

            /* Build the query string. */
            var query = new StringBuilder("select id, path");
            for (var name : taggers.keySet()) {
                query.append(", ");
                query.append(Database.taggerColumnName(name));
            }
            query.append(" from ");
            query.append(this.table);

            if (this.constraint != null) {
                query.append(" where ");
                query.append(this.constraint);
            }

            if (!statement.execute(query.toString())) {
                var warning = statement.getWarnings();
                warning.printStackTrace();

                return;
            }

            /* Execute the taggers on the results of the query. */
            var result = statement.getResultSet();
            var futures = new ArrayList<Future<TagResult>>(taggers.size());

            while (result.next()) {
                /* We only need to open and buffer the image once. */
                var path = result.getString("path");
                var imid = result.getString("id");
                var file = new File(path);
                var buff = ImageIO.read(file);

                for (var entry : taggers.entrySet()) {
                    var name = entry.getKey();
                    var tagger = entry.getValue();

                    futures.add(executor.submit(() -> {
                        try {
                            var packet = new Packet(
                                    name,
                                    imid,
                                    /* We should submit a null value to the database. */
                                    tagger.tag(buff).orElse(null));
                            return new TagResult(null, packet);
                        } catch (TaggingException e) {
                            return new TagResult(e, null);
                        }
                    }));
                }
            }
            statement.close();

            /* Every column needs a prepared statement for itself. */
            HashMap<String, PreparedStatement> statements = new HashMap<>();
            for(var tagger : taggers.keySet()) {
                var name = Database.taggerColumnName(tagger);
                var statementQuery0 = "update " + this.table + " set " + name + "=? where id=?";
                var statementQuery = connection.prepareStatement(statementQuery0);
                statements.put(tagger, statementQuery);
            }

            /* Collect the results and submit them to the database. */
            for (var future : futures) {
                var tag = future.get().getTag();

                var updateStatement = statements.get(tag.tag);
                assert(updateStatement != null);

                updateStatement.setObject(1, tag.data);
                updateStatement.setString(2, tag.id);

                updateStatement.execute();
            }


            for(var q : statements.entrySet())
                q.getValue().close();
        }
    }

    /** Represents a possible failure when running a tagger. */
    private static final class TagResult {
        private final TaggingException exception;
        private final Packet tag;

        public TagResult(TaggingException exception, Packet tag) {
            this.exception = exception;
            this.tag = tag;
        }

        public Packet getTag() throws TaggingException {
            assert(this.exception != null || this.tag != null);

            if(this.tag != null)
                return this.tag;
            else
                throw this.exception;
        }
    }

    /** Complete task packet. */
    private static final class Packet {
        public final String tag;
        public final String id;
        public final Object data;

        private Packet(String tag, String id, Object data) {
            this.tag = tag;
            this.id = id;
            this.data = data;
        }
    }
}
