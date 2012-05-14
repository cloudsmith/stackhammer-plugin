package org.cloudsmith.jenkins.stackhammer.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class GraphTrimmer {
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

	public static byte[] stripFixedSize(byte[] bytes) {
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
