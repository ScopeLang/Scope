package com.scopelang.project;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.scopelang.Scope;
import com.scopelang.Utils;

public class ScopeXml {
	public String mode = null;
	public File mainFile = null;
	public String name = null;
	public ArrayList<String> rawLibraries = new ArrayList<>();
	public ArrayList<String> libraries = new ArrayList<>();

	public ScopeXml(File file) {
		try {
			parse(file);
		} catch (Exception e) {
			Utils.error("Could not parse `.scope.xml`!", "Are there any syntax errors?");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
			Utils.forceExit();
		}
	}

	private void parse(File file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);

		// <scope>
		document.normalize();
		if (!document.getDocumentElement().getNodeName().equals("scope")) {
			Utils.error("`.scope.xml` document node must be named `scope`.",
				"The document node is instead named `" + document.getDocumentElement().getNodeName() + "`.");
			throw new Exception();
		}

		// <mode>
		NodeList modeNode = document.getElementsByTagName("mode");
		if (modeNode.getLength() != 1) {
			Utils.error("There must be exactly 1 node named `mode` in `.scope.xml`.",
				"Try adding the following or remove extras:",
				"<mode>project</mode>");
			throw new Exception();
		}

		// Validate <mode>
		mode = modeNode.item(0).getTextContent();
		if (!mode.equals("project") && !mode.equals("library")) {
			Utils.error("Unknown project mode `" + mode + "`.",
				"Try changing the mode to `project` or `library`.");
			throw new Exception();
		}

		switch (mode) {
			case "project":
				// <main>
				NodeList mainNode = document.getElementsByTagName("main");
				if (mainNode.getLength() != 1) {
					Utils.error("There must be exactly 1 node named `main` in `.scope.xml`.",
						"Try adding the following or remove extras:",
						"<main>HelloWorld.scope</main>");
					throw new Exception();
				}

				// Validate <main>
				mainFile = new File(Scope.workingDir, mainNode.item(0).getTextContent());
				if (!mainFile.exists()) {
					Utils.error("File referenced in `main` doesn't exist.",
						"Try making a file named `" + mainFile.getName() + "`.");
					throw new Exception();
				}
				break;

			case "library":
				// <name>
				NodeList nameNode = document.getElementsByTagName("name");
				if (nameNode.getLength() != 1) {
					Utils.error("There must be exactly 1 node named `name` in `.scope.xml`.",
						"Try adding the following or remove extras:",
						"<name>mylib</name>");
					throw new Exception();
				}

				name = nameNode.item(0).getTextContent();
				break;
		}

		// <library>
		NodeList libs = document.getElementsByTagName("library");
		for (int i = 0; i < libs.getLength(); i++) {
			String path = libs.item(i).getTextContent();

			if (path == null || path.equals("") || path.equals(".")) {
				Utils.error("Invalid library path!");
				throw new Exception();
			}

			rawLibraries.add(path);

			File f = new File(Scope.workingDir, path + File.separator + ".scope.xml");
			if (!f.exists()) {
				Utils.error("`" + path + "` is an invalid library.",
					"No `.scope.xml` file was found.");
				throw new Exception();
			}
			ScopeXml xml = new ScopeXml(f);
			libraries.add(xml.name);
		}
	}
}