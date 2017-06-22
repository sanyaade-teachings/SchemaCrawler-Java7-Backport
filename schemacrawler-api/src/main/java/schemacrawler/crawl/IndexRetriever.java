/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2017, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.crawl;


import static java.util.Objects.requireNonNull;
import static sf.util.Utility.isBlank;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import com.annimon.stream.Optional;
import java.util.logging.Level;

import schemacrawler.schema.Column;
import schemacrawler.schema.IndexColumnSortSequence;
import schemacrawler.schema.IndexType;
import schemacrawler.schema.SchemaReference;
import schemacrawler.schema.View;
import schemacrawler.schemacrawler.InformationSchemaViews;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerSQLException;
import schemacrawler.utility.Query;
import sf.util.SchemaCrawlerLogger;
import sf.util.StringFormat;

/**
 * A retriever uses database metadata to get the details about the
 * database tables.
 *
 * @author Sualeh Fatehi
 */
final class IndexRetriever
  extends AbstractRetriever
{

  private static final SchemaCrawlerLogger LOGGER = SchemaCrawlerLogger
    .getLogger(IndexRetriever.class.getName());

  IndexRetriever(final RetrieverConnection retrieverConnection,
                 final MutableCatalog catalog,
                 final SchemaCrawlerOptions options)
    throws SQLException
  {
    super(retrieverConnection, catalog, options);
  }

  void retrieveIndexes(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    requireNonNull(allTables);

    final MetadataRetrievalStrategy indexRetrievalStrategy = getRetrieverConnection()
      .getIndexRetrievalStrategy();
    switch (indexRetrievalStrategy)
    {
      case data_dictionary_all:
        LOGGER.log(Level.INFO,
                   "Retrieving indexes, using fast data dictionary retrieval");
        retrieveIndexesFromDataDictionary(allTables);
        break;

      case metadata_all:
        LOGGER.log(Level.INFO,
                   "Retrieving indexes, using fast meta-data retrieval");
        retrieveIndexesFromMetadataForAllTables(allTables);
        break;

      case metadata:
        LOGGER.log(Level.INFO, "Retrieving indexes");
        retrieveIndexesFromMetadata(allTables);
        break;

      default:
        break;
    }

  }

  void retrievePrimaryKeys(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    requireNonNull(allTables);

    final MetadataRetrievalStrategy pkRetrievalStrategy = getRetrieverConnection()
      .getPrimaryKeyRetrievalStrategy();
    switch (pkRetrievalStrategy)
    {
      case data_dictionary_all:
        LOGGER
          .log(Level.INFO,
               "Retrieving primary keys, using fast data dictionary retrieval");
        retrievePrimaryKeysFromDataDictionary(allTables);
        break;

      case metadata_all:
        LOGGER.log(Level.INFO,
                   "Retrieving primary keys, using fast meta-data retrieval");
        retrievePrimaryKeysFromMetadataForAllTables(allTables);
        break;

      case metadata:
        LOGGER.log(Level.INFO, "Retrieving primary keys");
        retrievePrimaryKeysFromMetadata(allTables);
        break;

      default:
        break;
    }

  }

  private void createIndexes(final MutableTable table,
                             final MetadataResultSet results)
    throws SQLException
  {
    while (results.next())
    {
      createIndexForTable(table, results);
    }
  }

  private void createIndexForTable(final MutableTable table,
                                   final MetadataResultSet results)
  {
    // "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME"
    String indexName = nameQuotedName(results.getString("INDEX_NAME"));
    LOGGER.log(Level.FINE,
               new StringFormat("Retrieving index <%s.%s>",
                                table.getFullName(),
                                indexName));

    // Work-around PostgreSQL JDBC driver bugs by unquoting column
    // names first
    // #3480 -
    // http://www.postgresql.org/message-id/200707231358.l6NDwlWh026230@wwwmaster.postgresql.org
    // #6253 -
    // http://www.postgresql.org/message-id/201110121403.p9CE3fsx039675@wwwmaster.postgresql.org
    final String columnName = nameQuotedName(unquotedName(results
      .getString("COLUMN_NAME")));
    if (isBlank(columnName))
    {
      return;
    }

    final boolean uniqueIndex = !results.getBoolean("NON_UNIQUE");
    final IndexType type = results.getEnumFromId("TYPE", IndexType.unknown);
    final int ordinalPosition = results.getInt("ORDINAL_POSITION", 0);
    final IndexColumnSortSequence sortSequence = IndexColumnSortSequence
      .valueOfFromCode(results.getString("ASC_OR_DESC"));
    final int cardinality = results.getInt("CARDINALITY", 0);
    final int pages = results.getInt("PAGES", 0);

    final Column column;
    final Optional<MutableColumn> columnOptional = table
      .lookupColumn(columnName);
    if (columnOptional.isPresent())
    {
      final MutableColumn mutableColumn = columnOptional.get();
      mutableColumn.markAsPartOfIndex();
      if (uniqueIndex)
      {
        mutableColumn.markAsPartOfUniqueIndex();
      }
      column = mutableColumn;
    }
    else
    {
      // Indexes may have pseudo-columns, that are not part of the table
      // for example, Oracle function-based indexes have columns from
      // the result of a function
      column = new ColumnPartial(table, columnName);
    }

    if (isBlank(indexName))
    {
      indexName = String.format("SC_%s",
                                Integer
                                  .toHexString(column.getFullName().hashCode())
                                  .toUpperCase());
    }

    final Optional<MutableIndex> indexOptional = table.lookupIndex(indexName);
    final MutableIndex index;
    if (indexOptional.isPresent())
    {
      index = indexOptional.get();
    }
    else
    {
      index = new MutableIndex(table, indexName);
      table.addIndex(index);
    }

    final MutableIndexColumn indexColumn = new MutableIndexColumn(index,
                                                                  column);
    indexColumn.setIndexOrdinalPosition(ordinalPosition);
    indexColumn.setSortSequence(sortSequence);
    //
    index.addColumn(indexColumn);
    index.setUnique(uniqueIndex);
    index.setIndexType(type);
    index.setCardinality(cardinality);
    index.setPages(pages);
    index.addAttributes(results.getAttributes());
  }

  private void createPrimaryKeyForTable(final MutableTable table,
                                        final MetadataResultSet results)
  {
    MutablePrimaryKey primaryKey;
    // "TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME"
    final String columnName = nameQuotedName(results.getString("COLUMN_NAME"));
    final String primaryKeyName = nameQuotedName(results.getString("PK_NAME"));
    final int keySequence = Integer.parseInt(results.getString("KEY_SEQ"));

    primaryKey = table.getPrimaryKey();
    if (primaryKey == null)
    {
      primaryKey = new MutablePrimaryKey(table, primaryKeyName);
      table.setPrimaryKeyAndReplaceIndex(primaryKey);
    }

    // Register primary key information
    final Optional<MutableColumn> columnOptional = table
      .lookupColumn(columnName);
    if (columnOptional.isPresent())
    {
      final MutableColumn column = columnOptional.get();
      column.markAsPartOfPrimaryKey();
      final MutableIndexColumn indexColumn = new MutableIndexColumn(primaryKey,
                                                                    column);
      indexColumn.setSortSequence(IndexColumnSortSequence.ascending);
      indexColumn.setIndexOrdinalPosition(keySequence);
      //
      primaryKey.addColumn(indexColumn);
    }
  }

  private Optional<MutableTable> lookupTable(final NamedObjectList<MutableTable> allTables,
                                             final MetadataResultSet results)
  {
    final String catalogName = nameQuotedName(results.getString("TABLE_CAT"));
    final String schemaName = nameQuotedName(results.getString("TABLE_SCHEM"));
    final String tableName = nameQuotedName(results.getString("TABLE_NAME"));

    final Optional<MutableTable> optionalTable = allTables
      .lookup(new SchemaReference(catalogName, schemaName), tableName);
    return optionalTable;
  }

  private void retrieveIndexesFromDataDictionary(final NamedObjectList<MutableTable> allTables)
    throws SchemaCrawlerSQLException
  {
    final InformationSchemaViews informationSchemaViews = getRetrieverConnection()
      .getInformationSchemaViews();

    if (!informationSchemaViews.hasIndexesSql())
    {
      LOGGER.log(Level.FINE, "Extended indexes SQL statement was not provided");
      return;
    }

    final Query indexesSql = informationSchemaViews.getIndexesSql();
    final Connection connection = getDatabaseConnection();
    try (final Statement statement = connection.createStatement();
        final MetadataResultSet results = new MetadataResultSet(indexesSql,
                                                                statement,
                                                                getSchemaInclusionRule());)
    {
      results.setDescription("retrieveIndexesFromDataDictionary");
      while (results.next())
      {
        final Optional<MutableTable> optionalTable = lookupTable(allTables,
                                                                 results);
        if (!optionalTable.isPresent())
        {
          continue;
        }
        final MutableTable table = optionalTable.get();
        createIndexForTable(table, results);
      }
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerSQLException("Could not retrieve indexes from SQL:\n"
                                          + indexesSql, e);
    }
  }

  private void retrieveIndexesFromMetadata(final MutableTable table,
                                           final boolean unique)
    throws SQLException
  {

    try (final MetadataResultSet results = new MetadataResultSet(getMetaData()
      .getIndexInfo(unquotedName(table.getSchema().getCatalogName()),
                    unquotedName(table.getSchema().getName()),
                    unquotedName(table.getName()),
                    unique,
                    true/* approximate */));)
    {
      createIndexes(table, results);
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerSQLException("Could not retrieve indexes for table "
                                          + table, e);
    }
  }

  private void retrieveIndexesFromMetadata(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    for (final MutableTable table: allTables)
    {
      if (table instanceof View)
      {
        continue;
      }
      retrieveIndexesFromMetadata(table, false);
      retrieveIndexesFromMetadata(table, true);
    }
  }

  private void retrieveIndexesFromMetadataForAllTables(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    retrieveIndexesFromMetadataForAllTables(allTables, false);
    retrieveIndexesFromMetadataForAllTables(allTables, true);
  }

  private void retrieveIndexesFromMetadataForAllTables(final NamedObjectList<MutableTable> allTables,
                                                       final boolean unique)
    throws SQLException
  {
    try (final MetadataResultSet results = new MetadataResultSet(getMetaData()
      .getIndexInfo(null,
                    null,
                    "%",
                    unique,
                    true/* approximate */));)
    {
      while (results.next())
      {
        final Optional<MutableTable> optionalTable = lookupTable(allTables,
                                                                 results);
        if (!optionalTable.isPresent())
        {
          continue;
        }
        final MutableTable table = optionalTable.get();
        createIndexForTable(table, results);
      }
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerSQLException("Could not retrieve indexes for tables",
                                          e);
    }

  }

  private void retrievePrimaryKeysFromDataDictionary(final NamedObjectList<MutableTable> allTables)
    throws SchemaCrawlerSQLException
  {
    final InformationSchemaViews informationSchemaViews = getRetrieverConnection()
      .getInformationSchemaViews();

    if (!informationSchemaViews.hasPrimaryKeysSql())
    {
      LOGGER.log(Level.FINE,
                 "Extended primary keys SQL statement was not provided");
      return;
    }

    final Query pkSql = informationSchemaViews.getPrimaryKeysSql();
    final Connection connection = getDatabaseConnection();
    try (final Statement statement = connection.createStatement();
        final MetadataResultSet results = new MetadataResultSet(pkSql,
                                                                statement,
                                                                getSchemaInclusionRule());)
    {
      results.setDescription("retrievePrimaryKeysFromDataDictionary");
      while (results.next())
      {
        final Optional<MutableTable> optionalTable = lookupTable(allTables,
                                                                 results);
        if (!optionalTable.isPresent())
        {
          continue;
        }
        final MutableTable table = optionalTable.get();
        createPrimaryKeyForTable(table, results);
      }
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerSQLException("Could not retrieve primary keys from SQL:\n"
                                          + pkSql, e);
    }
  }

  private void retrievePrimaryKeysFromMetadata(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    for (final MutableTable table: allTables)
    {
      if (table instanceof View)
      {
        continue;
      }
      try (final MetadataResultSet results = new MetadataResultSet(getMetaData()
        .getPrimaryKeys(unquotedName(table.getSchema().getCatalogName()),
                        unquotedName(table.getSchema().getName()),
                        unquotedName(table.getName())));)
      {
        while (results.next())
        {
          createPrimaryKeyForTable(table, results);
        }
      }
      catch (final SQLException e)
      {
        throw new SchemaCrawlerSQLException("Could not retrieve primary keys for table "
                                            + table, e);
      }
    }
  }

  private void retrievePrimaryKeysFromMetadataForAllTables(final NamedObjectList<MutableTable> allTables)
    throws SQLException
  {
    try (final MetadataResultSet results = new MetadataResultSet(getMetaData()
      .getPrimaryKeys(null,
                      null,
                      "%"));)
    {
      while (results.next())
      {
        final Optional<MutableTable> optionalTable = lookupTable(allTables,
                                                                 results);
        if (!optionalTable.isPresent())
        {
          continue;
        }
        final MutableTable table = optionalTable.get();
        createPrimaryKeyForTable(table, results);
      }
    }
    catch (final SQLException e)
    {
      throw new SchemaCrawlerSQLException("Could not retrieve primary keys for tables",
                                          e);
    }
  }

}
