package org.cloudsmith.jenkins.stackhammer.common;

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

import hudson.model.Describable;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

public abstract class StackOpDescriptor<T extends BuildStep & Describable<T>> extends BuildStepDescriptor<T> {
	public StackOpDescriptor(Class<? extends T> builderClass) {
		super(builderClass);
	}

	/**
	 * Performs on-the-fly validation of the form field 'stackName'.
	 * 
	 * @param value
	 *        This parameter receives the value that the user has typed.
	 * @return
	 *         Indicates the outcome of the validation. This is sent to the browser.
	 */
	public FormValidation doCheckStackName(@QueryParameter String value) throws IOException, ServletException {
		if(value.length() == 0)
			return FormValidation.error("Please set a stack name");
		String[] split = value.trim().split("/");
		if(split.length == 2) {
			String owner = split[0].trim();
			String name = split[1].trim();
			if(!(owner.isEmpty() || name.isEmpty())) {
				return FormValidation.ok();
			}
		}
		return FormValidation.error("Stack name must be in the form <owner>/<name>");
	}

	/**
	 * Indicates that this builder can be used with all kinds of project types
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> aClass) {
		return true;
	}
}
