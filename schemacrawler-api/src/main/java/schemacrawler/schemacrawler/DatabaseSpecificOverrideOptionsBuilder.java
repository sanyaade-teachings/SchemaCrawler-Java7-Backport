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
package schemacrawler.schemacrawler;


import com.annimon.stream.Optional;

import schemacrawler.crawl.MetadataRetrievalStrategy;

public class DatabaseSpecificOverrideOptionsBuilder
  implements OptionsBuilder<DatabaseSpecificOverrideOptions>
{

  private static final String SC_RETRIEVAL_TABLES = "schemacrawler.schema.retrieval.strategy.tables";
  private static final String SC_RETRIEVAL_TABLE_COLUMNS = "schemacrawler.schema.retrieval.strategy.tablecolumns";
  private static final String SC_RETRIEVAL_PRIMARY_KEYS = "schemacrawler.schema.retrieval.strategy.primarykeys";
  private static final String SC_RETRIEVAL_INDEXES = "schemacrawler.schema.retrieval.strategy.indexes";
  private static final String SC_RETRIEVAL_FOREIGN_KEYS = "schemacrawler.schema.retrieval.strategy.foreignkeys";

  private Optional<Boolean> supportsSchemas;
  private Optional<Boolean> supportsCatalogs;
  private MetadataRetrievalStrategy tableRetrievalStrategy;
  private MetadataRetrievalStrategy tableColumnRetrievalStrategy;
  private MetadataRetrievalStrategy pkRetrievalStrategy;
  private MetadataRetrievalStrategy indexRetrievalStrategy;
  private MetadataRetrievalStrategy fkRetrievalStrategy;
  private String identifierQuoteString;
  private final InformationSchemaViewsBuilder informationSchemaViewsBuilder;

  public DatabaseSpecificOverrideOptionsBuilder()
  {
    informationSchemaViewsBuilder = new InformationSchemaViewsBuilder();
    supportsSchemas = Optional.empty();
    supportsCatalogs = Optional.empty();
    identifierQuoteString = "";
    tableRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    tableColumnRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    pkRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    indexRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    fkRetrievalStrategy = MetadataRetrievalStrategy.metadata;
  }

  public DatabaseSpecificOverrideOptionsBuilder(final Config map)
  {
    this();
    fromConfig(map);
  }

  /**
   * Overrides the JDBC driver provided information about whether the
   * database supports catalogs.
   */
  public DatabaseSpecificOverrideOptionsBuilder doesNotSupportCatalogs()
  {
    supportsCatalogs = Optional.of(false);
    return this;
  }

  /**
   * Overrides the JDBC driver provided information about whether the
   * database supports schema.
   */
  public DatabaseSpecificOverrideOptionsBuilder doesNotSupportSchemas()
  {
    supportsSchemas = Optional.of(false);
    return this;
  }

  @Override
  public DatabaseSpecificOverrideOptionsBuilder fromConfig(final Config config)
  {
    final Config configProperties;
    if (config == null)
    {
      configProperties = new Config();
    }
    else
    {
      configProperties = new Config(config);
    }

    informationSchemaViewsBuilder.fromConfig(configProperties);

    tableRetrievalStrategy = configProperties
      .getEnumValue(SC_RETRIEVAL_TABLES, tableRetrievalStrategy);
    tableColumnRetrievalStrategy = configProperties
      .getEnumValue(SC_RETRIEVAL_TABLE_COLUMNS, tableColumnRetrievalStrategy);
    pkRetrievalStrategy = configProperties
      .getEnumValue(SC_RETRIEVAL_PRIMARY_KEYS, pkRetrievalStrategy);
    indexRetrievalStrategy = configProperties
      .getEnumValue(SC_RETRIEVAL_INDEXES, indexRetrievalStrategy);
    fkRetrievalStrategy = configProperties
      .getEnumValue(SC_RETRIEVAL_FOREIGN_KEYS, fkRetrievalStrategy);

    return this;
  }

  public MetadataRetrievalStrategy getForeignKeyRetrievalStrategy()
  {
    return fkRetrievalStrategy;
  }

  public String getIdentifierQuoteString()
  {
    return identifierQuoteString;
  }

  public MetadataRetrievalStrategy getIndexRetrievalStrategy()
  {
    return indexRetrievalStrategy;
  }

  public InformationSchemaViewsBuilder getInformationSchemaViewsBuilder()
  {
    return informationSchemaViewsBuilder;
  }

  public MetadataRetrievalStrategy getPrimaryKeyRetrievalStrategy()
  {
    return pkRetrievalStrategy;
  }

  public Optional<Boolean> getSupportsCatalogs()
  {
    return supportsCatalogs;
  }

  public Optional<Boolean> getSupportsSchemas()
  {
    return supportsSchemas;
  }

  public MetadataRetrievalStrategy getTableColumnRetrievalStrategy()
  {
    return tableColumnRetrievalStrategy;
  }

  public MetadataRetrievalStrategy getTableRetrievalStrategy()
  {
    return tableRetrievalStrategy;
  }

  /**
   * Overrides the JDBC driver provided information about the identifier
   * quote string.
   *
   * @param identifierQuoteString
   *        Value for the override
   */
  public DatabaseSpecificOverrideOptionsBuilder identifierQuoteString(final String identifierQuoteString)
  {
    this.identifierQuoteString = identifierQuoteString;
    return this;
  }

  /**
   * Overrides the JDBC driver provided information about whether the
   * database supports catalogs.
   */
  public DatabaseSpecificOverrideOptionsBuilder supportsCatalogs()
  {
    supportsCatalogs = Optional.of(true);
    return this;
  }

  /**
   * Overrides the JDBC driver provided information about whether the
   * database supports schema.
   */
  public DatabaseSpecificOverrideOptionsBuilder supportsSchemas()
  {
    supportsSchemas = Optional.of(true);
    return this;
  }

  @Override
  public Config toConfig()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public DatabaseSpecificOverrideOptions toOptions()
  {
    return new DatabaseSpecificOverrideOptions(this);
  }

  public DatabaseSpecificOverrideOptionsBuilder withForeignKeyRetrievalStrategy(final MetadataRetrievalStrategy fkRetrievalStrategy)
  {
    if (fkRetrievalStrategy == null)
    {
      this.fkRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    }
    else
    {
      this.fkRetrievalStrategy = fkRetrievalStrategy;
    }
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withIndexRetrievalStrategy(final MetadataRetrievalStrategy indexRetrievalStrategy)
  {
    if (indexRetrievalStrategy == null)
    {
      this.indexRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    }
    else
    {
      this.indexRetrievalStrategy = indexRetrievalStrategy;
    }
    return this;
  }

  public InformationSchemaViewsBuilder withInformationSchemaViews()
  {
    return informationSchemaViewsBuilder;
  }

  public DatabaseSpecificOverrideOptionsBuilder withoutIdentifierQuoteString()
  {
    identifierQuoteString = null;
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withoutSupportsCatalogs()
  {
    supportsCatalogs = Optional.empty();
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withoutSupportsSchemas()
  {
    supportsSchemas = Optional.empty();
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withPrimaryKeyRetrievalStrategy(final MetadataRetrievalStrategy pkRetrievalStrategy)
  {
    if (pkRetrievalStrategy == null)
    {
      this.pkRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    }
    else
    {
      this.pkRetrievalStrategy = pkRetrievalStrategy;
    }
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withTableColumnRetrievalStrategy(final MetadataRetrievalStrategy tableColumnRetrievalStrategy)
  {
    if (tableColumnRetrievalStrategy == null)
    {
      this.tableColumnRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    }
    else
    {
      this.tableColumnRetrievalStrategy = tableColumnRetrievalStrategy;
    }
    return this;
  }

  public DatabaseSpecificOverrideOptionsBuilder withTableRetrievalStrategy(final MetadataRetrievalStrategy tableRetrievalStrategy)
  {
    if (tableRetrievalStrategy == null)
    {
      this.tableRetrievalStrategy = MetadataRetrievalStrategy.metadata;
    }
    else
    {
      this.tableRetrievalStrategy = tableRetrievalStrategy;
    }
    return this;
  }

}
