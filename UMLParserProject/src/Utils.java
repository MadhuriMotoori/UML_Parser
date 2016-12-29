
import java.util.ArrayList;
import java.util.HashMap;

public class Utils {
	static HashMap<String, String> classMultipleRelation = new HashMap<String, String>();
	static HashMap<String, Boolean> classIncluded = new HashMap<String, Boolean>();
	static HashMap<String, ArrayList<String>> interfaceMethodNames = new HashMap<String, ArrayList<String>>();
	static ArrayList<String> interfacesNames = new ArrayList<String>();

	public String[] splitString(String str, String delims) {
		// String delims = "[ ]+";
		String[] tokens = str.split(delims);

		return tokens;
	}
}
