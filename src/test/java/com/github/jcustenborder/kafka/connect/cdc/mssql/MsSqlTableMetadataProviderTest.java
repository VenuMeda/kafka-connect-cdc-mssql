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
import com.github.jcustenborder.kafka.connect.cdc.TableMetadataProvider;
import com.github.jcustenborder.kafka.connect.cdc.TestDataUtils;
import com.github.jcustenborder.kafka.connect.cdc.docker.DockerCompose;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlClusterHealthCheck;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlSettings;
import com.github.jcustenborder.kafka.connect.cdc.mssql.docker.MsSqlSettingsExtension;
import com.github.jcustenborder.kafka.connect.cdc.mssql.model.MsSqlTableMetadataProviderTestData;
import org.apache.kafka.connect.storage.OffsetStorageReader;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.jcustenborder.kafka.connect.cdc.ChangeAssertions.assertTableMetadata;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.mock;

@Category(Integration.class)
@DockerCompose(dockerComposePath = MsSqlTestConstants.DOCKER_COMPOSE_FILE, clusterHealthCheck = MsSqlClusterHealthCheck.class)
@ExtendWith(MsSqlSettingsExtension.class)
public class MsSqlTableMetadataProviderTest extends MsSqlTest {
  private static final Logger log = LoggerFactory.getLogger(MsSqlTableMetadataProviderTest.class);
  MsSqlSourceConnectorConfig config;
  TableMetadataProvider tableMetadataProvider;
  OffsetStorageReader offsetStorageReader;

  @BeforeEach
  public void before(@MsSqlSettings Map<String, String> settings) {
    this.config = new MsSqlSourceConnectorConfig(settings);
    this.offsetStorageReader = mock(OffsetStorageReader.class);
    this.tableMetadataProvider = new MsSqlTableMetadataProvider(this.config, this.offsetStorageReader);
  }

  @TestFactory
  public Stream<DynamicTest> tableMetadata() throws IOException {
    String packageName = this.getClass().getPackage().getName() + ".metadata.table";
    List<MsSqlTableMetadataProviderTestData> testData = TestDataUtils.loadJsonResourceFiles(packageName, MsSqlTableMetadataProviderTestData.class);
    return testData.stream().map(data -> dynamicTest(data.name(), () -> tableMetadata(data)));
  }

  private void tableMetadata(MsSqlTableMetadataProviderTestData data) throws SQLException {

    assertNotNull(data, "data should not be null.");
    TableMetadataProvider.TableMetadata actual = this.tableMetadataProvider.tableMetadata(new ChangeKey(data.databaseName(), data.schemaName(), data.tableName()));
    assertTableMetadata(data.expected(), actual);
  }
}
