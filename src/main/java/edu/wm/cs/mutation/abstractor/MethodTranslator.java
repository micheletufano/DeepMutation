package edu.wm.cs.mutation.abstractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodTranslator {

	private static final String VAR_PREFIX = "VAR_";
	private static final String TYPE_PREFIX = "TYPE_";
	private static final String METHOD_PREFIX = "METHOD_";

	private static final String STRING_PREFIX = "STRING_";
	private static final String CHAR_PREFIX = "CHAR_";
	private static final String INT_PREFIX = "INT_";
	private static final String FLOAT_PREFIX = "FLOAT_";

	private static final String ERROR = "error";

	private static Map<String, LinkedHashMap<String,String>> translatedMutantsMap;

	public static void translateMethods(Map<String, LinkedHashMap<String, String>> mutantsMap,
                                        List<String> mappings, List<String> modelPaths) {

		System.out.println("Translating abstract mutants...");

		translatedMutantsMap = new HashMap<>();
		for (String modelPath : modelPaths) {
			File modelFile = new File(modelPath);
			String modelName = modelFile.getName();
			System.out.println("  Translating results from model " + modelName + "... ");

			LinkedHashMap<String, String> mutantMap = mutantsMap.get(modelName);
			LinkedHashMap<String, String> modelMap = new LinkedHashMap<>();

			int index = 0; // entry index of mapping and methods.abstract
			for (String signature : mutantMap.keySet()) {
				String srcCode = mutantMap.get(signature);

				String[] dicts = mappings.get(index++).split(";", -1); // 0: VAR,1: TYPE, 2: METHOD, 3: STR, 4: CHAR,
				// 5:INT, 6:FLOAT
				String transCode = translateCode(dicts, srcCode);
				if (!transCode.equals(ERROR) && checkCode(signature, transCode)) {
					modelMap.put(signature, transCode);
				}
			}

			translatedMutantsMap.put(modelName, modelMap);
			System.out.println("  done.");
		}
		System.out.println("done.");
	}

	public static String translateCode(String[] dicts, String code) {
		StringBuilder sb = new StringBuilder();
		String[] tokens = code.split(" ");
		// Un-wrap the dicts
		String[] dictVar = dicts[0].split(",");
		String[] dictType = dicts[1].split(",");
		String[] dictMethod = dicts[2].split(",");
		String[] dictString = dicts[3].split(",");
		String[] dictChar = dicts[4].split(",");
		String[] dictInt = dicts[5].split(",");
		String[] dictFloat = dicts[6].split(",");

		for (String token : tokens) {
			int index = -1;
			if (token.startsWith(VAR_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictVar[0].equals("") || index > dictVar.length)
					return ERROR;
				else
					sb.append(dictVar[index - 1]);
			} else if (token.startsWith(TYPE_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictType[0].equals("") || index > dictType.length)
					return ERROR;
				else
					sb.append(dictType[index - 1]);
			} else if (token.startsWith(METHOD_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictMethod[0].equals("") || index > dictMethod.length)
					return ERROR;
				else
					sb.append(dictMethod[index - 1]);
			} else if (token.startsWith(STRING_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictString[0].equals("") || index > dictString.length)
					return ERROR;
				else
					sb.append(dictString[index - 1]);
			} else if (token.startsWith(CHAR_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictChar[0].equals("") || index > dictChar.length)
					return ERROR;
				else
					sb.append(dictChar[index - 1]);
			} else if (token.startsWith(INT_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictInt[0].equals("") || index > dictInt.length)
					return ERROR;
				else
					sb.append(dictInt[index - 1]);
			} else if (token.startsWith(FLOAT_PREFIX)) {
				index = Integer.valueOf(token.substring(token.indexOf('_') + 1));
				if (dictFloat[0].equals("") || index > dictFloat.length)
					return ERROR;
				else
					sb.append(dictFloat[index - 1]);
			} else {
				sb.append(token);
			}
			sb.append(" ");
		}
		return sb.toString();
	}

	public static boolean checkCode(String signature, String srcCode) {
		// Parser
		MethodParser parser = new MethodParser();

		try {
			parser.parse(srcCode);
		} catch (Exception e) {
//			System.err.println("    Exception while parsing " + signature + "; ignored method.");
			return false;
		} catch (StackOverflowError e) {
//			System.err.println("    StackOverFlowError while parsing " + signature + "; ignored method.");
			return false;
		}
		// Tokenizer
		MethodLexer tokenizer = new MethodLexer();
		tokenizer.setTypes(parser.getTypes());
		tokenizer.setMethods(parser.getMethods());
		tokenizer.setIdioms(new HashSet<String>());

		String afterTokenized = tokenizer.tokenize(srcCode);
		if (afterTokenized.equals(MethodLexer.ERROR_LEXER)) {
			System.err.println("    Exception while lexing " + signature + "; ignored method.");
			return false;
		}

		return true;
	}

	public static Map<String, LinkedHashMap<String, String>> getTranslatedMutantsMap() {
		return translatedMutantsMap;
	}

	public static void setTranslatedMutantsMap(Map<String, LinkedHashMap<String, String>> translatedMutantsMap) {
		MethodTranslator.translatedMutantsMap = translatedMutantsMap;
	}
}