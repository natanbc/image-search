package com.github.natanbc.imagesearch.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Selection {
    /* What selectors should this selection run? */
    protected final HashSet<Selector> selectors;

    protected Selection(HashSet<Selector> selectors) {
        this.selectors = selectors;
    }

    private static Selection fromSingleSelector(Selector s) {
        HashSet<Selector> selectors = new HashSet<>();
        selectors.add(s);

        return new Selection(selectors);
    }

    /** Performs this selection.
     * @param connection The connection that will be used for the query.
     * @param table The table that will be queried.
     * @param columns The columns that will be queried.
     * @return A set of {@link Selected} objects, each containing both the
     * statement and its result set, which should be used in try-with-value
     * statements.
     * @throws SQLException When creation of a selection fails.
     */
    protected Set<Selected> perform(Connection connection, String table, String columns) throws SQLException {
        HashSet<Selected> selected = new HashSet<>();
        for(var selector : this.selectors) {
            var statement = selector.getStatement(connection, table, columns);
            statement.execute();
            var results = statement.getResultSet();
            selected.add(new Selected(statement, results));
        }

        return selected;
    }

    /** Create a new selector, which selects any row. */
    public static Selection all() {
        return Selection.fromSingleSelector(new All());
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column equal that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for equality.
     */
    public static Selection equals(String column, Object value) {
        return Selection.fromSingleSelector(new Equal(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column differ from that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for inequality.
     */
    public static Selection differs(String column, Object value) {
        return Selection.fromSingleSelector(new NotEqual(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column are less than that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for size.
     */
    public static Selection lessThan(String column, Object value) {
        return Selection.fromSingleSelector(new Less(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column are greater than that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for size.
     */
    public static Selection greaterThan(String column, Object value) {
        return Selection.fromSingleSelector(new Greater(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column are less or equal than that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for size.
     */
    public static Selection lessThanOrEqualTo(String column, Object value) {
        return Selection.fromSingleSelector(new LessOrEqual(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column are greater or equal than that of the given element.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for size.
     */
    public static Selection greaterThanOrEqualTo(String column, Object value) {
        return Selection.fromSingleSelector(new GreaterOrEqual(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column and that of the given element are alike.
     *
     * @param column The column whose values will be tested.
     * @param value The value which will be compared against for likeness.
     */
    public static Selection like(String column, Object value) {
        return Selection.fromSingleSelector(new Like(column, value));
    }
    /** Create a new selector, which selects all rows whose value in the given
     * column is between the values of the given elements.
     *
     * @param column The column whose values will be tested.
     * @param a The value which will be used as a lower bound.
     * @param b The value which will be used as un upper bound.
     */
    public static Selection between(String column, Object a, Object b) {
        return Selection.fromSingleSelector(new Between(column, a, b));
    }

    /** Join the ranges of two selections into a new selection, leaving the
     * original ranges in both selections unchanged.
     *
     * @param other The second selection with which the ranges in this selection
     *              should be joined.
     * @return The selection whose range is the combination of both this and the
     * range found in {@code other}.
     */
    public Selection join(Selection other) {
        var selectors = new HashSet<>(this.selectors);
        selectors.addAll(other.selectors);

        return new Selection(selectors);
    }

    protected interface Selector {
        /** Get the current state of the object and return a prepared statement
         * for the selection it represents.
         * @param connection The SQL database connection the statement must be
         *                   created in.
         * @param table The table that will be queried.
         * @param columns The columns that will be queried.
         * @return A prepared statement, making a selection.
         * @throws SQLException When creation of the selection fails.
         */
        PreparedStatement getStatement(Connection connection, String table, String columns) throws SQLException;
    }
    /* Selector types. */
    protected static class All implements Selector {
        @Override
        public PreparedStatement getStatement(Connection connection, String table, String columns) throws SQLException {
            String query = String.format("select %s from %s", columns, table);
            return connection.prepareStatement(query);
        }
    }
    protected static class UnarySelector implements Selector {
        private final String column;
        private final String operator;
        private final Object target;

        public UnarySelector(String column, String operator, Object target) {
            this.column = column;
            this.operator = operator;
            this.target = target;
        }

        @Override
        public PreparedStatement getStatement(Connection connection, String table, String columns) throws SQLException {
            String query = String.format("select %s from %s where %s%s?", columns, table, this.column, this.operator);
            var stmt = connection.prepareStatement(query);
            stmt.setObject(1, this.target);

            return stmt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnarySelector that = (UnarySelector) o;
            return Objects.equals(column, that.column) &&
                Objects.equals(operator, that.operator) &&
                Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(column, operator, target);
        }
    }
    protected static final class Equal extends UnarySelector {
        public Equal(String column, Object target) {
            super(column, "=", target);
        }
    }
    protected static final class NotEqual extends UnarySelector {
        public NotEqual(String column, Object target) {
            super(column, "<>", target);
        }
    }
    protected static final class Greater extends UnarySelector {
        public Greater(String column, Object target) {
            super(column, ">", target);
        }
    }
    protected static final class Less extends UnarySelector {
        public Less(String column, Object target) {
            super(column, "<", target);
        }
    }
    protected static final class GreaterOrEqual extends UnarySelector {
        public GreaterOrEqual(String column, Object target) {
            super(column, ">=", target);
        }
    }
    protected static final class LessOrEqual extends UnarySelector {
        public LessOrEqual(String column, Object target) {
            super(column, "<=", target);
        }
    }
    protected static final class Like extends UnarySelector {
        public Like(String column, Object target) {
            super(column, " LIKE ", target);
        }
    }
    protected static final class Between implements Selector {
        private final String column;
        private final Object a;
        private final Object b;

        public Between(String column, Object a, Object b) {
            this.column = column;
            this.a = a;
            this.b = b;
        }

        @Override
        public PreparedStatement getStatement(Connection connection, String table, String columns) throws SQLException {
            String query = String.format("select %s from %s where %s between ? and ?", columns, table, this.column);
            var stmt = connection.prepareStatement(query);

            stmt.setObject(1, this.a);
            stmt.setObject(2, this.b);

            return stmt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Between between = (Between) o;
            return Objects.equals(column, between.column) &&
                Objects.equals(a, between.a) &&
                Objects.equals(b, between.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(column, a, b);
        }
    }

    protected static class Selected implements AutoCloseable {
        protected final PreparedStatement statement;
        protected final ResultSet resultSet;

        public Selected(PreparedStatement statement, ResultSet resultSet) {
            this.statement = statement;
            this.resultSet = resultSet;
        }

        public PreparedStatement getStatement() {
            return statement;
        }

        public ResultSet getResultSet() {
            return resultSet;
        }

        @Override
        public void close() throws SQLException {
            resultSet.close();
            statement.close();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Selected selected = (Selected) o;
            return Objects.equals(statement, selected.statement) &&
                Objects.equals(resultSet, selected.resultSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statement, resultSet);
        }
    }
}
