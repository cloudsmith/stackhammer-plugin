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

import org.cloudsmith.jenkins.stackhammer.common.StackOpDescriptor;

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
	public DeploymentDescriptor() {
		super(Deployer.class);
	}

	/**
	 * This human readable name is used in the configuration screen.
	 */
	@Override
	public String getDisplayName() {
		return "Stack Hammer Deployment";
	}
}
