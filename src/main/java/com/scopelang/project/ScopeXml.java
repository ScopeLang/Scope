package com.scopelang.project;

import java.io.File;

import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.scopelang.Scope;
import com.scopelang.Utils;

public class ScopeXml {
	public String mode = null;
	public File mainFile = null;

	public ScopeXml(File file) {
		try {
			parse(file);
		} catch (Exception e) {
			Utils.error("Could not parse `.scope.xml`!", "Are there any syntax errors?");
			Utils.forceExit();
		}
	}

	private void parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);

		document.normalize();
		if (!document.getDocumentElement().getNodeName().equals("scope")) {
			Utils.error("`.scope.xml` document node must be named `scope`.",
				"The document node is instead named `" + document.getDocumentElement().getNodeName() + "`.");
			throw new Exception();
		}

		NodeList modeNode = document.getElementsByTagName("mode");
		if (modeNode.getLength() != 1) {
			Utils.error("There must be exactly 1 node named `mode` in `.scope.xml`.",
				"Try adding the following or remove extras:",
				"<mode>project</mode>");
			throw new Exception();
		}

		mode = modeNode.item(0).getTextContent();
		if (!mode.equals("project")) {
			Utils.error("Unknown project mode `" + mode + "`.",
				"Try changing the mode to `project`.");
			throw new Exception();
		}

		NodeList mainNode = document.getElementsByTagName("main");
		if (mainNode.getLength() != 1) {
			Utils.error("There must be exactly 1 node named `main` in `.scope.xml`.",
				"Try adding the following or remove extras:",
				"<main>HelloWorld.scope</main>");
			throw new Exception();
		}

		mainFile = new File(Scope.workingDir, mainNode.item(0).getTextContent());
		if (!mainFile.exists()) {
			Utils.error("File referenced in `main` doesn't exist.",
				"Try making a file named `" + mainFile.getName() + "`.");
			throw new Exception();
		}
	}
}
