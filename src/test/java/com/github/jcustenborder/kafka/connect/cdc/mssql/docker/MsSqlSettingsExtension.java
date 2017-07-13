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
package com.github.jcustenborder.kafka.connect.cdc.mssql.docker;

import com.github.jcustenborder.kafka.connect.cdc.docker.SettingsExtension;
import com.github.jcustenborder.kafka.connect.cdc.mssql.MsSqlTestConstants;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.Container;
import com.palantir.docker.compose.connection.DockerPort;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

public class MsSqlSettingsExtension extends SettingsExtension {
  @Override
  protected List<Class<? extends Annotation>> annotationClasses() {
    return Arrays.asList(MsSqlSettings.class);
  }

  @Override
  protected Object handleResolve(ParameterContext parameterContext, ExtensionContext extensionContext, Annotation annotation, DockerComposeRule docker) throws ParameterResolutionException {
    Container container = docker.containers().container(MsSqlTestConstants.CONTAINER_NAME);
    DockerPort dockerPort = container.port(MsSqlTestConstants.PORT);
    return MsSqlTestConstants.settings(dockerPort.getIp(), dockerPort.getExternalPort());
  }
}
