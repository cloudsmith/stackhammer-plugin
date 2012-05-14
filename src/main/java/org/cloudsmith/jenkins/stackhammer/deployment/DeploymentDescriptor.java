package org.cloudsmith.jenkins.stackhammer.deployment;

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

import hudson.Extension;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;

import org.cloudsmith.jenkins.stackhammer.common.StackOpDescriptor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link Deployer}. Used as a singleton.
 * The class is marked as public so that it can be accessed from views.
 * 
 * <p>
 * See <tt>src/main/resources/com/cloudsmith/hammer/jenkins/deployment/Deployer/*.jelly</tt>
 * for the actual HTML fragment for the configuration screen.
 */
@Extension
public final class DeploymentDescriptor extends StackOpDescriptor<Builder> {
	private int pollFrequency;

	private int maxTime;

	public DeploymentDescriptor() {
		super(Deployer.class);
		load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		pollFrequency = formData.getInt("pollFrequency");
		maxTime = formData.getInt("maxTime");
		save();
		return super.configure(req, formData);
	}

	/**
	 * This human readable name is used in the configuration screen.
	 */
	@Override
	public String getDisplayName() {
		return "Stack Hammer Deployment";
	}

	/**
	 * This method returns the max time from the global configuration
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * This method returns the poll frequency from the global configuration
	 */
	public int getPollFrequency() {
		return pollFrequency;
	}
}
