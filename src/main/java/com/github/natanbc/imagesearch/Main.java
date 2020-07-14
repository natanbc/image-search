package com.github.natanbc.imagesearch;

import com.github.darkryu550.imagesearch.frequency.FrequencyBand;
import com.github.darkryu550.imagesearch.magnitude.*;
import com.github.darkryu550.textextractor.TesseractTagger;
import com.github.natanbc.imagesearch.db.Database;
import com.github.natanbc.imagesearch.db.Image;
import com.github.natanbc.imagesearch.db.Selection;
import com.github.natanbc.imagesearch.db.pool.SingleConnectionPool;
import picocli.CommandLine;

import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@CommandLine.Command(
    name = "image-search",
    mixinStandardHelpOptions = true,
    description = "Search and managed a database of images")
public class Main implements AutoCloseable {
    /* After how many characters should a long string get elided? */
    private static final int CHARACTER_ELISION = 60;

    protected final ExecutorService executor;
    protected final SingleConnectionPool connection;
    protected final Database database;

    protected Main()
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
        try(var main = new Main()) {
            /* Start by registering our taggers into the database. */
            try {
                main.register();
            } catch (InterruptedException | SQLException e) {
                System.err.println("Could not register database tagger:");
                e.printStackTrace();
                System.exit(1);
            }

            var result = new CommandLine(main)
                .addSubcommand(main.getAddSubcommand())
                .addSubcommand(main.getQuerySubcommand())
                .addSubcommand(main.getGetSubcommand())
                .addSubcommand(main.getPassSubcommand())
                .addSubcommand(main.getClosestSubcommand())
                .execute(args);

            /* The cached thread pool may keep threads alive for 60 more seconds.
             * After we're sure all tasks have been terminated, we can safely just
             * exit and let those those threads get killed. */
            System.exit(result);
        } catch (Exception e) {
            /* General top-level error stop. Generally it's a better idea to
             * catch errors in their local contexts in order to provide more
             * interesting contextual information. */
            System.err.println("Caught top level fatality:");
            e.printStackTrace();
            e.printStackTrace();
            System.exit(1);
        }
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
            if(value.length() > Main.CHARACTER_ELISION) {
                value = value.substring(0, Main.CHARACTER_ELISION);
                elided = true;
            }
            if(elided) value += " (...)";

