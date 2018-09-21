/**
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.cdc.mssql;

import com.github.jcustenborder.kafka.connect.cdc.CachingTableMetadataProvider;
import com.github.jcustenborder.kafka.connect.cdc.Change;
import com.github.jcustenborder.kafka.connect.cdc.ChangeKey;
import com.github.jcustenborder.kafka.connect.cdc.JdbcUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class MsSqlTableMetadataProvider extends CachingTableMetadataProvider {
  final static String OFFSET_SQL = "SELECT " +
      "DB_NAME() AS [databaseName], " +
      "SCHEMA_NAME(OBJECTPROPERTY(object_id, 'SchemaId')) AS [schemaName], " +
      "OBJECT_NAME(object_id) AS [tableName], " +
      "[min_valid_version], " +
      "[begin_version] " +
      "FROM " +
      "[sys].[change_tracking_tables] " +
      "WHERE " +
      "SCHEMA_NAME(OBJECTPROPERTY(object_id, 'SchemaId')) = ? AND " +
      "OBJECT_NAME(object_id) = ?";
  private static Logger log = LoggerFactory.getLogger(MsSqlTableMetadataProvider.class);
  final static String PRIMARY_KEY_SQL =
      "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE " +
          "OBJECTPROPERTY(OBJECT_ID(CONSTRAINT_SCHEMA+'.['+CONSTRAINT_NAME)+']', 'IsPrimaryKey') = 1 AND " +
          "CONSTRAINT_SCHEMA = ? AND TABLE_NAME = ?";
  final static String COLUMN_DEFINITION_SQL =
      "SELECT column_name, iif(is_nullable='YES', 1, 0) AS is_optional, data_type, " +
          "numeric_scale FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
          "ORDER BY ORDINAL_POSITION";

  public MsSqlTableMetadataProvider(MsSqlSourceConnectorConfig config, OffsetStorageReader offsetStorageReader) {
    super(config, offsetStorageReader);
  }

  @Override
  protected TableMetadata fetchTableMetadata(ChangeKey changeKey) throws SQLException {
    log.info("{}: querying database for metadata.", changeKey);

    PooledConnection pooledConnection = null;
    try {
      pooledConnection = JdbcUtils.openPooledConnection(this.config, changeKey);
      log.trace("{}: Querying for primary keys.", changeKey);

      Set<String> keyColumns = new LinkedHashSet<>();
      try (PreparedStatement primaryKeyStatement = pooledConnection.getConnection().prepareStatement(PRIMARY_KEY_SQL)) {
        primaryKeyStatement.setString(1, changeKey.schemaName);
        primaryKeyStatement.setString(2, changeKey.tableName);
        try (ResultSet resultSet = primaryKeyStatement.executeQuery()) {
          while (resultSet.next()) {
            keyColumns.add(resultSet.getString(1));
          }
        }
      }

      log.trace("{}: Querying for schema.", changeKey);

      Map<String, Schema> columnSchemas = new LinkedHashMap<>();
      try (PreparedStatement columnDefinitionStatement = pooledConnection.getConnection().prepareStatement(COLUMN_DEFINITION_SQL)) {
        columnDefinitionStatement.setString(1, changeKey.schemaName);
        columnDefinitionStatement.setString(2, changeKey.tableName);
        try (ResultSet resultSet = columnDefinitionStatement.executeQuery()) {
          while (resultSet.next()) {
            String columnName = resultSet.getString(1);
            Schema schema = generateSchema(resultSet, changeKey, columnName);
            columnSchemas.put(columnName, schema);
          }
        }
      }
      return new MsSqlTableMetadata(changeKey, keyColumns, columnSchemas);
    } finally {
      JdbcUtils.closeConnection(pooledConnection);
    }
  }

  Schema generateSchema(ResultSet resultSet,
                        final ChangeKey changeKey,
                        final String columnName) throws SQLException {
    boolean optional = resultSet.getBoolean(2);
    String dataType = resultSet.getString(3);
    int scale = resultSet.getInt(4);
    SchemaBuilder builder;

    log.trace("{}: columnName='{}' dataType='{}' scale={} optional={}", changeKey, columnName, dataType, scale, optional);

    switch (dataType) {
      case "bigint":
        builder = SchemaBuilder.int64();
        break;
      case "bit":
        builder = SchemaBuilder.bool();
        break;
      case "char":
      case "varchar":
      case "text":
      case "nchar":
      case "nvarchar":
      case "ntext":
      case "uniqueidentifier":
        builder = SchemaBuilder.string();
        break;
      case "smallmoney":
      case "money":
      case "decimal":
      case "numeric":
        builder = Decimal.builder(scale);
        break;
      case "binary":
      case "image":
      case "varbinary":
        builder = SchemaBuilder.bytes();
        break;
      case "date":
        builder = Date.builder();
        break;
      case "datetime":
      case "datetime2":
      case "smalldatetime":
        builder = Timestamp.builder();
        break;
      case "time":
        builder = Time.builder();
        break;
      case "int":
        builder = SchemaBuilder.int32();
        break;
      case "smallint":
        builder = SchemaBuilder.int16();
        break;
      case "tinyint":
        builder = SchemaBuilder.int8();
        break;
      case "real":
        builder = SchemaBuilder.float32();
        break;
      case "float":
        builder = SchemaBuilder.float64();
        break;

      default:
        throw new DataException(
            String.format("Could not process (dataType = '%s', optional = %s, scale = %d) for %s.",
                dataType, optional, scale, changeKey
            )
        );
    }

    log.trace("{}: columnName='{}' schema.type='{}' schema.name='{}'", changeKey, columnName, builder.type(), builder.name());

    builder.parameters(
        ImmutableMap.of(Change.ColumnValue.COLUMN_NAME, columnName)
    );

    if (optional) {
      builder.optional();
    }

    return builder.build();
  }

  @Override
  public Map<String, Object> startOffset(ChangeKey changeKey) throws SQLException {
    Map<String, Object> offset = (Map<String, Object>) cachedOffsets.get(changeKey);

    log.trace("{}: Determining start offset.", changeKey);

    log.trace("{}: Checking local cache for offset.", changeKey);

    if (null != offset && !offset.isEmpty()) {
      log.trace("{}: Returning offset from local cache.", changeKey);
      return offset;
    }

    log.trace("{}: Checking kafka for offset.", changeKey);

    Map<String, Object> sourcePartition = Change.sourcePartition(changeKey);
    offset = this.offsetStorageReader.offset(sourcePartition);

    if (null != offset && !offset.isEmpty()) {
      log.trace("{}: Retrieved offset from offsetStorageReader.", changeKey);
      return offset;
    }

    log.trace("{}: Querying database for offset.", changeKey);

    PooledConnection pooledConnection = null;
    try {
      pooledConnection = JdbcUtils.openPooledConnection(this.config, changeKey);
      try (PreparedStatement statement = pooledConnection.getConnection().prepareStatement(OFFSET_SQL)) {
        statement.setString(1, changeKey.schemaName);
        statement.setString(2, changeKey.tableName);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            final long minValidVersion = resultSet.getLong("min_valid_version");
            Preconditions.checkState(
                !resultSet.wasNull(),
                "resultSet did not returned a null for min_valid_version of %s", changeKey
            );
            log.trace("{}: Found min_valid_version of {}.", changeKey, minValidVersion);

            offset = MsSqlChange.offset(minValidVersion);
          }
        }
      }
    } finally {
      JdbcUtils.closeConnection(pooledConnection);
    }

    return offset;
  }


  static class MsSqlTableMetadata implements TableMetadata {
    final String databaseName;
    final String schemaName;
    final String tableName;
    final Set<String> keyColumns;
    final Map<String, Schema> columnSchemas;

    MsSqlTableMetadata(ChangeKey changeKey, Set<String> keyColumns, Map<String, Schema> columnSchemas) {
      this(changeKey.databaseName, changeKey.schemaName, changeKey.tableName, keyColumns, columnSchemas);
    }

    MsSqlTableMetadata(String databaseName, String schemaName, String tableName, Set<String> keyColumns, Map<String, Schema> columnSchemas) {
      this.databaseName = databaseName;
      this.schemaName = schemaName;
      this.tableName = tableName;
      this.keyColumns = keyColumns;
      this.columnSchemas = columnSchemas;
    }

    @Override
    public String databaseName() {
      return this.databaseName;
    }

    @Override
    public String schemaName() {
      return this.schemaName;
    }

    @Override
    public String tableName() {
      return this.tableName;
    }

    @Override
    public Set<String> keyColumns() {
      return this.keyColumns;
    }

    @Override
    public Map<String, Schema> columnSchemas() {
      return this.columnSchemas;
    }
  }
}
