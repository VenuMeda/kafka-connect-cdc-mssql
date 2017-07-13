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
package com.github.jcustenborder.kafka.connect.cdc.mssql.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.jcustenborder.kafka.connect.cdc.JsonChange;
import com.github.jcustenborder.kafka.connect.cdc.JsonTableMetadata;
import com.github.jcustenborder.kafka.connect.cdc.JsonTime;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MsSqlQueryBuilderTestData {
  JsonTableMetadata tableMetadata;
  JsonTime time;
  JsonChange expected;

  public JsonChange expected() {
    return this.expected;
  }

  public void expected(JsonChange expected) {
    this.expected = expected;
  }

  public JsonTableMetadata tableMetadata() {
    return this.tableMetadata;
  }

  public void tableMetadata(JsonTableMetadata tableMetadata) {
    this.tableMetadata = tableMetadata;
  }

  public JsonTime time() {
    return this.time;
  }

  public void time(JsonTime time) {
    this.time = time;
  }
}
