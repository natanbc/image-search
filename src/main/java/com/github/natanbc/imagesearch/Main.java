package com.github.natanbc.imagesearch;

import java.util.Arrays;
import java.util.List;

public class Main {
    private static final List<String> COMMANDS = List.of(
            "help",
            "add",
            "search",
            "remove",
            "list"
    );

    public static void main(String[] args) throws ReflectiveOperationException {
        if(args.length == 0 || !COMMANDS.contains(args[0].toLowerCase())) {
            help(null);
            return;
        }
        Main.class.getDeclaredMethod(args[0].toLowerCase(), String[].class)
                .invoke(null, (Object)Arrays.copyOfRange(args, 1, args.length));
    }

    private static void help(String[] unused) {
        System.err.println("Usage: image-search.jar <add [path]/search [query]/remove [hash]/list>");
        System.err.println("  add: Adds images to the index");
        System.err.println("  search: Searches for an image based on text");
        System.err.println("  remove: Removes images from the index");
        System.err.println("  list: Lists all images in the index");
        System.err.println();
        System.err.println("  Search queries must be valid arguments for the SQLite 'like' operator,");
        System.err.println("  for example: `java -jar image-search.jar search '%world%'`");
    }

    private static void add(String[] args) {
        try(var index = Index.open()) {
            for(var path : args) {
                System.out.println("Adding " + path);
                index.add(path);
            }
        }
    }

    private static void search(String[] args) {
        try(var index = Index.open()) {
            for(var search : args) {
                System.out.println("Searching for " + search);
                index.search(search);
                System.out.println();
            }
        }
    }

    private static void remove(String[] args) {
        try(var index = Index.open()) {
            for(var hash : args) {
                System.out.println("Removing " + hash);
                index.remove(hash);
            }
        }
    }

    private static void list(String[] unused) {
        try(var index = Index.open()) {
            index.list();
        }
    }
}
