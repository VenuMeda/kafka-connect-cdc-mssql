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

import com.github.jcustenborder.kafka.connect.cdc.ChangeKey;
import com.github.jcustenborder.kafka.connect.cdc.Integration;
import com.github.jcustenborder.kafka.connect.cdc.JdbcUtils;
import com.github.jcustenborder.kafka.connect.cdc.docker.DockerCompose;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlClusterHealthCheck;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlSettings;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlSettingsExtension;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@Category(Integration.class)
@DockerCompose(dockerComposePath = MsSqlTestConstants.DOCKER_COMPOSE_FILE, clusterHealthCheck = MsSqlClusterHealthCheck.class)
@ExtendWith(MsSqlSettingsExtension.class)
public class QueryServiceTest extends MsSqlTest {
  private static final Logger log = LoggerFactory.getLogger(QueryServiceTest.class);


  MsSqlSourceConnectorConfig config;

  @BeforeEach
  public void before(@MsSqlSettings Map<String, String> settings) {
    config = new MsSqlSourceConnectorConfig(settings);
  }

  @TestFactory
  public Stream<DynamicTest> queryTable() throws SQLException {
    List<ChangeKey> changeCaptureTables = new ArrayList<>();
    PooledConnection pooledConnection = null;
    try {
      pooledConnection = JdbcUtils.openPooledConnection(this.config, new ChangeKey(MsSqlTestConstants.DATABASE_NAME, null, null));
      MsSqlQueryBuilder queryBuilder = new MsSqlQueryBuilder(pooledConnection.getConnection());
      try (PreparedStatement statement = queryBuilder.listChangeTrackingTablesStatement()) {
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            String databaseName = resultSet.getString("databaseName");
            String schemaName = resultSet.getString("schemaName");
            String tableName = resultSet.getString("tableName");
            ChangeKey changeKey = new ChangeKey(databaseName, schemaName, tableName);
            changeCaptureTables.add(changeKey);
            log.trace("Found Change Tracking Enabled Table {}", changeKey);
          }
        }
      }
    } finally {
      JdbcUtils.closeConnection(pooledConnection);
    }

    return changeCaptureTables.stream().map(data -> dynamicTest(data.tableName, () -> queryTable(data)));
  }

  private void queryTable(ChangeKey input) throws SQLException, IOException {
//    JsonChangeList expectedChanges;
//    String fileName = String.format("%s.%s.json", input.schemaName, input.tableName);
//    String resourceName = String.format("query/table/%s/%s", input.databaseName, fileName);
//
//    long timestamp = 0L;
//    try (InputStream stream = this.getClass().getResourceAsStream(resourceName)) {
//      Preconditions.checkNotNull(stream, "Could not find resource %s.", resourceName);
//      if (log.isInfoEnabled()) {
//        log.info("Loading expected changes from {}", resourceName);
//      }
//      expectedChanges = JsonChangeList.read(stream);
//      for (Change change : expectedChanges) {
//        timestamp = change.timestamp();
//        break;
//      }
//    }
//
//    OffsetStorageReader offsetStorageReader = mock(OffsetStorageReader.class);
//    TableMetadataProvider tableMetadataProvider = new MsSqlTableMetadataProvider(config, offsetStorageReader);
//    Time time = mock(Time.class);
//    ChangeWriter changeWriter = mock(ChangeWriter.class);
//    List<Change> actualChanges = new ArrayList<>(1000);
//
//    doAnswer(invocationOnMock -> {
//      Change change = invocationOnMock.getArgumentAt(0, Change.class);
//      actualChanges.add(change);
//      return null;
//    }).when(changeWriter).addChange(any());
//
//
//    QueryService queryService = new QueryService(time, tableMetadataProvider, config, changeWriter);
//
//    when(time.milliseconds()).thenReturn(timestamp);
//    queryService.queryTable(changeWriter, input);
//
//    verify(offsetStorageReader, only()).offset(anyMap());
//    verify(time, atLeastOnce()).milliseconds();
//
//
//    if (log.isDebugEnabled()) {
//      log.trace("Found {} change(s).", actualChanges.size());
//    }
//
//    assertFalse(actualChanges.isEmpty(), "Changes should have been returned.");
//    assertEquals(expectedChanges.size(), actualChanges.size(), "The number of actualChanges returned is not the expect count.");
//    for (int i = 0; i < expectedChanges.size(); i++) {
//      Change expectedChange = expectedChanges.get(i);
//      Change actualChange = actualChanges.get(i);
//      assertChange(expectedChange, actualChange);
//    }
  }

}
