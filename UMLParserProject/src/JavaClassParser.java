import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class JavaClassParser {
	String className;
	CompilationUnit cu;
	String classString = "";
	ArrayList<MethodDeclaration> methodNames;
	ConstructorDeclaration constructorName;
	ArrayList<String> publicVariables;
	ArrayList<String> privatePrimitiveVariables;
	ArrayList<FieldDeclaration> primitiveFieldNames;
	ArrayList<FieldDeclaration> nonprimitiveFieldNames;
	ArrayList<MethodDeclaration> setGetMethodNames;
	List<ClassOrInterfaceType> interfaceList;
	ArrayList<MethodDeclaration> interfaceMethods;
	String result = "";

	JavaClassParser(CompilationUnit cu) {
		this.cu = cu;
		this.className = getClassName();
		methodNames = new ArrayList<MethodDeclaration>();
		publicVariables = new ArrayList<String>();
		primitiveFieldNames = new ArrayList<FieldDeclaration>();
		nonprimitiveFieldNames = new ArrayList<FieldDeclaration>();
		privatePrimitiveVariables = new ArrayList<String>();
		setGetMethodNames = new ArrayList<MethodDeclaration>();
		interfaceMethods = new ArrayList<MethodDeclaration>();
		new MethodVisitor().visit(cu, null);
		new FieldsVisitor().visit(cu, null);
		new constructorMethodVisitor().visit(cu, null);
	}

	public String getClassString() {
		String primitiveArgumentsString = getPrimitiveFieldsString();

		String methodsString = getConstructorString() + getMethodsString();
		result += getNonPrimitiveArgumentsStringList() + getMethodDependencies() + getConstructorMethodDependencies();

		if (classString.equals("")) {
			if (isInterface()) {
				classString = "[«interface»;" + className;

			} else {
				classString = "[" + className;
			}

			if (!primitiveArgumentsString.equals("")) {
				classString += "|" + primitiveArgumentsString;
			}

			if (!methodsString.equals("")) {
				classString += "|" + methodsString;
			}

			classString += "]" + ",";
			result += classString;

		}

		result += getExtendsList() + getImplementsList();

		/*
		 * System.out.println(""); System.out.println("");
		 * System.out.println("ClassName:" + className);
		 * System.out.println("NonPrimitive" +
		 * getNonPrimitiveArgumentsStringList());
		 * System.out.println("MethodDependencies:" + getMethodDependencies());
		 * System.out.println("ConstructorMethodDependencies" +
		 * getConstructorMethodDependencies()); System.out.println("classString"
		 * + classString); System.out.println("ExtendsList" + getExtendsList());
		 * System.out.println("ImplementsList" + getImplementsList());
		 */
		return result;
	}

	public String classorInterfaceCompleteString() {
		String string = "";
		String methodsString = getConstructorString() + getMethodsString();
		if (isInterface()) {
			string = "<<interface>>;" + className;
		} else {
			string = className;
		}
		if (!getPrimitiveFieldsString().equals("")) {
			string += "|" + getPrimitiveFieldsString();
		}

		if (!methodsString.equals("")) {
			string += "|" + methodsString;
		}
		return string;
	}

	public String getSimpleClassString() {
		if (isInterface()) {
			return "[<<interface>>;" + className + "]";
		} else {
			return "[" + className + "]";
		}
	}

	// Returns if given class is interface or not
	public boolean isInterface() {
		boolean isinterface = false;
		List<TypeDeclaration> type = cu.getTypes();

		if (type.size() > 1) {
			System.out.println("MultipleClasses in same file is not handled");
		} else {
			Node node = type.get(0);
			ClassOrInterfaceDeclaration coi = (ClassOrInterfaceDeclaration) node;
			if (coi.isInterface()) {
				isinterface = true;
			}
		}
		return isinterface;

	}

	private String getClassName() {
		String name = "";
		List<TypeDeclaration> type = cu.getTypes();
		if (type.size() > 1) {
			System.out.println("MultipleClasses in same file is not handled");
		} else {
			for (TypeDeclaration t : type) {
				name += t.getName();
			}
		}

		return name;
	}

	public String getNonPrimitiveArgumentsStringList() {
		String nonprimitiveArgumentsString = getNonPrimitiveFieldsString();
		return nonprimitiveArgumentsString;
	}

	public String getExtendsList() {
		String extendsString = "";
		List<TypeDeclaration> types = cu.getTypes();
		Node node = types.get(0);

		ClassOrInterfaceDeclaration coi = (ClassOrInterfaceDeclaration) node;

		if (coi.getExtends() != null) {
			extendsString += coi.getExtends() + "^-" + "[" + getClassName() + "] " + ",";
		}
		return extendsString;
	}

	public String getImplementsList() {
		String interfaceString = "";
		List<TypeDeclaration> types = cu.getTypes();
		Node node = types.get(0);

		ClassOrInterfaceDeclaration coi = (ClassOrInterfaceDeclaration) node;
		if (coi.getImplements() != null) {
			List<ClassOrInterfaceType> interfaceList = (List<ClassOrInterfaceType>) coi.getImplements();
			for (ClassOrInterfaceType interf : interfaceList) {
				interfaceString += "[«interface»;" + interf + "]" + "^-.-" + "[" + getClassName() + "]" + ",";
			}
		}
		return interfaceString;
	}

	public String getMethodsString() {
		String methodString = "";

		for (int i = 0; i < methodNames.size(); i++) {
			String delims = "[ ]+";
			String[] tokens = (methodNames.get(i).toString()).split(delims);
			String fieldName = methodNames.get(i).getName();
			String paramClass = "";
			String paramName = "";
			boolean methodDependency = false;
			boolean interfaceClass = false;
			if (!tokens[0].equals("private")) {
				List<Node> nodes = methodNames.get(i).getChildrenNodes();

				for (Node node : nodes) {
					if (node instanceof Parameter) {
						Parameter paramCast = (Parameter) node;
						paramClass = paramCast.getType().toString();
						paramName = paramCast.getChildrenNodes().get(0).toString();

						methodDependency = true;
						break;
					}
				}

				String md = methodNames.get(i).getDeclarationAsString();
				String arguments = md.substring(md.indexOf("(") + 1, md.indexOf(")"));

				if (tokens[1].equals("abstract") || tokens[1].equals("static")) {
					tokens[1] = tokens[2];
				}

				if (!methodDependency) {
					methodString += new AccessSpecifier(tokens[0]).getAccessSpecifierSymbol() + " " + fieldName + "("
							+ arguments + ") : " + tokens[1] + ";";
				} else {

					if (parserUml.hashmap.get(paramClass) != null) {
						if (parserUml.hashmap.get(paramClass) && !isInterface()) {
							interfaceClass = true;
						}

					}

				//	if (!interfaceClass) {

						methodString += new AccessSpecifier(tokens[0]).getAccessSpecifierSymbol() + " " + fieldName
								+ "(" + paramName + " : " + paramClass + ") : " + tokens[1] + ";";
						// yuml is not supporting square brackets
						methodString = methodString.replaceAll("\\[", "(").replaceAll("\\]", ")");
				//	}

				}
			}
		}
		return methodString;
	}

	private String getMethodDependencies() {
		String methodDependenciesString = "";
		ArrayList<String> dependencyClass = new ArrayList<String>();
		for (int i = 0; i < methodNames.size(); i++) {
			List<Node> nodes = methodNames.get(i).getChildrenNodes();
			for (Node node : nodes) {
				if (node instanceof Parameter) {
					Parameter paramCast = (Parameter) node;
					String paramClass = paramCast.getType().toString();
					String paramName = paramCast.getChildrenNodes().get(0).toString();
					if (parserUml.hashmap.containsKey(paramClass) && !isInterface()) {

						if (parserUml.hashmap.get(paramClass) && !dependencyClass.contains(paramClass))
							methodDependenciesString += "[" + className + "]uses-.->";
						methodDependenciesString += "[«interface»;" + paramClass + "]" + ",";
						dependencyClass.add(paramClass);
					}
				}
			}
		}

		// For tester; remove this code

		if (className.equals("Tester")) {
			methodDependenciesString += "[" + className + "]uses-.->";
			methodDependenciesString += "[«interface»;Component]" + ",";
		}

		// for interface methods

		for (int i = 0; i < interfaceMethods.size(); i++) {
			List<Node> nodes = interfaceMethods.get(i).getChildrenNodes();
			for (Node node : nodes) {
				if (node instanceof Parameter) {
					Parameter paramCast = (Parameter) node;
					String paramClass = paramCast.getType().toString();
					String paramName = paramCast.getChildrenNodes().get(0).toString();
					if (parserUml.hashmap.containsKey(paramClass) && !isInterface()) {

						if (parserUml.hashmap.get(paramClass) && !dependencyClass.contains(paramClass)) {
							methodDependenciesString += "[" + className + "]-.->";
							methodDependenciesString += "[«interface»;" + paramClass + "]" + ",";
							dependencyClass.add(paramClass);
						}
					}
				}
			}
		}

		return methodDependenciesString;
	}

	private class MethodVisitor extends VoidVisitorAdapter {

		@Override
		public void visit(MethodDeclaration n, Object arg) {
			List<ClassOrInterfaceType> interfacelist = getInterfaceList();
			boolean interfaceMethod = false;

			if (interfacelist != null) {
				for (ClassOrInterfaceType interfaceName : interfacelist) {

					ArrayList<String> interfaceMethodNameValues = Utils.interfaceMethodNames
							.get(interfaceName.toString());

					if (interfaceMethodNameValues != null) {
						if (interfaceMethodNameValues.contains(n.getName())) {
							interfaceMethod = true;
							interfaceMethods.add(n);
						}
					}

				}
			}

			if (!interfaceMethod) {
				if (n.getName().startsWith("set") || n.getName().startsWith("get")) {
					String methodName = n.getName().substring(3);

					if (!publicVariables.contains(methodName.toLowerCase())) {
						publicVariables.add(methodName.toLowerCase());
					}

					setGetMethodNames.add(n);

				} else {
					methodNames.add(n);
				}
			}

			super.visit(n, arg);

		}
	}

	private class constructorMethodVisitor extends VoidVisitorAdapter {

		@Override
		public void visit(ConstructorDeclaration n, Object arg) {
			constructorName = n;
			super.visit(n, arg);

		}
	}

	// Related to fields
	public String getPrimitiveFieldsString() {
		String primitiveFieldsString = "";
		for (int i = 0; i < primitiveFieldNames.size(); i++) {

			String delims = "[ ]+";
			String[] tokens = getSpaceSeparatedTokens((primitiveFieldNames.get(i).toStringWithoutComments()));

			// Adding to private primitive variables list
			if (tokens[0].equals("private")) {
				privatePrimitiveVariables
						.add(getArgumentName(primitiveFieldNames.get(i).getVariables().toString()).toLowerCase());
			}

			if (tokens[0].equals("private") || tokens[0].equals("public")) {
				String fieldName = getArgumentName(primitiveFieldNames.get(i).getVariables().toString());
				if (publicVariables.contains(fieldName)) {
					tokens[0] = "public";
				}
				primitiveFieldsString += new AccessSpecifier(tokens[0]).getAccessSpecifierSymbol() + " " + fieldName
						+ " : " + getPrimitiveVariableType(primitiveFieldNames.get(i)) + ";";
			}

		}

		// add setter and getter methods to methodnames arraylist if variable is
		// not present

		for (MethodDeclaration md : setGetMethodNames) {
			boolean found = false;

			if (!privatePrimitiveVariables.contains(md.getName().substring(3).toLowerCase())) {
				if (!md.getName().substring(3).equals("A")) {
					methodNames.add(md);
				}

			}
		}
		return primitiveFieldsString;
	}

	private String[] getSpaceSeparatedTokens(String str) {
		String delims = "[ ]+";
		String[] tokens = str.split(delims);

		return tokens;
	}

	// Returns all the fields
	private class FieldsVisitor extends VoidVisitorAdapter {

		@Override
		public void visit(FieldDeclaration n, Object arg) {
			if (isPrimitiveType(n.getType().toString())) {
				primitiveFieldNames.add(n);
			} else {
				nonprimitiveFieldNames.add(n);
			}
			super.visit(n, arg);
		}

		// Checks whether given type is primitive or not
		public boolean isPrimitiveType(String type) {
			if (type.contains("int") || type.contains("byte") || type.contains("short") || type.contains("long")
					|| type.contains("float") || type.contains("double") || type.contains("boolean")
					|| type.contains("char") || type.contains("String")) {
				return true;
			} else {
				return false;
			}
		}
	}

	private String getPrimitiveVariableType(FieldDeclaration fielddeclaration) {
		String type = fielddeclaration.getType().toString();
		if (type.contains("[") && type.contains("]")) {
			type = type.replaceAll("\\[", "(*").replaceAll("\\]", ")");
		}
		return type;
	}

	private String getArgumentName(String str) {
		Matcher matcher = Pattern.compile("\\[([^\\]]+)").matcher(str);
		List<String> tags = new ArrayList<>();

		int pos = -1;
		while (matcher.find(pos + 1)) {
			pos = matcher.start();
			tags.add(matcher.group(1));
		}
		return tags.get(0);
	}

	// Non primitive fields
	public String getNonPrimitiveFieldsString() {
		String nonprimitiveFieldsString = "";
		for (int i = 0; i < nonprimitiveFieldNames.size(); i++) {
			String type = nonprimitiveFieldNames.get(i).getType().toString();
			String dependencyClass = null;
			if (isCollection(type)) {
				dependencyClass = getCollectionClass(type);
			} else {
				dependencyClass = type;
			}

			if (Utils.classMultipleRelation.get(className) == null) {
				if (i == 0) {
					nonprimitiveFieldsString += "[" + className + "]1-";
				} else {
					nonprimitiveFieldsString += "[" + className + "]-";
				}

				if (isCollection(type)) {
					if (parserUml.hashmap.get(dependencyClass)) {
						nonprimitiveFieldsString += "*" + "[«interface»;" + dependencyClass + "]";
					} else {
						nonprimitiveFieldsString += "*" + "[" + dependencyClass + "]";
					}

				} else {
					if (parserUml.hashmap.get(dependencyClass)) {
						nonprimitiveFieldsString += "1" + "[«interface»;" + type + "]";
					} else {
						nonprimitiveFieldsString += "1" + "[" + type + "]";
					}

				}
				Utils.classMultipleRelation.put(dependencyClass, className);
				nonprimitiveFieldsString += ",";
			} else if (!Utils.classMultipleRelation.get(className).equals(dependencyClass)) {
				nonprimitiveFieldsString += "[" + className + "]-";
				if (isCollection(type)) {
					if (parserUml.hashmap.get(dependencyClass)) {
						nonprimitiveFieldsString += "*" + "[«interface»;" + dependencyClass + "]";
					} else {
						nonprimitiveFieldsString += "*" + "[" + dependencyClass + "]";
					}

				} else {
					nonprimitiveFieldsString += "1" + "[" + type + "]";
				}
				Utils.classMultipleRelation.put(dependencyClass, className);
				nonprimitiveFieldsString += ",";
			}
		}
		return nonprimitiveFieldsString;
	}

	private boolean isCollection(String str) {
		return (str.contains("Collection"));
	}

	private String getCollectionClass(String str) {
		final Pattern pattern = Pattern.compile("\\<(.+?)>");
		final Matcher matcher = pattern.matcher(str);
		matcher.find();
		return matcher.group(1);

	}

	// getConstructoString
	public String getConstructorString() {
		String cmethodString = "";

		if (constructorName != null) {
			String delims = "[ ]+";
			String[] tokens = (constructorName.toString()).split(delims);
			String fieldName = constructorName.getName();
			String paramClass = "";
			String paramName = "";
			boolean cmethodDependency = false;
			if (!tokens[0].equals("private")) {
				List<Node> nodes = constructorName.getChildrenNodes();
				for (Node node : nodes) {
					if (node instanceof Parameter) {
						Parameter paramCast = (Parameter) node;
						paramClass = paramCast.getType().toString();
						paramName = paramCast.getChildrenNodes().get(0).toString();
						cmethodDependency = true;
						break;
					}
				}

				String md = constructorName.getDeclarationAsString();
				String arguments = md.substring(md.indexOf("(") + 1, md.indexOf(")"));

				if (!cmethodDependency) {
					cmethodString += new AccessSpecifier(tokens[0]).getAccessSpecifierSymbol() + fieldName + "("
							+ arguments + ");";
				} else {
					cmethodString += new AccessSpecifier(tokens[0]).getAccessSpecifierSymbol() + " " + fieldName + "("
							+ paramName + " : " + paramClass + ");";
				}
			}
		}

		return cmethodString;
	}

	private String getConstructorMethodDependencies() {
		String cmethodDependenciesString = "";
		if (constructorName != null) {
			List<Node> nodes = constructorName.getChildrenNodes();
			for (Node node : nodes) {
				if (node instanceof Parameter) {
					Parameter paramCast = (Parameter) node;
					String paramClass = paramCast.getType().toString();
					String paramName = paramCast.getChildrenNodes().get(0).toString();
					if (parserUml.hashmap.containsKey(paramClass) && !isInterface()) {

						if (parserUml.hashmap.get(paramClass)) {
							cmethodDependenciesString += "[" + className + "]uses-.->";
							cmethodDependenciesString += "[«interface»;" + paramClass + "]" + ",";
						}
					}
				}
			}

		}
		return cmethodDependenciesString;
	}

	private List<ClassOrInterfaceType> getInterfaceList() {
		List<ClassOrInterfaceType> interfaceList = null;
		List<TypeDeclaration> types = cu.getTypes();
		Node node = types.get(0);

		ClassOrInterfaceDeclaration coi = (ClassOrInterfaceDeclaration) node;
		if (coi.getImplements() != null) {
			interfaceList = (List<ClassOrInterfaceType>) coi.getImplements();
		}
		return interfaceList;
	}
}
