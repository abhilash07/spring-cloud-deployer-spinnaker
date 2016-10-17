/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.spi.deployer.spinnaker.cloudfoundry;

import com.netflix.spinnaker.clouddriver.cf.security.CloudFoundryAccountCredentials;
import com.netflix.spinnaker.clouddriver.cf.utils.CloudFoundryClientFactory;
import com.netflix.spinnaker.clouddriver.cf.utils.DefaultCloudFoundryClientFactory;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Greg Turnquist
 */
public class CloudFoundryTestSupport extends AbstractExternalResourceTestSupport<CloudFoundryClient> {

	private ConfigurableApplicationContext context;

	protected CloudFoundryTestSupport() {
		super("SPINNAKER-CLOUDFOUNDRY");
	}

	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		context = new SpringApplicationBuilder(Config.class).web(false).run();
		resource = context.getBean(CloudFoundryClient.class);
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		CloudFoundryAccountCredentials cloudFoundryAccountCredentials() {
			return null;
		}

		@Bean
		CloudFoundryClientFactory cloudFoundryClientFactory() {
			return new DefaultCloudFoundryClientFactory();
		}

		@Bean
		CloudFoundryOperations cloudFoundryClient(CloudFoundryClientFactory factory, CloudFoundryAccountCredentials credentials) {
			return factory.createCloudFoundryClient(credentials, true);
		}

	}
}
