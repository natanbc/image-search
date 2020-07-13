package com.github.natanbc.imagesearch;

import com.github.darkryu550.imagesearch.TaggingException;
import com.github.darkryu550.imagesearch.frequency.FrequencyBand;
import com.github.darkryu550.textextractor.TesseractTagger;
import com.github.natanbc.imagesearch.db.Database;
import com.github.natanbc.imagesearch.db.pool.SingleConnectionPool;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NewMain {
    public static void main(String[] args)
        throws SQLException, IOException, InterruptedException, ExecutionException, TaggingException {

        var executor = Executors.newCachedThreadPool();
        var connection = new SingleConnectionPool(connect("./index.db"));
        var database = new Database(connection);

        try {
            database.register("frequencyBand", new FrequencyBand());
            database.register("tesseract", new TesseractTagger());
        } catch (InterruptedException e) {
            System.err.println("Could not register database tagger:");
            e.printStackTrace();
            System.exit(1);
        }
        database.addImage(Path.of("images/final_project.png")).runOn(executor);

        /* Cleanup. */
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        connection.close();

        /* The cached thread pool may keep threads alive for 60 more seconds.
         * After we're sure all tasks have been terminated, we can safely just
         * exit and let those those threads get killed. */
        System.exit(0);
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