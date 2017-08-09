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

import com.google.common.base.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;

public class MsSqlTestConstants {
  public static final String USERNAME = "sa";
  public static final String PASSWORD = "1wRr8jZIxYlBVO";
  public static final int PORT = 1433;
  public static final String JDBCURL_FORMAT_MASTER = "jdbc:sqlserver://$HOST:$EXTERNAL_PORT;databaseName=master";
  public static final String JDBCURL_FORMAT_CDC_TESTING = "jdbc:sqlserver://$HOST:$EXTERNAL_PORT;databaseName=cdc_testing";
  public static final String CONTAINER_NAME = "mssql";
  public static final String DATABASE_NAME = "cdc_testing";
  public static final String DOCKER_COMPOSE_FILE = "src/test/resources/docker-compose.yml";


  public static Map<String, String> settings(String host, Integer port) {
    Preconditions.checkNotNull(host, "host cannot be null");
    Preconditions.checkNotNull(port, "port cannot be null");
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(MsSqlSourceConnectorConfig.SERVER_NAME_CONF, host);
    settings.put(MsSqlSourceConnectorConfig.SERVER_PORT_CONF, port.toString());
    settings.put(MsSqlSourceConnectorConfig.INITIAL_DATABASE_CONF, DATABASE_NAME);
    settings.put(MsSqlSourceConnectorConfig.JDBC_USERNAME_CONF, USERNAME);
    settings.put(MsSqlSourceConnectorConfig.JDBC_PASSWORD_CONF, PASSWORD);
    return settings;
  }

}
