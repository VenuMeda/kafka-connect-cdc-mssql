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
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MsSqlQueryBuilderTest {
  Connection connection;
  PreparedStatement statement;


  @BeforeEach
  public void before() throws SQLException {
    this.connection = mock(Connection.class);
    this.statement = mock(PreparedStatement.class);
  }

  @Test
  public void singlePrimaryKey() throws SQLException {
    TableMetadataProvider.TableMetadata tableMetadata = mock(TableMetadataProvider.TableMetadata.class);
    when(tableMetadata.keyColumns()).thenReturn(ImmutableSet.of("user_id"));
    when(tableMetadata.tableName()).thenReturn("users");
    when(tableMetadata.schemaName()).thenReturn("dbo");

    MsSqlQueryBuilder builder = new MsSqlQueryBuilder(this.connection);

    final String expected = "SELECT " +
        "[ct].[sys_change_version] AS [__metadata_sys_change_version], " +
        "[ct].[sys_change_creation_version] AS [__metadata_sys_change_creation_version], " +
        "[ct].[sys_change_operation] AS [__metadata_sys_change_operation], " +
        "[u].* " +
        "FROM [dbo].[users] AS [u] " +
        "RIGHT OUTER JOIN " +
        "CHANGETABLE(CHANGES [dbo].[users], ?) AS [ct] " +
        "ON " +
        "[ct].[user_id] = [u].[user_id]";

    final String actual = builder.changeTrackingStatementQuery(tableMetadata);
    assertEquals(expected, actual, "Query should match.");
  }

  @Test
  public void multiplePrimaryKey() throws SQLException {
    TableMetadataProvider.TableMetadata tableMetadata = mock(TableMetadataProvider.TableMetadata.class);
    when(tableMetadata.keyColumns()).thenReturn(ImmutableSet.of("first_key", "second_key"));
    when(tableMetadata.tableName()).thenReturn("users");
    when(tableMetadata.schemaName()).thenReturn("dbo");

    MsSqlQueryBuilder builder = new MsSqlQueryBuilder(this.connection);

    final String expected = "SELECT " +
        "[ct].[sys_change_version] AS [__metadata_sys_change_version], " +
        "[ct].[sys_change_creation_version] AS [__metadata_sys_change_creation_version], " +
        "[ct].[sys_change_operation] AS [__metadata_sys_change_operation], " +
        "[u].* " +
        "FROM [dbo].[users] AS [u] " +
        "RIGHT OUTER JOIN " +
        "CHANGETABLE(CHANGES [dbo].[users], ?) AS [ct] " +
        "ON " +
        "[ct].[first_key] = [u].[first_key] AND " +
        "[ct].[second_key] = [u].[second_key]";

    final String actual = builder.changeTrackingStatementQuery(tableMetadata);
    assertEquals(expected, actual, "Query should match.");
  }

}
