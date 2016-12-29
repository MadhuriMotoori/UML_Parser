
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class InterfaceMethods {
	ArrayList<String> methodNames;
	CompilationUnit cu;

	InterfaceMethods(CompilationUnit cu) {
		this.cu = cu;
		methodNames = new ArrayList<String>();
	}

	private class MethodVisitor extends VoidVisitorAdapter {

		@Override
		public void visit(MethodDeclaration n, Object arg) {

			// System.out.println(n.getName());
			methodNames.add(n.getName());
			super.visit(n, arg);

		}
	}

	public void buildInterfaceMethodMaps(String name) {

		new MethodVisitor().visit(cu, null);

		Utils.interfaceMethodNames.put(name, methodNames);

	}
}
