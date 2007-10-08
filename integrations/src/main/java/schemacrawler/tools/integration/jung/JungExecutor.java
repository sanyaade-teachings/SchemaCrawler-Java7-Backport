/* 
 *
 * SchemaCrawler
 * http://sourceforge.net/projects/schemacrawler
 * Copyright (c) 2000-2007, Sualeh Fatehi.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package schemacrawler.tools.integration.jung;


import java.awt.Dimension;
import java.io.File;

import javax.sql.DataSource;

import schemacrawler.crawl.InformationSchemaViews;
import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.crawl.SchemaCrawlerOptions;
import schemacrawler.schema.Schema;
import schemacrawler.tools.ExecutionContext;
import schemacrawler.tools.Executor;
import schemacrawler.tools.ToolType;
import schemacrawler.tools.schematext.SchemaTextOptions;
import edu.uci.ics.jung.graph.Graph;

/**
 * Main executor for the JUNG integration.
 * 
 * @author Sualeh Fatehi
 */
public final class JungExecutor
  implements Executor
{

  private static final int DEFAULT_IMAGE_WIDTH = 600;

  public void execute(final ExecutionContext executionContext,
                      final DataSource dataSource)
    throws Exception
  {

    if (executionContext == null
        || executionContext.getToolType() != ToolType.schema_text)
    {
      throw new IllegalArgumentException("Bad execution context specified");
    }

    final SchemaCrawlerOptions schemaCrawlerOptions = executionContext
      .getSchemaCrawlerOptions();
    final SchemaTextOptions schemaTextOptions = (SchemaTextOptions) executionContext
      .getToolOptions();
    final InformationSchemaViews informationSchemaViews = executionContext
      .getInformationSchemaViews();

    // Get the entire schema at once, since we need to use this to
    // render the velocity template
    final File outputFile = schemaTextOptions.getOutputOptions()
      .getOutputFile();
    final Dimension size = getSize(schemaTextOptions.getOutputOptions()
      .getOutputFormatValue());
    final Schema schema = SchemaCrawler.getSchema(dataSource,
                                                  informationSchemaViews,
                                                  schemaTextOptions
                                                    .getSchemaTextDetailType()
                                                    .mapToInfoLevel(),
                                                  schemaCrawlerOptions);
    final Graph graph = JungUtil.makeSchemaGraph(schema);
    JungUtil.saveGraphJpeg(graph, outputFile, size);
  }

  private Dimension getSize(final String dimensions)
  {
    final String[] sizes = dimensions.split("x");
    try
    {
      final int width = Integer.parseInt(sizes[0]);
      final int height = Integer.parseInt(sizes[1]);
      return new Dimension(width, height);
    }
    catch (final NumberFormatException e)
    {
      return new Dimension(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_WIDTH);
    }
  }

}
