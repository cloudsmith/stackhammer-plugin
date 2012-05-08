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

package org.cloudsmith.hammer.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link StackValidatorBuilder}. Used as a singleton.
 * The class is marked as public so that it can be accessed from views.
 * 
 * <p>
 * See <tt>src/main/resources/com/cloudsmith/hammer/jenkins/StackValidatorBuilder/*.jelly</tt>
 * for the actual HTML fragment for the configuration screen.
 */
@Extension
public final class StackValidatorDescriptor extends BuildStepDescriptor<Builder> {
	private String serverURL;

	private String webURL;

	public StackValidatorDescriptor() {
		super(StackValidatorBuilder.class);
		load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
		serverURL = formData.getString("serverURL");
		webURL = formData.getString("webURL");
		save();
		return super.configure(req, formData);
	}

	/**
	 * Performs on-the-fly validation of the form field 'serverURL'.
	 * 
	 * @param value
	 *        This parameter receives the value that the user has typed.
	 * @return
	 *         Indicates the outcome of the validation. This is sent to the browser.
	 */
	public FormValidation doCheckServerURL(@QueryParameter String value) throws IOException, ServletException {
		if(value.length() == 0)
			// This is OK, we'll use the default
			return FormValidation.ok();

		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
				return FormValidation.ok();
			return FormValidation.error("The Server URL must use http or https");
		}
		catch(URISyntaxException e) {
			return FormValidation.error("The Server URL is not syntactially correct: %s", e.getMessage());
		}
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
	 * Performs on-the-fly validation of the form field 'serverURL'.
	 * 
	 * @param value
	 *        This parameter receives the value that the user has typed.
	 * @return
	 *         Indicates the outcome of the validation. This is sent to the browser.
	 */
	public FormValidation doCheckWebURL(@QueryParameter String value) throws IOException, ServletException {
		if(value.length() == 0)
			// This is OK, we'll use the default
			return FormValidation.ok();

		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			if("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
				return FormValidation.ok();
			return FormValidation.error("The Web URL must use http or https");
		}
		catch(URISyntaxException e) {
			return FormValidation.error("The Web URL is not syntactially correct: %s", e.getMessage());
		}
	}

	/**
	 * This human readable name is used in the configuration screen.
	 */
	@Override
	public String getDisplayName() {
		return "Stack Hammer Validation";
	}

	/**
	 * This method returns the serverURL of the global configuration.
	 */
	public String getServerURL() {
		return serverURL;
	}

	/**
	 * This method returns the webURL of the global configuration.
	 */
	public String getWebURL() {
		return webURL;
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
