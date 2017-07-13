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

import com.github.jcustenborder.kafka.connect.cdc.BaseServiceTask;
import com.github.jcustenborder.kafka.connect.cdc.ChangeWriter;
import com.github.jcustenborder.kafka.connect.cdc.TableMetadataProvider;
import com.google.common.util.concurrent.Service;
import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.storage.OffsetStorageReader;

import java.util.Map;

public class MsSqlSourceTask extends BaseServiceTask<MsSqlSourceConnectorConfig> {
  ChangeWriter changeWriter;
  Time time = new SystemTime();
  TableMetadataProvider tableMetadataProvider;

  @Override
  protected Service service(ChangeWriter changeWriter, OffsetStorageReader offsetStorageReader) {
    this.changeWriter = changeWriter;
    this.tableMetadataProvider = new MsSqlTableMetadataProvider(this.config, offsetStorageReader);
    return new QueryService(this.time, this.tableMetadataProvider, this.config, this.changeWriter);
  }

  @Override
  protected MsSqlSourceConnectorConfig getConfig(Map<String, String> map) {
    return new MsSqlSourceConnectorConfig(map);
  }
}