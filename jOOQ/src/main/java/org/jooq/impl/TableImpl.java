/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package org.jooq.impl;

import static org.jooq.Clause.TABLE;
import static org.jooq.Clause.TABLE_ALIAS;
import static org.jooq.Clause.TABLE_REFERENCE;
import static org.jooq.SQLDialect.FIREBIRD;
// ...
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.Keywords.K_TABLE;

import java.util.Arrays;
import java.util.EnumSet;

import org.jooq.Clause;
import org.jooq.Context;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.tools.StringUtils;

/**
 * A common base type for tables
 * <p>
 * This type is for JOOQ INTERNAL USE only. Do not reference directly
 *
 * @author Lukas Eder
 */
public class TableImpl<R extends Record> extends AbstractTable<R> {

    private static final long                serialVersionUID               = 261033315221985068L;
    private static final Clause[]            CLAUSES_TABLE_REFERENCE        = { TABLE, TABLE_REFERENCE };
    private static final Clause[]            CLAUSES_TABLE_ALIAS            = { TABLE, TABLE_ALIAS };
    private static final EnumSet<SQLDialect> NO_SUPPORT_QUALIFIED_TVF_CALLS = EnumSet.of(POSTGRES);

    final Fields<R>                          fields;
    final Alias<Table<R>>                    alias;

    protected final Field<?>[]               parameters;

    /**
     * @deprecated - 3.10 - [#5996] - Use {@link #TableImpl(Name)} instead (or
     *             re-generated your code).
     */
    @Deprecated
    public TableImpl(String name) {
        this(DSL.name(name));
    }

    /**
     * @deprecated - 3.10 - [#5996] - Use {@link #TableImpl(Name, Schema)}
     *             instead (or re-generated your code).
     */
    @Deprecated
    public TableImpl(String name, Schema schema) {
        this(DSL.name(name), schema);
    }

    /**
     * @deprecated - 3.10 - [#5996] - Use {@link #TableImpl(Name, Schema, Table)}
     *             instead (or re-generated your code).
     */
    @Deprecated
    public TableImpl(String name, Schema schema, Table<R> aliased) {
        this(DSL.name(name), schema, aliased);
    }

    /**
     * @deprecated - 3.10 - [#5996] - Use {@link #TableImpl(Name, Schema, Table, Field[])}
     *             instead (or re-generated your code).
     */
    @Deprecated
    public TableImpl(String name, Schema schema, Table<R> aliased, Field<?>[] parameters) {
        this(DSL.name(name), schema, aliased, parameters);
    }

    /**
     * @deprecated - 3.10 - [#5996] - Use {@link #TableImpl(Name, Schema, Table, Field[], String)}
     *             instead (or re-generated your code).
     */
    @Deprecated
    public TableImpl(String name, Schema schema, Table<R> aliased, Field<?>[] parameters, String comment) {
        this(DSL.name(name), schema, aliased, parameters, comment);
    }

    public TableImpl(Name name) {
        this(name, null, null, null, null);
    }

    public TableImpl(Name name, Schema schema) {
        this(name, schema, null, null, null);
    }

    public TableImpl(Name name, Schema schema, Table<R> aliased) {
        this(name, schema, aliased, null, null);
    }

    public TableImpl(Name name, Schema schema, Table<R> aliased, Field<?>[] parameters) {
        this(name, schema, aliased, parameters, null);
    }

    public TableImpl(Name name, Schema schema, Table<R> aliased, Field<?>[] parameters, String comment) {
        super(name, schema, comment);

        this.fields = new Fields<R>();

        if (aliased != null)
            alias = new Alias<Table<R>>(aliased, name);
        else
            alias = null;

        this.parameters = parameters;
    }

    /**
     * Get the aliased table wrapped by this table
     */
    Table<R> getAliasedTable() {
        if (alias != null) {
            return alias.wrapped();
        }

        return null;
    }

    @Override
    final Fields<R> fields0() {
        return fields;
    }

    @Override
    public final Clause[] clauses(Context<?> ctx) {
        return alias != null ? CLAUSES_TABLE_ALIAS : CLAUSES_TABLE_REFERENCE;
    }

    @Override
    public final void accept(Context<?> ctx) {
        if (alias != null) {
            ctx.visit(alias);
        }
        else {















            accept0(ctx);
        }
    }

    private void accept0(Context<?> ctx) {
        if (ctx.qualify() &&
                (!NO_SUPPORT_QUALIFIED_TVF_CALLS.contains(ctx.family()) || parameters == null || ctx.declareTables())) {
            Schema mappedSchema = Tools.getMappedSchema(ctx.configuration(), getSchema());

            if (mappedSchema != null) {
                ctx.visit(mappedSchema);
                ctx.sql('.');
            }
        }

        ctx.visit(Tools.getMappedTable(ctx.configuration(), this).getUnqualifiedName());

        if (parameters != null && ctx.declareTables()) {

            // [#2925] Some dialects don't like empty parameter lists
            if (ctx.family() == FIREBIRD && parameters.length == 0)
                ctx.visit(new QueryPartList<Field<?>>(parameters));
            else
                ctx.sql('(')
                   .visit(new QueryPartList<Field<?>>(parameters))
                   .sql(')');
        }
    }

    /**
     * Subclasses may override this method to provide custom aliasing
     * implementations
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Table<R> as(Name as) {
        if (alias != null) {
            return alias.wrapped().as(as);
        }
        else {
            return new TableAlias<R>(this, as);
        }
    }

    /**
     * Subclasses may override this method to provide custom aliasing
     * implementations
     * <p>
     * {@inheritDoc}
     */
    @Override
    public Table<R> as(Name as, Name... fieldAliases) {
        if (alias != null) {
            return alias.wrapped().as(as, fieldAliases);
        }
        else {
            return new TableAlias<R>(this, as, fieldAliases);
        }
    }

    public Table<R> rename(String rename) {
        return new TableImpl<R>(rename, getSchema());
    }

    public Table<R> rename(Name rename) {
        return new TableImpl<R>(rename, getSchema());
    }

    /**
     * Subclasses must override this method if they use the generic type
     * parameter <R> for other types than {@link Record}
     * <p>
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends R> getRecordType() {
        return (Class<? extends R>) RecordImpl.class;
    }

    @Override
    public boolean declaresTables() {
        return (alias != null) || (parameters != null) || super.declaresTables();
    }

    // ------------------------------------------------------------------------
    // XXX: Object API
    // ------------------------------------------------------------------------

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        // [#2144] TableImpl equality can be decided without executing the
        // rather expensive implementation of AbstractQueryPart.equals()
        if (that instanceof TableImpl) {
            TableImpl<?> other = (TableImpl<?>) that;
            return
                StringUtils.equals(getSchema(), other.getSchema()) &&
                StringUtils.equals(getName(), other.getName()) &&
                Arrays.equals(parameters, other.parameters);
        }

        return super.equals(that);
    }
}
