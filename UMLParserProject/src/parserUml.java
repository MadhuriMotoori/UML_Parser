
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TreeVisitor;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class parserUml {
	static HashMap<String, Boolean> hashmap = new HashMap<String, Boolean>();
	static ArrayList<CompilationUnit> cuArray;
	String path;
	String outputPath;

	public parserUml(String path, String outputfileName) {
		this.path = path;
		this.outputPath = path + "/" + outputfileName + ".png";
	}

	public void generateUMLDiagram() {
		try {
			createInterfaceMap();
			String resultString = ParseFilesInDir();
			generatePNG(resultString, outputPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Create interface map , which contains information about whether given
	// class is an interface or not
	private void createInterfaceMap() throws Exception {
		cuArray = getCuArray();
		buildInterfaceMap(cuArray);
	}

	private ArrayList<CompilationUnit> getCuArray() throws Exception {
		File folder = new File(path);
		ArrayList<CompilationUnit> cuArray = new ArrayList<CompilationUnit>();
		for (final File f : folder.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".java")) {
				FileInputStream in = new FileInputStream(f);
				CompilationUnit cu;
				try {
					cu = JavaParser.parse(in);
					cuArray.add(cu);
				} finally {
					in.close();
				}
			}
		}
		return cuArray;
	}

	public String ParseFilesInDir() throws IOException, ParseException {
		File rootDir = new File(path);
		File[] files = rootDir.listFiles();
		String filePath = null;
		String result = "";

		for (File f : files) {
			filePath = f.getAbsolutePath();
			if (f.isFile()) {
				if (f.getName().endsWith(".java")) {

					result += parseJavaFile(filePath);

				}

			}
		}

		result = reArrange(result);
		// System.out.println(result);
		return result;
	}

	public String parseJavaFile(String filePath) throws FileNotFoundException, ParseException {
		FileInputStream in = new FileInputStream(filePath);
		CompilationUnit cu = JavaParser.parse(in);

		String output = new JavaClassParser(cu).getClassString();
		return output;
	}

	public String reArrange(String output) {

		String[] tokens = output.split(",");
		String result = "";
		String relationresult = "";
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("1-1") || tokens[i].contains("^-.-") || tokens[i].contains("^-")
					|| tokens[i].contains("-*") || tokens[i].contains("-1") || tokens[i].contains("-.->")) {
				relationresult += tokens[i] + ",";
			} else {
				result += tokens[i] + ",";
			}
		}

		result += relationresult;
		return result;
	}

	public void buildInterfaceMap(ArrayList<CompilationUnit> cuArray) {
		for (CompilationUnit cu : cuArray) {
			List<TypeDeclaration> cl = cu.getTypes();
			for (Node n : cl) {
				ClassOrInterfaceDeclaration coi = (ClassOrInterfaceDeclaration) n;
				hashmap.put(coi.getName(), coi.isInterface());

				if (coi.isInterface()) {
					Utils.interfacesNames.add(coi.getName());
					new InterfaceMethods(cu).buildInterfaceMethodMaps(coi.getName());
				}
			}
		}
	}

	public void generatePNG(String grammar, String outPath) {
		try {
			String webLink = "http://yuml.me/diagram/scruffy/class/%2F%2F Cool Class Diagram," + grammar + ".png";
			URL url = new URL(webLink);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			OutputStream outputStream = new FileOutputStream(new File(outPath));
			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = conn.getInputStream().read(bytes)) != -1) {
				outputStream.write(bytes, 0, read);
			}
			outputStream.close();
			conn.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
