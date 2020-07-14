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

    /** Given a {@link String} representing a tag from this tagger, try to
     * build a tag object that's equivalent to it.
     *
     * @param value The string representation of the tag.
     * @throws IllegalArgumentException If the given string value does not
     * contain a valid representation in this tag format or if not enough
     * information is present for the creation of the tag object.
     * @return The tag that most closely matches the given input string.
     */
    Object getTagFromString(String value);

    /** Given two tag objects, calculate a distance value between them.
     * @param a The origin tag.
     * @param b The destination tag.
     * @throws IllegalArgumentException If either tag objects are not valid
     * tag objects.
     * @return The signed distance from the first object to the second object,
     * if any can be computed.
     */
    Optional<Double> getTagDistance(Object a, Object b);
}
