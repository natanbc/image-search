package com.github.natanbc.imagesearch.db;

import com.github.darkryu550.imagesearch.Tagger;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Image {
    protected final UUID id;
    protected final Path path;
    protected final HashMap<String, Object> tags;

    public Image(UUID id, Path path, HashMap<String, Object> tags) {
        this.id = id;
        this.path = path;
        this.tags = tags;
    }

    /** Tries to get an image from a result set.
     *
     * @param set The result set.
     * @param taggers The set of taggers whose values will be queried.
     * @return The value of the next image, if any.
     * @throws SQLException In case of an SQL error.
     * @throws IllegalArgumentException When the next element in the given
     * {@link ResultSet} does not store a valid {@link Image}.
     */
    protected static Optional<Image> fromResultSet(ResultSet set, HashMap<String, Tagger> taggers) throws SQLException {
        var uuid_str = set.getString("id");
        var path_str = set.getString("path");
        if (uuid_str == null)
            throw new IllegalArgumentException("Required field \"id\" has a null value");

        if (path_str == null)
            /* This statement having failed to execute is a bug. */
            throw new IllegalArgumentException("Required field \"path\" has a null value");

        HashMap<String, Object> tags = new HashMap<>(taggers.size());
        for (var name : taggers.keySet()) {
            var column = Database.taggerColumnName(name);
            var value = set.getObject(column);

            tags.put(name, value);
        }

        return Optional.of(new Image(UUID.fromString(uuid_str), Path.of(path_str), tags));
    }

    public UUID getId() {
        return this.id;
    }

    public Path getPath() {
        return path;
    }

    public Optional<Object> getTag(String name) {
        return Optional.ofNullable(this.tags.get(name));
    }

    public Map<String, Object> getTags() { return this.tags; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return Objects.equals(id, image.id) &&
            Objects.equals(path, image.path) &&
            Objects.equals(tags, image.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path, tags);
    }
}
