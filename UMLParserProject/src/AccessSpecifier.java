public class AccessSpecifier {
	String accessSpecifier;

	AccessSpecifier(String accessSpecifier) {
		this.accessSpecifier = accessSpecifier;
	}

	public String getAccessSpecifierSymbol() {
		if (accessSpecifier.equals("public")) {
			return "+";
		} else if (accessSpecifier.equals("private")) {
			return "-";
		} else {
			return "";
		}
	}
}
