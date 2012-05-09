package org.cloudsmith.jenkins.stackhammer;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.Api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.cloudsmith.stackhammer.api.model.Diagnostic;
import org.cloudsmith.stackhammer.api.model.Repository;
import org.cloudsmith.stackhammer.api.model.ResultWithDiagnostic;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.ExportedBean;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

@ExportedBean(defaultVisibility = 999)
public class BuildData implements Action, Serializable, Cloneable {
	private static final long serialVersionUID = 264848698476660935L;

	private final AbstractBuild<?, ?> build;

	private final String pluginName;

	private ResultWithDiagnostic<Repository> cloneDiagnostic;

	private ResultWithDiagnostic<String> validationDiagnostic;

	// @fmtOff
	private static final String XSL_TO_STRIP_FIXED_SIZE = 
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+	"<transform version=\"1.0\" xmlns=\"http://www.w3.org/1999/XSL/Transform\" xmlns:svg=\"http://www.w3.org/2000/svg\">\n"
		+	"	<output method=\"xml\" encoding=\"UTF-8\" omit-xml-declaration=\"no\"/>"
		+	"	<template match=\"svg:svg/@width\"/>\n"
		+	"	<template match=\"svg:svg/@height\"/>\n"
		+	"	<template match=\"node()|@*\">\n"
		+	"		<copy>\n"
		+ 	"			<apply-templates select=\"@*|node()\" />\n"
		+	"		</copy>\n"
		+	"	</template>\n"
		+	"</transform>\n";

	// @fmtOn

	private static final Templates STRIP_FIXED_SIZE_TEMPLATE;

	static {
		TransformerFactory tFactory = TransformerFactory.newInstance();
		try {
			STRIP_FIXED_SIZE_TEMPLATE = tFactory.newTemplates(new StreamSource(
				new StringReader(XSL_TO_STRIP_FIXED_SIZE)));
		}
		catch(TransformerConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

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

	/**
	 * Method called by the Stapler dispatcher. It is automatically detected
	 * when the dispatcher looks for methods that starts with &quot;do&quot;
	 * The method doValidation corresponds to the path <build>/stackhammer/validation/
	 * 
	 * @param req
	 * @param rsp
	 * @throws IOException
	 */
	public void doValidation(StaplerRequest req, StaplerResponse rsp) throws IOException {
		String name = req.getRestOfPath(); // Remove leading /
		if("/graph.svg".equals(name)) {
			if(validationDiagnostic != null && validationDiagnostic.getResult() != null) {
				rsp.setContentType("image/svg+xml");
				OutputStream out = rsp.getOutputStream();
				try {
					byte[] svgData = Base64.decodeBase64(validationDiagnostic.getResult());
					svgData = stripFixedSize(svgData);
					rsp.setContentLength(svgData.length);
					out.write(svgData);
					return;
				}
				finally {
					out.close();
				}
			}
		}
		if("/diagnostics.txt".equals(name)) {
			if(validationDiagnostic != null) {
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

	public Api getApi() {
		return new Api(this);
	}

	public String getDisplayName() {
		return "Validation Diagnostics";
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
				? getUrlFor("validation/graph.svg")
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
				? "validation/diagnostics.txt"
				: null;
	}

	public String getValidationGraphURL() {
		return validationDiagnostic != null && validationDiagnostic.getResult() != null
				? "validation/graph.svg"
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

	private byte[] stripFixedSize(byte[] bytes) {
		try {
			Transformer transformer = STRIP_FIXED_SIZE_TEMPLATE.newTransformer();
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			SAXSource saxSource = new SAXSource(reader, new InputSource(new ByteArrayInputStream(bytes)));
			transformer.transform(saxSource, new StreamResult(result));
			return result.toByteArray();
		}
		catch(RuntimeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
