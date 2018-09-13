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

import com.netflix.frigga.Names;
import com.netflix.spinnaker.clouddriver.cf.deploy.description.CloudFoundryDeployDescription;
import com.netflix.spinnaker.clouddriver.cf.deploy.description.DestroyCloudFoundryServerGroupDescription;
import com.netflix.spinnaker.clouddriver.cf.deploy.handlers.CloudFoundryDeployHandler;
import com.netflix.spinnaker.clouddriver.cf.deploy.ops.DestroyCloudFoundryServerGroupAtomicOperation;
import com.netflix.spinnaker.clouddriver.cf.security.CloudFoundryAccountCredentials;
import com.netflix.spinnaker.clouddriver.cf.utils.CloudFoundryClientFactory;
import com.netflix.spinnaker.clouddriver.cf.utils.RestTemplateFactory;
import com.netflix.spinnaker.clouddriver.deploy.DeploymentResult;
import com.netflix.spinnaker.clouddriver.helpers.OperationPoller;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * Deploy apps to Cloud Foundry through Spinnaker.
 *
 * @author Greg Turnquist
 */
public class SpinnakerCloudFoundryDeployer implements AppDeployer {

	private final CloudFoundryClientFactory cloudFoundryClientFactory;

	private final OperationPoller operationPoller;

	private final RestTemplateFactory restTemplateFactory;

	public SpinnakerCloudFoundryDeployer(CloudFoundryClientFactory cloudFoundryClientFactory, OperationPoller operationPoller, RestTemplateFactory restTemplateFactory) {
		this.cloudFoundryClientFactory = cloudFoundryClientFactory;
		this.operationPoller = operationPoller;
		this.restTemplateFactory = restTemplateFactory;
	}

	@Override
	public String deploy(AppDeploymentRequest appDeploymentRequest) {

		CloudFoundryAccountCredentials credentials = new CloudFoundryAccountCredentials();
		credentials.setOrg("spinnaker");

		CloudFoundryDeployDescription description = new CloudFoundryDeployDescription();

		description.setCredentials(credentials);

		Names names = Names.parseName(appDeploymentRequest.getDefinition().getName());
		description.setApplication(names.getApp());
		description.setStack(names.getStack());
		description.setFreeFormDetails(names.getDetail());

		try {
			String[] urlParts = appDeploymentRequest.getResource().getURL().toString().split("/");
			description.setArtifact(urlParts[urlParts.length-1]);
			description.setRepository(StringUtils.arrayToDelimitedString(Arrays.copyOf(urlParts, urlParts.length-1), "/"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		CloudFoundryDeployHandler handler = new CloudFoundryDeployHandler();
		handler.setClientFactory(cloudFoundryClientFactory);
		handler.setRestTemplateFactory(restTemplateFactory);
		handler.setOperationPoller(operationPoller);

		DeploymentResult results = handler.handle(description, Collections.emptyList());

		return results.getServerGroupNameByRegion().values().stream().findFirst().get();
	}

	@Override
	public void undeploy(String appName) {

		CloudFoundryAccountCredentials credentials = new CloudFoundryAccountCredentials();

		DestroyCloudFoundryServerGroupDescription description = new DestroyCloudFoundryServerGroupDescription();
		description.setServerGroupName(appName);
		description.setRegion("");
		description.setCredentials(credentials);

		DestroyCloudFoundryServerGroupAtomicOperation operation =
			new DestroyCloudFoundryServerGroupAtomicOperation(description);
		operation.setCloudFoundryClientFactory(cloudFoundryClientFactory);
		operation.setOperationPoller(operationPoller);

		operation.operate(Collections.emptyList());
	}

	@Override
	public AppStatus status(String appName) {

		CloudFoundryAccountCredentials credentials = new CloudFoundryAccountCredentials();


		cloudFoundryClientFactory.createCloudFoundryClient(credentials, true);

		return null;
	}
}
