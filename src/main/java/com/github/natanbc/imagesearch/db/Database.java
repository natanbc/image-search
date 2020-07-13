package com.github.natanbc.imagesearch.db;

import com.github.darkryu550.imagesearch.Tagger;
import com.github.natanbc.imagesearch.db.pool.ConnectionPool;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

public class Database {
    public static final String IMAGES_TABLE = "images";
    protected final ConnectionPool database;

    /* Map of taggers, indexed by a given name. This name will be used for the
     * columns their tags will fill in the table. So keep them consistent. */
    protected HashMap<String, Tagger> taggers;

     /** Given an existing connection to a database, initialize this manager. */
    public Database(ConnectionPool database) throws InterruptedException, SQLException {
        this.database = database;
        this.taggers = new HashMap<>();

        /* Initialize the database if needed. */
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var query = String.format(
                    "create table if not exists %s(id STRING, path STRING)",
                    IMAGES_TABLE);
            statement.execute(query);
            statement.close();
        }
    }

    /** Registers a new tagger with the given name.
     * <br><br>
     * It is important to note that registering a tagger does not run it on the
     * entries that are already in the database, only registers it to be run on
     * new entries, as they are inserted. If you wish to run the tagger on all
     * the entries already present in the database, you must do so by using a
     * {@link Pass} object.
     *
     * @param key The name that will be given to the tagger. The same name will
     *            be used as the column name for the tags generated by it.
     * @param tagger The tagger object itself.
     * @return A {@link Pass} object that, when run, will execute the
     * registered tagger over the entire range of elements already present in
     * the database.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     */
    public Pass register(String key, Tagger tagger) throws InterruptedException, SQLException {
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var column = Database.taggerColumnName(key);

            /* We have to test and see if the table exists before we register it.
             * it would be better to do this in a transaction, but eh, too much
             * effort for what this is setting out to do. Nonetheless:
             * TODO: Make conditional new column addition into a transaction.
             */
            var query = String.format(
                    "alter table %s add column %s %s",
                    IMAGES_TABLE,
                    column,
                    tagger.getType().getName());
            var test = String.format(
                    "select %s from %s",
                    column,
                    IMAGES_TABLE);

            try {
                statement.execute(test);
            } catch(SQLException e) {
                /* Assume this error is caused by table not existing. */
                statement.execute(query);
            }
            statement.close();
        }

        this.taggers.put(key, tagger);
        /* Create the pass for this tagger, over the whole column. */
        var isolate = new HashMap<String, Tagger>();
        isolate.put(key, tagger);

        return new Pass(
            this.database,
            isolate,
            IMAGES_TABLE,
            null
        );
    }

    /** Adds a new image to the catalogue.
     * @return A {@link Pass} object that, when run, will execute all the
     * registered taggers on this new image.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     * @throws IOException When the image file could not be read or its absolute
     * path could not be acquired.
     */
    public Pass addImage(Path path) throws InterruptedException, SQLException, IOException {
        /* Get a new UUID, making sure it hasn't been used before. */
        UUID id;
        do {
            id = UUID.randomUUID();
        } while(this.getImageById(id).isPresent());

        /* Load the image to sure it is a valid image file. */
        var canonical = path.toAbsolutePath().normalize();
        ImageIO.read(canonical.toFile());

        /* It's valid, add the entry to the database. */
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var query = String.format(
                    "insert into %s(id, path) values (\"%s\", \"%s\")",
                    IMAGES_TABLE,
                    id.toString(),
                    path.toString());
            statement.execute(query);
            statement.close();
        }

        return new Pass(
            this.database,
            this.taggers,
            IMAGES_TABLE,
            String.format("id=\"%s\"", id));
    }

    /** Queries for an image given its UUID value.
     * @return A description of the requested image in the form of {@link Image},
     * if any could be found.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     * */
    public Optional<Image> getImageById(UUID id) throws InterruptedException, SQLException {
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var query = String.format(
                    "select * from %s where id=\"%s\"",
                    IMAGES_TABLE,
                    id.toString());
            if (!statement.execute(query))
                /* This statement having failed to execute is a bug. */
                throw new RuntimeException(
                        "Could not submit query: " + query,
                        statement.getWarnings());

            var result = statement.getResultSet();
            if (!result.next())
                return Optional.empty();

            var uuid_str = result.getString("id");
            var path_str = result.getString("path");
            if (!id.toString().equals(uuid_str))
                throw new RuntimeException("Entry queried with ID has different " +
                        "ID value: Expected \"" + id.toString() + "\", got \""
                        + uuid_str + "\"");

            if (path_str == null)
                /* This statement having failed to execute is a bug. */
                throw new RuntimeException("Required field \"path\" has a null value");

            HashMap<String, Object> tags = new HashMap<>(this.taggers.size());
            for (var name : this.taggers.keySet()) {
                var column = Database.taggerColumnName(name);
                var value = result.getObject(column);

                tags.put(name, value);
            }
            statement.close();

            return Optional.of(new Image(id, Path.of(path_str), tags));
        }
    }

    /** Queries all of the registered images.
     * @return A set of all the images in the database, described as
     * {@link Image} objects.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     */
    public Set<Image> getImages() throws InterruptedException, SQLException {
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var query = String.format(
                    "select * from %s",
                    IMAGES_TABLE);
            if (!statement.execute(query))
                /* This statement having failed to execute is a bug. */
                throw new RuntimeException(
                        "Could not submit query: " + query,
                        statement.getWarnings());

            var result = statement.getResultSet();
            var images = new HashSet<Image>();
            while (result.next()) {
                var uuid_str = result.getString("id");
                var path_str = result.getString("path");

                if (uuid_str == null || path_str == null) {
                    /* This statement having failed to execute is a bug. */
                    throw new RuntimeException("Required field \"path\" has a null value");
                }
                var uuid = UUID.fromString(uuid_str);

                HashMap<String, Object> tags = new HashMap<>(this.taggers.size());
                for (var name : this.taggers.keySet()) {
                    var column = Database.taggerColumnName(name);
                    var value = result.getObject(column);

                    tags.put(name, value);
                }

                images.add(new Image(uuid, Path.of(path_str), tags));
            }

            statement.close();
            return images;
        }
    }

    /** Queries the database, searching for all the entries with the given path.
     * @return A set of all the entries that have the given path, described as
     * {@link Image} objects.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     */
    public Set<Image> getImageByPath(Path path) throws InterruptedException, SQLException {
        try(var handle = this.database.take()) {
            var connection = handle.getConnection();
            var statement = connection.createStatement();
            var query = String.format(
                    "select * from %s where path=\"%s\"",
                    IMAGES_TABLE,
                    path.toString());
            if (!statement.execute(query))
                /* This statement having failed to execute is a bug. */
                throw new RuntimeException(
                        "Could not submit query: " + query,
                        statement.getWarnings());

            var result = statement.getResultSet();
            var images = new HashSet<Image>();
            while (result.next()) {
                var uuid_str = result.getString("id");
                var path_str = result.getString("path");

                if (uuid_str == null || path_str == null) {
                    /* This statement having failed to execute is a bug. */
                    throw new RuntimeException("Required field \"path\" has a null value");
                }
                var uuid = UUID.fromString(uuid_str);

                HashMap<String, Object> tags = new HashMap<>(this.taggers.size());
                for (var name : this.taggers.keySet()) {
                    var column = Database.taggerColumnName(name);
                    var value = result.getObject(column);

                    tags.put(name, value);
                }

                images.add(new Image(uuid, Path.of(path_str), tags));
            }

            statement.close();
            return images;
        }
    }

    /** Given the name of a tagger, format it into its column name. */
    public static String taggerColumnName(String name) {
        return "tag$" + name;
    }
}