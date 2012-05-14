package org.cloudsmith.jenkins.stackhammer.validation;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.cloudsmith.jenkins.stackhammer.common.GraphTrimmer;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class ValidationResult implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private final AbstractBuild<?, ?> build;

	private ResultWithDiagnostic<Repository> cloneDiagnostic;

	private ResultWithDiagnostic<String> validationDiagnostic;

	public ValidationResult(AbstractBuild<?, ?> build) {
		this.build = build;
	}

	@Override
	public ValidationResult clone() {
		ValidationResult clone;
		try {
			clone = (ValidationResult) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException("Error cloning BuildData", e);
		}
		return clone;
	}

	/**
	 * Method called by the Stapler dispatcher. It is automatically detected
	 * when the dispatcher looks for methods that starts with &quot;do&quot;
	 * The method doValidation corresponds to the path <build>/stackhammerValidation/dependencyGraph
	 * 
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doDependencyGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath();
		if((name == null || name.isEmpty()) && validationDiagnostic != null && validationDiagnostic.getResult() != null) {
			rsp.setContentType("image/svg+xml");
			OutputStream out = rsp.getOutputStream();
			try {
				byte[] svgData = Base64.decodeBase64(validationDiagnostic.getResult());
				svgData = GraphTrimmer.stripFixedSize(svgData);
				rsp.setContentLength(svgData.length);
				out.write(svgData);
				return;
			}
			finally {
				out.close();
			}
		}
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public Api getApi() {
		return new Api(this);
	}

	public String getDisplayName() {
		return "Validation Diagnostics";
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/stackhammer/icons/hammer-32x32.png";
	}

	public String getStackBase() {
		if(cloneDiagnostic == null)
			return null;

		Repository repo = cloneDiagnostic.getResult();
		return repo == null
				? null
				: repo.getProvider().getRepositoryBase(repo.getOwner(), repo.getName(), repo.getBranch());
	}

	public String getSummaryValidationGraphURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? getUrlFor("dependencyGraph")
				: null;
	}

	private String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	@Override
	public String getUrlName() {
		return "stackhammerValidation";
	}

	public List<Diagnostic> getValidationDiagnostics() {
		return validationDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: validationDiagnostic.getChildren();
	}

	public String getValidationGraphURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? "dependencyGraph"
				: null;
	}

	/**
	 * @param cloneDiagnostic the cloneDiagnostic to set
	 */
	public void setCloneDiagnostic(ResultWithDiagnostic<Repository> cloneDiagnostic) {
		this.cloneDiagnostic = cloneDiagnostic;
	}

	/**
	 * @param validationDiagnostic the validationDiagnostic to set
	 */
	public void setValidationDiagnostic(ResultWithDiagnostic<String> validationDiagnostic) {
		this.validationDiagnostic = validationDiagnostic;
	}
}
