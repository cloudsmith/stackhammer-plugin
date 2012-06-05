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
package org.cloudsmith.jenkins.stackhammer.deployment;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;

import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import jenkins.model.Jenkins;

import org.cloudsmith.jenkins.stackhammer.validation.ValidationDescriptor;
import org.cloudsmith.jenkins.stackhammer.validation.Validator;
import org.cloudsmith.stackhammer.api.StackHammerModule;
import org.cloudsmith.stackhammer.api.model.CatalogGraph;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.LogEntry;
import org.cloudsmith.stackhammer.api.model.PollResult;
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
public class Deployer extends Builder {

	private final Boolean dryRun;

	private final String branch;

	private final String stack;

	private final String apiKey;

	@DataBoundConstructor
	public Deployer(String stack, Boolean dryRun, String branch, String apiKey) {
		this.stack = stack;
		this.dryRun = dryRun;
		this.branch = branch;
		this.apiKey = apiKey;
	}

	private void emitLogEntries(List<LogEntry> logEntries, DeploymentResult data, PrintStream logger) {
		for(LogEntry logEntry : logEntries)
			logger.println(logEntry);
		data.addLogEntries(logEntries);
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBranch() {
		return branch;
	}

	@Override
	public DeploymentDescriptor getDescriptor() {
		return (DeploymentDescriptor) super.getDescriptor();
	}

	public Boolean getDryRun() {
		return dryRun;
	}

	public String getStack() {
		return stack;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
		try {
			PrintStream logger = listener.getLogger();

			ValidationDescriptor validationDesc = (ValidationDescriptor) Jenkins.getInstance().getDescriptorOrDie(
				Validator.class);

			String serverURL = validationDesc.getServiceURL();
			URI uri = URI.create(serverURL);
			logger.format(
				"Using parameters%n scheme=%s%n host=%s%n port=%s%n prefix=%s%n", uri.getScheme(), uri.getHost(),
				uri.getPort(), uri.getPath());

			Injector injector = Guice.createInjector(new StackHammerModule(
				uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), getApiKey()));

			DeploymentResult data = new DeploymentResult();
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

			Integer pollIntervalObj = validationDesc.getPollInterval();
			long pollInterval = pollIntervalObj == null
					? 0
					: pollIntervalObj.longValue();

			if(pollInterval < 1)
				pollInterval = 1;

			long startTime = System.currentTimeMillis();
			long lastPollTime = startTime;
			long failTime = Long.MAX_VALUE;
			Integer maxTimeObj = validationDesc.getMaxTime();
			if(maxTimeObj != null && maxTimeObj.longValue() > 0)
				failTime = startTime + maxTimeObj.longValue() * 1000;

			Boolean dryRunObj = getDryRun();
			boolean dryRun = dryRunObj == null
					? false
					: dryRunObj.booleanValue();

			String jobIdentifier = stackService.deployStack(repo, repo.getOwner() + "/" + repo.getName(), dryRun);

			logger.format("Sending order to deploy %s/%s to Stack Hammer Service%n", owner, name);
			for(;;) {
				long now = System.currentTimeMillis();
				if(now > failTime) {
					logger.format("Job didn't finish in time.%n");
					return false;
				}

				long sleepTime = (lastPollTime + pollInterval * 1000) - now;
				if(sleepTime > 0)
					Thread.sleep(sleepTime);

				lastPollTime = System.currentTimeMillis();
				PollResult pollResult = stackService.pollJob(jobIdentifier);
				switch(pollResult.getJobState()) {
					case SCHEDULED:
					case STARTING:
						continue;
					case SLEEPING:
					case RUNNING:
						emitLogEntries(pollResult.getLogEntries(), data, logger);
						continue;
					case CANCELLED:
						listener.error("Job was cancelled");
						return false;
					default:
						emitLogEntries(pollResult.getLogEntries(), data, logger);
						break;
				}
				break;
			}

			ResultWithDiagnostic<List<CatalogGraph>> deploymentResult = stackService.getDeploymentResult(jobIdentifier);

			data.setResult(deploymentResult);

			if(deploymentResult.getSeverity() == Diagnostic.ERROR) {
				listener.error(deploymentResult.toString());
				return false;
			}
			deploymentResult.log(logger);
		}
		catch(Exception e) {
			e.printStackTrace(listener.error("Exception during deployment of %s", getStack()));
			return false;
		}
		return true;
	}
}
