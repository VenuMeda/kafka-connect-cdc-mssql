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

import com.github.jcustenborder.kafka.connect.cdc.TableMetadataProvider;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class MsSqlQueryBuilder {
  private static final Logger log = LoggerFactory.getLogger(MsSqlQueryBuilder.class);
  final Connection connection;
  final static String LIST_CHANGE_TRACKING_TABLES_SQL = "SELECT DB_NAME() AS [databaseName], " +
      "SCHEMA_NAME(OBJECTPROPERTY(object_id, 'SchemaId')) AS [schemaName], " +
      "OBJECT_NAME(object_id) AS [tableName], " +
      "min_valid_version, " +
      "begin_version " +
      "FROM " +
      "[sys].[change_tracking_tables]";

  MsSqlQueryBuilder(Connection connection) {
    this.connection = connection;
  }

  String joinSelect(String table, Collection<String> keyColumns) {
    List<String> selectColumns = new ArrayList<>(keyColumns.size());

    for (String keyColumn : keyColumns) {
      selectColumns.add(
          String.format("[%s].[%s]", table, keyColumn)
      );
    }

    return Joiner.on(", ").join(selectColumns);
  }

  String joinCriteria(Set<String> keyColumns) {
    StringBuilder joinCriteria = new StringBuilder();
    for (String keyColumn : keyColumns) {
      if (joinCriteria.length() > 0) {
        joinCriteria.append(" AND ");
      }

      joinCriteria.append(
          String.format("[ct].[%s] = [u].[%s]", keyColumn, keyColumn)
      );
    }
    return joinCriteria.toString();
  }

  String changeTrackingStatementQuery(TableMetadataProvider.TableMetadata tableMetadata) {
    Preconditions.checkState(
        tableMetadata.keyColumns().size() > 0,
        "Table([%s].[%s]) must have at least one primary key column.",
        tableMetadata.schemaName(),
        tableMetadata.tableName()
    );
    Collection<String> valueColumns = Collections2.filter(tableMetadata.columnSchemas().keySet(), Predicates.not(Predicates.in(tableMetadata.keyColumns())));

    String joinCriteria = joinCriteria(tableMetadata.keyColumns());
    final String sql = String.format("SELECT " +
            "[ct].[sys_change_version] AS [__metadata_sys_change_version], " +
            "[ct].[sys_change_creation_version] AS [__metadata_sys_change_creation_version], " +
            "[ct].[sys_change_operation] AS [__metadata_sys_change_operation], " +
            joinSelect("ct", tableMetadata.keyColumns()) + ", " +
            joinSelect("u", valueColumns) + " " +
            "FROM [%s].[%s] AS [u] " +
            "RIGHT OUTER JOIN " +
            "CHANGETABLE(CHANGES [%s].[%s], ?) AS [ct] " +
            "ON %s",
        tableMetadata.schemaName(),
        tableMetadata.tableName(),
        tableMetadata.schemaName(),
        tableMetadata.tableName(),
        joinCriteria
    );
    log.trace("changeTrackingStatementQuery() - sql:\n{}", sql);
    return sql;
  }

  public PreparedStatement listChangeTrackingTablesStatement() throws SQLException {
    return this.connection.prepareStatement(LIST_CHANGE_TRACKING_TABLES_SQL);
  }

  public PreparedStatement changeTrackingStatement(TableMetadataProvider.TableMetadata tableMetadata) throws SQLException {
    final String sql = changeTrackingStatementQuery(tableMetadata);
    return this.connection.prepareStatement(sql);
  }
}
