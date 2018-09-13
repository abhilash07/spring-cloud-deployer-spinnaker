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

import com.netflix.spinnaker.clouddriver.cf.utils.CloudFoundryClientFactory;
import com.netflix.spinnaker.clouddriver.cf.utils.DefaultRestTemplateFactory;
import com.netflix.spinnaker.clouddriver.cf.utils.RestTemplateFactory;
import com.netflix.spinnaker.clouddriver.data.task.DefaultTask;
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository;
import com.netflix.spinnaker.clouddriver.helpers.OperationPoller;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.UrlResource;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.rules.ExpectedException.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

/**
 * @author Greg Turnquist
 */
public class SpinnakerCloudFoundryDeployerTests {

	@Rule
	public ExpectedException thrown = none();

	private SpinnakerCloudFoundryDeployer deployer;

	private CloudFoundryClientFactory cloudFoundryClientFactory;

	private CloudFoundryOperations cloudFoundryOperations;

	private CloudFoundryClient cloudFoundryClient;

	private OperationPoller operationPoller;

	private RestTemplateFactory restTemplateFactory;

	private RestTemplate restTemplate;

	@Before
	public void setUp() {
		TaskRepository.threadLocalTask.set(new DefaultTask("TEST"));

		cloudFoundryClientFactory = mock(CloudFoundryClientFactory.class);
		cloudFoundryOperations = mock(CloudFoundryOperations.class);
		cloudFoundryClient = mock(CloudFoundryClient.class);
		operationPoller = new OperationPoller(10, 1);
		restTemplateFactory = new DefaultRestTemplateFactory();

		deployer = new SpinnakerCloudFoundryDeployer(cloudFoundryClientFactory, operationPoller, restTemplateFactory);
	}

	@Test
	public void shouldHandleBasicDeploy() throws MalformedURLException {
		// given
		given(cloudFoundryClientFactory.createCloudFoundryClient(any(), eq(true))).willReturn(cloudFoundryClient);
		given(cloudFoundryClient.getApplicationInstances("app-stack-v000")).willReturn(new InstancesInfo(Collections.singletonList(new HashMap(){{
			put("state", "RUNNING");
		}})));

		// when
		deployer.deploy(new AppDeploymentRequest(
			new AppDefinition("app-stack", Collections.emptyMap()),
			new UrlResource("http://central.maven.org/maven2/org/springframework/spring-core/4.3.3.RELEASE/spring-core-4.3.3.RELEASE.jar"),
			new HashMap<>()));

		// then
		List<String> statuses = TaskRepository.threadLocalTask.get().getHistory().stream()
			.map(status -> status.getStatus())
			.collect(Collectors.toList());

		assertThat(statuses.size(), equalTo(12));
		assertThat(statuses.get(0), equalTo("Creating task TEST"));
		assertThat(statuses.get(1), equalTo("Initializing handler..."));
		assertThat(statuses.get(2), equalTo("Found next sequence 000."));
		assertThat(statuses.get(3), equalTo("Creating application app-stack-v000"));
		assertThat(statuses.get(4), equalTo("Memory set to 1024"));
		assertThat(statuses.get(5), equalTo("Disk limit set to 1024"));
		assertThat(statuses.get(6), equalTo("Successfully downloaded 1110374 bytes"));
		assertThat(statuses.get(7), equalTo("Uploading 1110374 bytes to app-stack-v000"));
		assertThat(statuses.get(8), containsString("Deleted"));
		assertThat(statuses.get(9), equalTo("Setting environment variables..."));
		assertThat(statuses.get(10), equalTo("Starting app-stack-v000"));
		assertThat(statuses.get(11), equalTo("Done operating on app-stack-v000."));
	}


	@Test
	public void shouldHandleBasicUndeploy() {
		// given
		given(cloudFoundryClientFactory.createCloudFoundryClient(any(), eq(true))).willReturn(cloudFoundryOperations);
		given(cloudFoundryOperations.getApplications()).willReturn(Collections.singletonList(new CloudApplication(null, "bar")));

		// when
		deployer.undeploy("foo");

		// then
		List<String> statuses = TaskRepository.threadLocalTask.get().getHistory().stream()
			.map(status -> status.getStatus())
			.collect(Collectors.toList());

		assertThat(statuses.size(), equalTo(4));
		assertThat(statuses.get(0), equalTo("Creating task TEST"));
		assertThat(statuses.get(1), equalTo("Initializing destruction of server group foo in ..."));
		assertThat(statuses.get(2), equalTo("Done operating on foo."));
		assertThat(statuses.get(3), equalTo("Done destroying server group foo in ."));
	}            


}
