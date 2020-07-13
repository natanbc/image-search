package com.github.natanbc.imagesearch;

import com.github.darkryu550.imagesearch.frequency.FrequencyBand;
import com.github.darkryu550.imagesearch.magnitude.*;
import com.github.darkryu550.textextractor.TesseractTagger;
import com.github.natanbc.imagesearch.db.Database;
import com.github.natanbc.imagesearch.db.Image;
import com.github.natanbc.imagesearch.db.Selection;
import com.github.natanbc.imagesearch.db.pool.SingleConnectionPool;

import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NewMain implements AutoCloseable {
    /* After how many characters should a long string get elided? */
    private static final int CHARACTER_ELISION = 60;

    protected final ExecutorService executor;
    protected final SingleConnectionPool connection;
    protected final Database database;

    protected NewMain()
        throws SQLException, InterruptedException {

        this.executor   = Executors.newCachedThreadPool();
        this.connection = new SingleConnectionPool(connect("./index.db"));
        this.database   = new Database(connection);
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        connection.close();
    }

    public static void main(String[] args) {
        try(var main = new NewMain()) {
            /* Start by registering our taggers into the database. */
            try {
                main.register();
            } catch (InterruptedException | SQLException e) {
                System.err.println("Could not register database tagger:");
                e.printStackTrace();
                System.exit(1);
            }

            main.printImageSummaryFromSelection(Selection.all(), System.out);
        } catch (Exception e) {
            /* General top-level error stop. Generally it's a better idea to
             * catch errors in their local contexts in order to provide more
             * interesting contextual information. */
            System.err.println("Caught top level fatality:");
            e.printStackTrace();
            e.printStackTrace();
            System.exit(1);
        }

        /* The cached thread pool may keep threads alive for 60 more seconds.
         * After we're sure all tasks have been terminated, we can safely just
         * exit and let those those threads get killed. */
        System.exit(0);
    }

    /** Pretty-print the contents of all the images in a selection.
     *
     * @param selection The selection that will be queried.
     * @param target The writer on to which the data will be printed.
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     */
    protected void printImageSummaryFromSelection(Selection selection, PrintStream target)
        throws SQLException, InterruptedException {

        for(var image : this.database.getImages(selection)) {
            printImageSummary(image, target);
        }
    }

    /** Pretty-print the contents of an image entry, in a summed up way.
     * @param image The image whose contents are to be pretty-printed.
     * @param target The writer on to which the data will be printed.
     */
    protected void printImageSummary(Image image, PrintStream target) {
        target.println();
        target.printf("Image ID:   %s\n", image.getId().toString());
        target.printf("Image path: %s\n", image.getPath().toString());
        target.println("Tags:");

        /* This is a surprise tool that will help us later. */
        var longestTagLength = 0;
        for(var entry : image.getTags().keySet())
            if(entry.length() > longestTagLength)
                longestTagLength = entry.length();

        for(var entry : image.getTags().entrySet()) {
            var missing = longestTagLength - entry.getKey().length();
            target.printf("    %s:%s ", entry.getKey(), " ".repeat(missing));

            String value;
            if(entry.getValue() == null)
                value = "NULL";
            else
                value = entry.getValue().toString();

            var elided = false;
            if(value.contains("\n")) {
                value = value.substring(0, value.indexOf("\n"));
                elided = true;
            }
            if(value.contains("\r")) {
                value = value.substring(0, value.indexOf("\n"));
                elided = true;
            }
            if(value.length() > NewMain.CHARACTER_ELISION) {
                value = value.substring(0, NewMain.CHARACTER_ELISION);
                elided = true;
            }
            if(elided) value += " (...)";

            target.println(value);
        }
    }

    /** Register the taggers in the database.
     *
     * @throws InterruptedException When a connection to the database could not
     * be acquired from the pool.
     * @throws SQLException Upon failure of a SQL operation.
     */
    protected void register() throws InterruptedException, SQLException {
        database.register("frequencyBand", new FrequencyBand());
        database.register("tesseract", new TesseractTagger());
        database.register("haralickContrast", new HaralickContrast());
        database.register("haralickCorrelation", new HaralickCorrelation());
        database.register("haralickEnergy", new HaralickEnergy());
        database.register("haralickEntropy", new HaralickEntropy());
        database.register("haralickHomogeneity", new HaralickHomogeneity());
        database.register("haralickMaxProb", new HaralickMaximumProbability());
        database.register("histogram", new Histogram());
    }

    private static Connection connect(String path) {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + Path.of(path));
        } catch (SQLException e) {
            System.err.println("Unable to open database file:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }


}
