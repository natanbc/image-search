package com.github.natanbc.imagesearch;

import com.github.darkryu550.textextractor.TesseractExtractor;
import com.github.darkryu550.textextractor.TextBlock;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Index implements AutoCloseable {
    private static final String DB_FILE = "index.db";
    private final Connection connection;
    private final TesseractExtractor ocr;

    private Index(Connection connection) {
        this.connection = connection;
        this.ocr = new TesseractExtractor();
    }

    public static Index open() {
        return new Index(connect());
    }

    public void add(String path) throws Throwable {
        var image = Utils.readImage(path);
        var hash = Utils.hash(Utils.getData(image));
        if(exists(hash)) {
            System.err.println(path + ": Image already exists in index");
            return;
        }
        var p = Utils.copyToIndex(path, hash);
        var f = new File(path);
        var name = f.getName();

        /* Perform OCR extraction. */
        var text = new StringBuilder();
        for (var a : ocr.extractTextBlocks(ImageIO.read(f))) {
            text.append(a.getText());
        }

        executeSql(() -> {
            try(var s = connection.prepareStatement("insert into images values(?, ?, ?, ?)")) {
                s.setString(1, name);
                s.setString(2, hash);
                s.setString(3, text.toString());
                s.setString(4, p);
                s.execute();
            }
        });
        System.out.println("Added " + path + " (hash: " + hash + ") to index");
    }

    public void search(String text) {
        executeSql(() -> {
            try(var s = connection.prepareStatement("select * from images where text like ?")) {
                s.setString(1, text);
                try(var rs = s.executeQuery()) {
                    var found = false;
                    while(rs.next()) {
                        found = true;
                        System.out.println("Name: " + rs.getString("name"));
                        System.out.println("Hash: " + rs.getString("hash"));
                        System.out.println("Text: " + rs.getString("text"));
                        System.out.println("Path: " + rs.getString("path"));
                        System.out.println();
                    }
                    if(!found) {
                        System.out.println("No matching images found");
                    }
                }
            }
        });
    }

    public void remove(String hash) {
        if(!exists(hash)) {
            System.err.println("There's no image with hash " + hash + " on the index");
            return;
        }
        var path = executeSql(() -> {
            try(var s = connection.prepareStatement("select path from images where hash = ?")) {
                s.setString(1, hash);
                try(var rs = s.executeQuery()) {
                    rs.next();
                    return rs.getString("path");
                }
            }
        });
        Utils.rm(path);
        executeSql(() -> {
            try(var s = connection.prepareStatement("delete from images where hash = ?")) {
                s.setString(1, hash);
                s.execute();
            }
        });
    }

    public void list() {
        executeSql(() -> {
            try(var s = connection.prepareStatement("select * from images")) {
                try(var rs = s.executeQuery()) {
                    while(rs.next()) {
                        System.out.println("Name: " + rs.getString("name"));
                        System.out.println("Hash: " + rs.getString("hash"));
                        System.out.println("Text: " + rs.getString("text"));
                        System.out.println("Path: " + rs.getString("path"));
                        System.out.println();
                    }
                }
            }
        });
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing database file:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }

    private boolean exists(String hash) {
        return executeSql(() -> {
            try(var s = connection.prepareStatement("select * from images where hash = ?")) {
                s.setString(1, hash);
                try(var rs = s.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    private void executeSql(SQLClosure c) {
        executeSql(() -> { c.execute(); return null; });
    }

    private <T> T executeSql(SQLFunction<T> c) {
        try {
            return c.execute();
        } catch (SQLException e) {
            System.err.println("Error executing query:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }

    private interface SQLClosure {
        void execute() throws SQLException;
    }

    private interface SQLFunction<T> {
        T execute() throws SQLException;
    }

    private static Connection connect() {
        try {
            var connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            try(var s = connection.createStatement()) {
                s.execute("create table if not exists images(name string, hash string," +
                        "text string, path string)");
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("Unable to open database file:");
            e.printStackTrace();
            System.exit(1);
            throw new AssertionError(e);
        }
    }
}
