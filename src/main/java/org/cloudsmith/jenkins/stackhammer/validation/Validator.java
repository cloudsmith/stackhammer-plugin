/**
 * Copyright 2012-, Cloudsmith Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.cloudsmith.jenkins.stackhammer.validation;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;

import java.io.PrintStream;
import java.net.URI;

import org.cloudsmith.stackhammer.api.StackHammerModule;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.Provider;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;
import org.cloudsmith.stackhammer.api.service.RepositoryService;
import org.cloudsmith.stackhammer.api.service.StackHammerFactory;
import org.cloudsmith.stackhammer.api.service.StackService;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A {@link Builder} that performs Stack Hammer Validation
 */
public class Validator extends Builder {

	private final String branch;

	private final String stack;

	private final String apiKey;

	@DataBoundConstructor
	public Validator(String stack, String branch, String apiKey) {
		this.stack = stack;
		this.branch = branch;
		this.apiKey = apiKey;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBranch() {
		return branch;
	}

	@Override
	public ValidationDescriptor getDescriptor() {
		return (ValidationDescriptor) super.getDescriptor();
	}

	public String getStack() {
		return stack;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		try {
			PrintStream logger = listener.getLogger();
			String serverURL = getDescriptor().getServiceURL();

			URI uri = URI.create(serverURL);
			logger.format(
				"Using parameters%n scheme=%s%n host=%s%n port=%s%n prefix=%s%n", uri.getScheme(), uri.getHost(),
				uri.getPort(), uri.getPath());

			Injector injector = Guice.createInjector(new StackHammerModule(
				uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), getApiKey()));

			ValidationResult data = new ValidationResult(build);
			build.addAction(data);

			StackHammerFactory factory = injector.getInstance(StackHammerFactory.class);
			RepositoryService repoService = factory.createRepositoryService();
			String[] splitName = getStack().split("/");
			String owner = splitName[0];
			String name = splitName[1];
			logger.format(
				"Verifying that a local clone of repository %s/%s[%s] exists at Stack Hammer Service%n", owner, name,
				branch);
			ResultWithDiagnostic<Repository> cloneResult = repoService.cloneRepository(
				Provider.GITHUB, owner, name, branch);

			data.setCloneDiagnostic(cloneResult);

			if(cloneResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(cloneResult.toString());
				return false;
			}
			cloneResult.log(logger);

			StackService stackService = factory.createStackService();
			Repository repo = cloneResult.getResult();

			logger.format("Sending order to validate stack %s/%s%n", owner, name);
			ResultWithDiagnostic<String> validationResult = stackService.validateStack(repo, repo.getOwner() + "/" +
					repo.getName());

			data.setResult(validationResult);

			if(validationResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(validationResult.toString());
				return false;
			}
			validationResult.log(logger);
		}
		catch(Exception e) {
			e.printStackTrace(listener.error("Exception during validation of %s", getStack()));
			return false;
		}
		return true;
	}
}
