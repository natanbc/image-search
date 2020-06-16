package com.github.natanbc.imagesearch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        var options = new Options();
        options.addOption("h", "help", false, "Show the help message");
        options.addOption("d", "data-path", true, "Path to where data should be stored." +
                "Defaults to the current working directory");
        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            System.exit(1);
            return;
        }
        if(cmd.getArgs().length == 0 || cmd.hasOption('h')) {
            printHelp(options);
            return;
        }
        var functionArguments = Arrays.copyOfRange(cmd.getArgs(), 1, cmd.getArgs().length);
        var path = cmd.hasOption('d') ? cmd.getOptionValue('d') : ".";
        validatePath(Path.of(path));
        switch (cmd.getArgs()[0]) {
            case "add" -> add(path, functionArguments);
            case "list" -> list(path);
            case "remove" -> remove(path, functionArguments);
            case "search" -> search(path, functionArguments);
            default -> printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        new HelpFormatter().printHelp("image-search.jar <add <path>/list/remove <hash>/search <query>>", options);
        System.out.println("Search queries must be valid arguments for the SQLite 'like' operator,");
        System.out.println("for example: `java -jar image-search.jar search \"%text%\"`");
    }

    private static void add(String path, String[] args) {
        try(var index = Index.open(path)) {
            for(var image : args) {
                System.out.println("Adding " + image);

                try {
                    index.add(image);
                } catch (Throwable throwable) {
                    System.err.println("Could not add image " + image);
                    throwable.printStackTrace();
                }
            }
        }
    }

    private static void list(String path) {
        try(var index = Index.open(path)) {
            index.list();
        }
    }

    private static void remove(String path, String[] args) {
        try(var index = Index.open(path)) {
            for(var hash : args) {
                System.out.println("Removing " + hash);
                index.remove(hash);
            }
        }
    }

    private static void search(String path, String[] args) {
        try(var index = Index.open(path)) {
            for(var search : args) {
                System.out.println("Searching for " + search);
                index.search(search);
                System.out.println();
            }
        }
    }

    private static void validatePath(Path path) {
        if(Files.exists(path) && !Files.isDirectory(path)) {
            System.out.println("Invalid data path: " + path);
            System.exit(1);
        }
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("Unable to create data path:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