            target.println(value);
        }
    }

    @CommandLine.Command(
        name = "add",
        mixinStandardHelpOptions = true,
        description = "Adds a new image to the database")
    protected class Add implements Callable<Integer> {
        @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "The image to be added")
        protected Path file;
        @CommandLine.Option(names = { "-s", "--skip-tagging" }, description = "Don't run any taggers on the added image")
        protected boolean skipTagging = false;

        @Override
        public Integer call() throws Exception {
            var selection = database.addImage(this.file);
            if(!this.skipTagging)
                database.getPassWithAllTaggers().runOn(executor, selection);

            Main.this.printImageSummaryFromSelection(selection, System.out);
            return 0;
        }
    }
    protected Add getAddSubcommand() { return new Add(); }

    /** Convert a string into a selection, the string has the following form:
     *      [Column Name][Whitespace]*[Operator][Whitespace]*[Value]
     * Where [Column Name] is the string representing the column name,
     * [Value] is the string that will be converted to the column's value and
     * [Operator] is one of the following:
     *      - {@code =}  Equality
     *      - {@code <>} Inequality
     *      - {@code <}  Less than
     *      - {@code >}  Greater than
     *      - {@code >=} Greater than or equal to
     *      - {@code <=} Less than or equal to
     *      - {@code ~}  Likeness
     *      - {@code //} Between
     *      - {@code *} All, in this case, all other values are ignored.
     */
    protected Selection selectionFromString(String s) {
        String  operator = null;
        Integer index = null;

        var e = new IllegalArgumentException("More than one operator");
        var i = s.indexOf(">=");
        if(i != -1) { operator = ">="; index = i; }
        i = s.indexOf("<=");
        if(i != -1) { if(operator != null) throw e; operator = "<="; index = i; }
        i = s.indexOf("<>");
        if(i != -1) { if(operator != null) throw e; operator = "<>"; index = i; }
        i = s.indexOf("//");
        if(i != -1) { if(operator != null) throw e; operator = "//"; index = i; }
        i = s.indexOf("~");
        if(i != -1) { if(operator != null) throw e; operator = "~"; index = i; }
        i = s.indexOf("=");
        if(i != -1) { if(operator != null) throw e; operator = "="; index = i; }
        i = s.indexOf(">");
        if(i != -1) { if(operator != null) throw e; operator = ">"; index = i; }
        i = s.indexOf("<");
        if(i != -1) { if(operator != null) throw e; operator = "<"; index = i; }
        i = s.indexOf("*");
        if(i != -1) { if(operator != null) throw e; operator = "*"; index = i; }

        /* Checking for the operator also covers for the index. */
        if(operator == null)
            throw new IllegalArgumentException("No valid operator found in expression");

        /* Catch the wildcard. */
        if(operator.equals("*"))
            return Selection.all();

        var lhs = s.substring(0, index).strip();
        var rhs = s.substring(index + operator.length()).strip();

        var src = this.database.getTaggers().get(lhs);
        if(src == null)
            throw new IllegalArgumentException("No registered tagger matches \"" + lhs + "\"");
        var tag = src.getTagFromString(rhs);
        var col = Database.taggerColumnName(lhs);

        Selection sel;
        switch(operator) {
            case "="  -> sel = Selection.equals(col, tag);
            case "<"  -> sel = Selection.lessThan(col, tag);
            case ">"  -> sel = Selection.greaterThan(col, tag);
            case "~"  -> sel = Selection.like(col, tag);
            case "<>" -> sel = Selection.differs(col, tag);
            case "<=" -> sel = Selection.lessThanOrEqualTo(col, tag);
            case ">=" -> sel = Selection.greaterThanOrEqualTo(col, tag);
            case "//" -> throw new RuntimeException("TODO: Implement between operator");
            default -> throw new RuntimeException("Invalid operator " + operator);
        }

        return sel;
    }

    @CommandLine.Command(
        name = "query",
        mixinStandardHelpOptions = true,
        description = "Queries a set of images from the database")
    protected class Query implements Callable<Integer> {
        @CommandLine.Option(names = { "-s", "--select" }, description = "Make a selection")
        protected String[] selections;

        @Override
        public Integer call() throws Exception {
            var selection = Selection.none();
            if(this.selections != null && this.selections.length > 0) {
                for (var s : this.selections) {
                    Selection sel = selectionFromString(s);
                    selection = selection.join(sel);
                }
            } else
                /* Default to selecting everything. */
                selection = Selection.all();

            Main.this.printImageSummaryFromSelection(selection, System.out);
            return 0;
        }
    }
    protected Query getQuerySubcommand() { return new Query(); }

    @CommandLine.Command(
        name = "get",
        mixinStandardHelpOptions = true,
        description = "Get one of the tags from an image in the database")
    protected class Get implements Callable<Integer> {
        @CommandLine.Parameters(index = "0", paramLabel = "UUID", description = "The ID of the image")
        protected UUID id;
        @CommandLine.Parameters(index = "1", paramLabel = "TAG", description = "Name of the tag to fetch")
        protected String tag;

        @Override
        public Integer call() throws Exception {
            var selection = Selection.equals("id", this.id);
            var images = database.getImages(selection);

            if(images.size() <= 0) {
                System.err.println("Could not find image with ID " + this.id.toString());
                return 1;
            }
            if(images.size() > 1)
                throw new RuntimeException("More than one image has the same UUID");

            for(var image : images) {
                var tag = image.getTag(this.tag);
                if(tag.isPresent())
                    System.out.println(tag.get().toString());
                else {
                    System.err.println("Could not find tag " + this.tag);
                    return 1;
                }
            }
            return 0;
        }
    }
    protected Get getGetSubcommand() { return new Get(); }

    @CommandLine.Command(
        name = "distances",
        mixinStandardHelpOptions = true,
        description = "Get the ordered list of images closest to a given image")
    protected class Closest implements Callable<Integer> {
        @CommandLine.Parameters(index = "0", paramLabel = "UUID", description = "The ID of the image")
        protected UUID id;
        @CommandLine.Parameters(index = "1", paramLabel = "TAG", description = "Name of the tag to fetch")
        protected String tag;
        @CommandLine.Option(names = { "-n", "--number" }, description = "Number of closest images to show")
        protected Integer number;

        @Override
        public Integer call() throws Exception {
            var selection = Selection.equals("id", this.id);
            var images = database.getImages(selection);

            if(!database.getTaggers().containsKey(this.tag)) {
                System.err.println("The given tagger has not been registered");
                return 1;
            }
            if(images.size() <= 0) {
                System.err.println("Could not find image with ID " + this.id.toString());
                return 1;
            }
            if(images.size() > 1)
                throw new RuntimeException("More than one image has the same UUID");

            TagBundle current = null;
            for(var image : images) {
                var tag = image.getTag(this.tag).orElse(null);
                current = new TagBundle(image, tag);
            }

            ArrayList<TagBundle> others = new ArrayList<>(images.size());
            for(var image : database.getImages(Selection.differs("id", this.id))) {
                var tag = image.getTag(this.tag).orElse(null);
                others.add(new TagBundle(image, tag));
            }

            var tagger = database.getTaggers().get(this.tag);
            ArrayList<Future<Optional<DistanceBundle>>> futures = new ArrayList<>(others.size());
            for(var bundle : others) {
                TagBundle finalCurrent = current;
                futures.add(executor.submit(() -> {
                    var distance = tagger.getTagDistance(finalCurrent.tag, bundle.tag);
                    return distance.map((d) -> new DistanceBundle(bundle.image, d));
                }));
            }
            ArrayList<DistanceBundle> distances = futures.stream()
                .map((future) -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    return Optional.ofNullable((DistanceBundle) null);
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(a -> a.distance))
                .collect(Collectors.toCollection(ArrayList::new));

            var len = distances.size();
            if(this.number != null) len = this.number;
            len = Math.min(len, distances.size());

            for(int i = 0; i < len; ++i) {
                var bundle = distances.get(i);
                System.out.printf("[%d/%d] With a distance of %f", i + 1, len, bundle.distance);
                Main.this.printImageSummary(bundle.image, System.out);
            }

            return 0;
        }

        private final class TagBundle {
            public final Image image;
            public final Object tag;

            private TagBundle(Image image, Object tag) {
                this.image = image;
                this.tag = tag;
            }
        }

        private final class DistanceBundle {
            public final Image image;
            public final Double distance;

            private DistanceBundle(Image image, Double distance) {
                this.image = image;
                this.distance = distance;
            }
        }
    }
    protected Closest getClosestSubcommand() { return new Closest(); }

    @CommandLine.Command(
        name = "pass",
        mixinStandardHelpOptions = true,
        description = "Pass a set of taggers over a selection of elements in the database")
    protected class Pass implements Callable<Integer> {
        @CommandLine.Option(names = { "-t", "--tagger" }, description = "Specify a tagger")
        protected HashSet<String> taggers;
        @CommandLine.Option(names = { "-s", "--select" }, description = "Make a selection")
        protected String[] selections;

        @Override
        public Integer call() throws Exception {
            var selection = Selection.all();
            if(this.selections != null && this.selections.length > 0) {
                selection = Selection.none();
                for (var s : this.selections) {
                    Selection sel = selectionFromString(s);
                    selection = selection.join(sel);
                }
            }

            var pass = database.getPassWithAllTaggers();
            if(this.taggers != null && this.taggers.size() > 0)
                pass = database.getPassForFilteredTaggers((name, tagger) -> {
                    var cont = taggers.contains(name);
                    if(cont) taggers.remove(name);

                    return cont;
                });

            pass.runOn(executor, selection);
            return 0;
        }
    }
    protected Pass getPassSubcommand() { return new Pass(); }

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
