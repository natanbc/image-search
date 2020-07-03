package com.github.darkryu550.imagesearch;

import java.awt.image.BufferedImage;
import java.sql.SQLType;
import java.util.Optional;

/** A thread-safe tagger.*/
public interface Tagger {
    /** The SQL type of the objects produced by this tagger. */
    SQLType getType();

    /** Given a {@link BufferedImage}, try to produce a tag from it, which can
     * then be passed on to a SQL database or used as is.
     * <br><br>
     * Note that this function must be thread safe.
     * @return A SQL-compatible object tagging the given image is possible with
     * this tagger, no value otherwise.
     * @throws TaggingException When the image could have received a tag but did
     * not because of an error unrelated to the nature of the image.
     */
    Optional<Object> tag(BufferedImage image) throws TaggingException;
}
