
public class UMLParserProject {

	public static void main(String[] args) {
		parserUml parser = new parserUml(args[0], args[1]);
		parser.generateUMLDiagram();
	}
}
