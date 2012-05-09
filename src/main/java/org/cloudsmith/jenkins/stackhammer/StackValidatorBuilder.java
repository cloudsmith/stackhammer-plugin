/**
 * Copyright (c) 2012 Cloudsmith Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Thomas Hallgren (Cloudsmith Inc.) - initial API and implementation
 */
package org.cloudsmith.jenkins.stackhammer;

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
public class StackValidatorBuilder extends Builder {

	private final String branch;

	private final String stackName;

	private final String accessKey;

	@DataBoundConstructor
	public StackValidatorBuilder(String stackName, String branch, String accessKey) {
		this.stackName = stackName;
		this.branch = branch;
		this.accessKey = accessKey;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public String getBranch() {
		return branch;
	}

	@Override
	public StackValidatorDescriptor getDescriptor() {
		return (StackValidatorDescriptor) super.getDescriptor();
	}

	public String getStackName() {
		return stackName;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		try {
			PrintStream logger = listener.getLogger();
			String serverURL = getDescriptor().getServerURL();
			if(serverURL == null)
				serverURL = "https://stackhammer.cloudsmith.com:443/api";

			URI uri = URI.create(serverURL);
			logger.format(
				"Using parameters%n scheme=%s%n host=%s%n port=%s%n prefix=%s%n token=%s%n", uri.getScheme(),
				uri.getHost(), uri.getPort(), uri.getPath(), getAccessKey());

			Injector injector = Guice.createInjector(new StackHammerModule(
				uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), getAccessKey()));

			BuildData data = new BuildData(build);
			build.addAction(data);

			StackHammerFactory factory = injector.getInstance(StackHammerFactory.class);
			RepositoryService repoService = factory.createRepositoryService();
			String[] splitName = getStackName().split("/");
			String owner = splitName[0];
			String name = splitName[1];
			logger.format("Sending order to clone repository %s/%s to Stack Hammer Service%n", owner, name);
			ResultWithDiagnostic<Repository> cloneResult = repoService.cloneRepository(
				Provider.GITHUB, owner, name, branch);

			data.setCloneDiagnostic(cloneResult);

			if(cloneResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(cloneResult.toString());
				return false;
			}
			logger.format("Clone of %s/%s was a success%n", owner, name);

			StackService stackService = factory.createStackService();
			Repository repo = cloneResult.getResult();
			ResultWithDiagnostic<String> validationResult = stackService.validateStack(repo, repo.getOwner() + "/" +
					repo.getName());

			data.setValidationDiagnostic(validationResult);

			if(validationResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(validationResult.toString());
				return false;
			}
		}
		catch(Exception e) {
			e.printStackTrace(listener.error("Exception during validation of %s", getStackName()));
			return false;
		}
		return true;
	}
}
