package org.cloudsmith.jenkins.stackhammer.common;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.Api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.MessageWithSeverity;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;

public abstract class StackOpResult<T> implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private ResultWithDiagnostic<Repository> cloneDiagnostic;

	private ResultWithDiagnostic<T> resultDiagnostic;

	public Api getApi() {
		return new Api(this);
	}

	public List<Diagnostic> getCloneDiagnostics() {
		return cloneDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: cloneDiagnostic.getChildren();
	}

	public String getIconFileName() {
		return Functions.getResourcePath() + "/plugin/stackhammer/icons/hammer-32x32.png";
	}

	public String getLargeIconFileName() {
		return "/plugin/stackhammer/icons/hammer-48x48.png";
	}

	protected Repository getRepository() {
		return cloneDiagnostic == null
				? null
				: cloneDiagnostic.getResult();
	}

	public T getResult() {
		return resultDiagnostic == null
				? null
				: resultDiagnostic.getResult();
	}

	public List<Diagnostic> getResultDiagnostics() {
		return resultDiagnostic == null
				? Collections.<Diagnostic> emptyList()
				: resultDiagnostic.getChildren();
	}

	public int getResultDiagnosticsCount() {
		return getResultDiagnostics().size();
	}

	protected String getSummary(List<? extends MessageWithSeverity> messages) {
		int errorCount = 0;
		int warningCount = 0;
		for(MessageWithSeverity msg : messages) {
			switch(msg.getSeverity()) {
				case MessageWithSeverity.WARNING:
					warningCount++;
					break;
				case MessageWithSeverity.ERROR:
				case MessageWithSeverity.FATAL:
					errorCount++;
			}
		}

		if(errorCount == 0) {
			if(warningCount == 0)
				return "No errors or warnings";
			return warningCount + (warningCount > 1
					? " warnings"
					: " warning");
		}

		if(warningCount == 0)
			return errorCount + (errorCount > 1
					? " errors"
					: " error");
		return errorCount + (errorCount > 1
				? " errors and "
				: " error and ") + warningCount + (warningCount > 1
				? " warnings"
				: " warning");
	}

	protected String getUrlFor(String item) {
		return getUrlName() + '/' + item;
	}

	/**
	 * @param cloneDiagnostic the cloneDiagnostic to set
	 */
	public void setCloneDiagnostic(ResultWithDiagnostic<Repository> cloneDiagnostic) {
		this.cloneDiagnostic = cloneDiagnostic;
	}

	public void setResult(ResultWithDiagnostic<T> resultDiagnostic) {
		this.resultDiagnostic = resultDiagnostic;
	}
}
