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


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import com.annimon.stream.Optional;
import java.util.logging.Level;

import schemacrawler.schema.RoutineBodyType;
import schemacrawler.schemacrawler.InformationSchemaViews;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.utility.Query;
import sf.util.SchemaCrawlerLogger;
import sf.util.StringFormat;

/**
 * A retriever that uses database metadata to get the extended details
 * about the database routines.
 *
 * @author Sualeh Fatehi
 */
final class RoutineExtRetriever
  extends AbstractRetriever
{

  private static final SchemaCrawlerLogger LOGGER = SchemaCrawlerLogger
    .getLogger(RoutineExtRetriever.class.getName());

  RoutineExtRetriever(final RetrieverConnection retrieverConnection,
                      final MutableCatalog catalog,
                      final SchemaCrawlerOptions options)
    throws SQLException
  {
    super(retrieverConnection, catalog, options);
  }

  /**
   * Retrieves a routine definitions from the database.
   *
   * @throws SQLException
   *         On a SQL exception
   */
  void retrieveRoutineInformation()
    throws SQLException
  {
    final InformationSchemaViews informationSchemaViews = getRetrieverConnection()
      .getInformationSchemaViews();
    if (!informationSchemaViews.hasRoutinesSql())
    {
      LOGGER
        .log(Level.INFO,
             "Not retrieving routine definitions, since this was not requested");
      LOGGER.log(Level.FINE,
                 "Routine definition SQL statement was not provided");
      return;
    }

    LOGGER.log(Level.INFO, "Retrieving procedure definitions");

    final Query routineDefinitionsSql = informationSchemaViews.getRoutinesSql();
    final Connection connection = getDatabaseConnection();
    try (final Statement statement = connection.createStatement();
        final MetadataResultSet results = new MetadataResultSet(routineDefinitionsSql,
                                                                statement,
                                                                getSchemaInclusionRule());)
    {
      while (results.next())
      {
        final String catalogName = quotedName(results
          .getString("ROUTINE_CATALOG"));
        final String schemaName = quotedName(results
          .getString("ROUTINE_SCHEMA"));
        final String routineName = quotedName(results
          .getString("ROUTINE_NAME"));
        final String specificName = quotedName(results
          .getString("SPECIFIC_NAME"));

        final Optional<MutableRoutine> routineOptional = lookupRoutine(catalogName,
                                                                       schemaName,
                                                                       routineName,
                                                                       specificName);
        if (routineOptional.isPresent())
        {
          final MutableRoutine routine = routineOptional.get();
          LOGGER.log(Level.FINER,
                     new StringFormat("Retrieving routine information for <%s>",
                                      routineName));
          final RoutineBodyType routineBodyType = results
            .getEnum("ROUTINE_BODY", RoutineBodyType.unknown);
          final String definition = results.getString("ROUTINE_DEFINITION");

          routine.setRoutineBodyType(routineBodyType);
          routine.appendDefinition(definition);

          routine.addAttributes(results.getAttributes());
        }
      }
    }
    catch (final Exception e)
    {
      LOGGER.log(Level.WARNING, "Could not retrieve routines", e);
    }
  }

}
