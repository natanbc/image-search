package com.github.natanbc.imagesearch.db;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Image {
    protected final UUID id;
    protected final Path path;
    protected final HashMap<String, Object> tags;

    public Image(UUID id, Path path, HashMap<String, Object> tags) {
        this.id = id;
        this.path = path;
        this.tags = tags;
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
