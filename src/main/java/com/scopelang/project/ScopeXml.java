package com.scopelang.project;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.scopelang.Scope;
import com.scopelang.Utils;

import net.lingala.zip4j.ZipFile;

public class ScopeXml {
	public class LibraryInfo {
		public String type;
		public String path;
		public String name;

		public LibraryInfo() {

		}
	}

	public String mode = null;
	public File mainFile = null;
	public String name = null;
	public ArrayList<LibraryInfo> libraries = new ArrayList<>();

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
			// Get the type
			String type = "file";
			var typeAttrib = libs.item(i).getAttributes().getNamedItem("type");
			if (typeAttrib != null) {
				type = typeAttrib.getTextContent();
			}

			// Check the type
			if (!type.equals("file") && !type.equals("remote")) {
				Utils.error("Invalid library type `" + type + "`.",
					"Try one of the following options: file, remote");
				throw new Exception();
			}

			// Get the path
			String path = libs.item(i).getTextContent();

			if (type.equals("file")) {
				// Check the path (if file)
				if (path == null || path.equals("") || path.equals(".")) {
					Utils.error("Invalid library path!");
					throw new Exception();
				}
			}

			// Create library info
			LibraryInfo lib = new LibraryInfo();
			lib.type = type;
			lib.path = path;

			// Done!
			libraries.add(lib);
		}
	}

	public void solveLibraries() {
		boolean errored = false;

		for (var lib : libraries) {
			// Download and extract library if needed
			if (lib.type.equals("remote")) {
				// Check if the library is already downloaded
				String urlMd5 = DigestUtils.md5Hex(lib.path.getBytes());
				File expectedFolder = new File(Scope.libDir, urlMd5);

				// If not, download it
				if (!expectedFolder.exists()) {
					if (!downloadLib(lib.path, expectedFolder)) {
						errored = true;
						continue;
					}
				}

				// Then, set the path correctly
				lib.path = Utils.pathRelativeToWorkingDir(expectedFolder.toPath()).toString();
			}

			// Get the name
			File f = new File(Scope.workingDir, lib.path + File.separator + ".scope.xml");
			if (!f.exists()) {
				Utils.error("`" + lib.path + "` is an invalid library.",
					"No `.scope.xml` file was found.");
				errored = true;
				continue;
			}
			ScopeXml xml = new ScopeXml(f);
			lib.name = xml.name;
		}

		if (errored) {
			Utils.forceExit();
		}
	}

	private static boolean downloadLib(String path, File expectedFolder) {
		Utils.log("Downloading `" + path + "`...");

		// Download
		File zip = new File(Scope.cacheDir, "lib.zip");
		try {
			FileUtils.copyURLToFile(new URL(path), zip);
		} catch (Exception e) {
			Utils.error("Failed to download from `" + path + "`.",
				"Is the URL correct? Are you using the right library type?");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
			return false;
		}

		Utils.log("Extracting `" + path + "`...");

		// Extract
		try (ZipFile zipFile = new ZipFile(zip)) {
			zipFile.extractAll(expectedFolder.getPath());
		} catch (Exception e) {
			Utils.error("Failed to extract zip from `" + path + "`.",
				"Is the zip corrupt?");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
			return false;
		}

		// Delete zip
		zip.delete();

		return true;
	}

	public LibraryInfo libraryInfoByName(String name) {
		for (var lib : libraries) {
			if (lib.name.equals(name)) {
				return lib;
			}
		}

		return null;
	}
}