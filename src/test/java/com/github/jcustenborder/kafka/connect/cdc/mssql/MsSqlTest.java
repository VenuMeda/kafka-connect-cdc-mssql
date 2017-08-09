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

import com.github.jcustenborder.kafka.connect.cdc.docker.DockerFormatString;
import com.google.common.base.Joiner;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MsSqlTest {
  private static final Logger log = LoggerFactory.getLogger(MsSqlTest.class);

  @BeforeAll
  public static void beforeClass(
      @DockerFormatString(container = MsSqlTestConstants.CONTAINER_NAME, port = MsSqlTestConstants.PORT, format = MsSqlTestConstants.JDBCURL_FORMAT_MASTER) String masterJdbcUrl,
      @DockerFormatString(container = MsSqlTestConstants.CONTAINER_NAME, port = MsSqlTestConstants.PORT, format = MsSqlTestConstants.JDBCURL_FORMAT_CDC_TESTING) String cdcTestingJdbcUrl
  ) throws SQLException, InterruptedException, IOException {
    createDatabase(masterJdbcUrl);
    flywayMigrate(cdcTestingJdbcUrl);
  }

  static void createDatabase(final String jdbcUrl) throws SQLException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, MsSqlTestConstants.USERNAME, MsSqlTestConstants.PASSWORD)) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("CREATE DATABASE cdc_testing");
      }
    }
  }

  static void flywayMigrate(final String jdbcUrl) throws SQLException {
    Flyway flyway = new Flyway();
    flyway.setDataSource(jdbcUrl, MsSqlTestConstants.USERNAME, MsSqlTestConstants.PASSWORD);
    log.info("flyway locations. {}", Joiner.on(", ").join(flyway.getLocations()));
    flyway.migrate();
  }
}
