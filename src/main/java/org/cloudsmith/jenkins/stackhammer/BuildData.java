package org.cloudsmith.jenkins.stackhammer;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private final AbstractBuild<?, ?> build;

	private final String pluginName;

	private ResultWithDiagnostic<Repository> cloneDiagnostic;

	private ResultWithDiagnostic<String> validationDiagnostic;

	public BuildData(AbstractBuild<?, ?> build, String pluginName) {
		this.build = build;
		this.pluginName = pluginName;
	}

	@Override
	public BuildData clone() {
		BuildData clone;
		try {
			clone = (BuildData) super.clone();
		}
		catch(CloneNotSupportedException e) {
			throw new RuntimeException("Error cloning BuildData", e);
		}
		return clone;
	}

	public void doDiag(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath(); // Remove leading /
		if("/clone.txt".equals(name)) {
			if(cloneDiagnostic != null) {
				rsp.setContentType("text/plain");
				PrintWriter out = rsp.getWriter();
				try {
					out.println(cloneDiagnostic);
					return;
				}
				finally {
					out.close();
				}
			}
		}
		if("/validation.txt".equals(name)) {
			if(cloneDiagnostic != null) {
				rsp.setContentType("text/plain");
				PrintWriter out = rsp.getWriter();
				try {
					out.println(validationDiagnostic);
					return;
				}
				finally {
					out.close();
				}
			}
		}
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/**
	 * Method called by the Stapler dispatcher. It is automatically detected
	 * when the dispatcher looks for methods that starts with &quot;do&quot;
	 * The method doGraph corresponds to the path <build>/stackhammer/graph/
	 * 
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath(); // Remove leading /
		if("/validation.svg".equals(name)) {
			if(validationDiagnostic != null && validationDiagnostic.getResult() != null) {
				rsp.setContentType("image/svg+xml");
				OutputStream out = rsp.getOutputStream();
				try {
					byte[] svgData = Base64.decodeBase64(validationDiagnostic.getResult());
					rsp.setContentLength(svgData.length);
					out.write(svgData);
					return;
				}
				finally {
					out.close();
				}
			}
		}
		rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public Api getApi() {
		return new Api(this);
	}

	public String getCloneDiagURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? "diag/clone.txt"
				: null;
	}

	public String getDisplayName() {
		return "Validation Result";
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/" + pluginName + "/icons/hammer-32x32.png";
	}

	public String getPluginName() {
		return pluginName;
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
				? getUrlFor("graph/validation.svg")
				: null;
	}

	private String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	@Override
	public String getUrlName() {
		return getPluginName();
	}

	public List<Diagnostic> getValidationDiagnostics() {
		return validationDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: validationDiagnostic.getChildren();
	}

	public String getValidationDiagURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? "diag/validation.txt"
				: null;
	}

	public String getValidationGraphURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? "graph/validation.svg"
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
