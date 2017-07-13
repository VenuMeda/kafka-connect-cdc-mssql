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

import com.github.jcustenborder.kafka.connect.cdc.CDCSourceConnector;
import com.github.jcustenborder.kafka.connect.cdc.JdbcUtils;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("The Microsoft SQL Server connector utilizes [Change Tracking](https://msdn.microsoft.com/en-us/library/bb933875.aspx) " +
    "to identify changes. There are two ways to read the changes from the source system as they are generated. " +
    "[Change Data Capture](https://msdn.microsoft.com/en-us/library/cc645937.aspx) is a feature that is only available " +
    "on SQL Server Enterprise and Developer editions. [Change Tracking](https://msdn.microsoft.com/en-us/library/bb933875.aspx) " +
    "is a lightweight solution that will efficiently find rows that have changed. If the rows are modified in quick " +
    "succession all of the changes might not be found. The latest version of the change will be returned.")
public class MsSqlSourceConnector extends CDCSourceConnector {
  private static final Logger log = LoggerFactory.getLogger(MsSqlSourceConnector.class);
  Map<String, String> settings;
  MsSqlSourceConnectorConfig config;
  int taskCount;

  @Override
  public void start(Map<String, String> settings) {
    this.settings = settings;
    this.config = new MsSqlSourceConnectorConfig(settings);
  }

  @Override
  public Class<? extends Task> taskClass() {
    return MsSqlSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int taskCount) {
    Preconditions.checkState(taskCount > 0, "At least one task is required");
    this.taskCount = taskCount;

    final List<String> changeTrackingTables;
    if (null == this.config.changeTrackingTables || this.config.changeTrackingTables.isEmpty()) {
      try {
        PooledConnection pooledConnection = null;
        try {
          pooledConnection = JdbcUtils.openPooledConnection(this.config, null);
          Connection connection = pooledConnection.getConnection();

          final String sql = "SELECT s.name AS schema_name, t.name AS table_name, tr.is_track_columns_updated_on " +
              "FROM sys.change_tracking_tables tr " +
              "INNER JOIN sys.tables t ON t.object_id = tr.object_id " +
              "INNER JOIN sys.schemas s ON s.schema_id = t.schema_id " +
              "WHERE tr.is_track_columns_updated_on = 1";

          List<String> tables = new ArrayList<>(1024);
          try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
              while (resultSet.next()) {
                final String schemaName = resultSet.getString(1);
                final String tableName = resultSet.getString(2);
                tables.add(
                    String.format("%s.%s", schemaName, tableName)
                );
              }
            }
          }
          changeTrackingTables = ImmutableList.copyOf(tables);
        } finally {
          JdbcUtils.closeConnection(pooledConnection);
        }
      } catch (SQLException e) {
        throw new ConnectException(e);
      }
    } else {
      changeTrackingTables = this.config.changeTrackingTables;
    }

    List<Map<String, String>> taskConfigs = new ArrayList<>(taskCount);
    int index = 0;
    for (Iterable<String> tables : Iterables.partition(changeTrackingTables, taskCount)) {
      if (Iterables.size(tables) == 0) {
        continue;
      }

      final String tableConfig = Joiner.on(',').join(tables);
      log.info(
          "Setting task {} '{}' to '{}'",
          index,
          MsSqlSourceConnectorConfig.CHANGE_TRACKING_TABLES_CONFIG,
          tableConfig
      );
      Map<String, String> taskSettings = new LinkedHashMap<>();
      taskSettings.putAll(this.settings);
      taskSettings.put(MsSqlSourceConnectorConfig.CHANGE_TRACKING_TABLES_CONFIG, tableConfig);
      taskConfigs.add(taskSettings);
      index++;
    }

    return taskConfigs;
  }

  @Override
  public void stop() {

  }

  @Override
  public ConfigDef config() {
    return MsSqlSourceConnectorConfig.config();
  }
}