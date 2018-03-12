package edu.wm.cs.mutation.abstractor;

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

	private static final String PRED_INPUT = "methods.pred";
	private static final String PRED_OUT = "pred/";

	private static final String VAR_PREFIX = "VAR_";
	private static final String TYPE_PREFIX = "TYPE_";
	private static final String METHOD_PREFIX = "METHOD_";

	private static final String STRING_PREFIX = "STRING_";
	private static final String CHAR_PREFIX = "CHAR_";
	private static final String INT_PREFIX = "INT_";
	private static final String FLOAT_PREFIX = "FLOAT_";

	private static final String ERROR = "error";

	public static Map<String, LinkedHashMap<String, String>> getRewPredMethods(
			Map<String, LinkedHashMap<String, String>> absMethods) {
		System.out.println("Reading predicted methods...");
		Map<String, LinkedHashMap<String, String>> predMethods = new HashMap<String, LinkedHashMap<String, String>>();
		for (String outPath : absMethods.keySet()) {
			String predPath = outPath + PRED_INPUT;
			List<String> methods = null;
			List<String> signatures = null;
			try {
				signatures = new ArrayList<String>(absMethods.get(outPath).keySet());
				methods = Files.readAllLines(Paths.get(predPath));
			} catch (IOException e) {
				e.printStackTrace();
			}
			LinkedHashMap<String, String> methodMap = new LinkedHashMap<>();
			int index = 0;
			for (String sign : signatures)
				methodMap.put(sign, methods.get(index++));
			predMethods.put(outPath, methodMap);
		}
		System.out.println("done");
		return predMethods;

	}

	public static void translateFromDefects4J(Map<String, LinkedHashMap<String, String>> predMethods,
											  Map<String, List<String>> mappings) {

		System.out.println("Translating predicted methods...");
		for (String outPath : predMethods.keySet()) {
			String predOutPath = outPath + PRED_OUT;

			LinkedHashMap<String, String> predMethodMap = new LinkedHashMap<>(predMethods.get(outPath));
			List<String> signatures = new ArrayList<>();
			List<String> transMethods = new ArrayList<>();

			System.out.println("  Processing " + outPath);

			List<String> mapping = mappings.get(outPath); // the mapping list for each method
			int index = 0; // entry index of mapping and methods.abstract
			for (String signature : predMethodMap.keySet()) {
				String srcCode = predMethodMap.get(signature);

				String[] dicts = mapping.get(index++).split(";", -1); // 0: VAR,1: TYPE, 2: METHOD, 3: STR, 4: CHAR,
																		// 5:INT, 6:FLOAT
				String transCode = translateCode(dicts, srcCode);
				if (!transCode.equals(ERROR) && checkCode(transCode)) {
					signatures.add(signature);
					transMethods.add(transCode);
					System.out.println("Mappings: " + mapping.get(index - 1));
					System.out.println("Before translation: " + srcCode);
					System.out.println("After translation: " + transCode);
				}

			}
			System.out.println("    Writing files... ");
			try {
				if (!Files.exists(Paths.get(predOutPath)))
					Files.createDirectories(Paths.get(predOutPath));
				Files.write(Paths.get(predOutPath + IOHandler.METHODS + IOHandler.KEY_SUFFIX), signatures);
				Files.write(Paths.get(predOutPath + IOHandler.METHODS + IOHandler.SRC_SUFFIX), transMethods);
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	public static boolean checkCode(String srcCode) {
		// Parser
		MethodParser parser = new MethodParser();

		try {
			parser.parse(srcCode);
		} catch (Exception e) {
			System.err.println("Exception during parsing:");
			System.err.println(srcCode);
			return false;
		} catch (StackOverflowError e) {
			System.err.println("StackOverflow during parsing:");
			System.err.println(srcCode);
			return false;
		}
		// Tokenizer
		MethodLexer tokenizer = new MethodLexer();
		tokenizer.setTypes(parser.getTypes());
		tokenizer.setMethods(parser.getMethods());
		tokenizer.setIdioms(new HashSet<String>());

		String afterTokenized = tokenizer.tokenize(srcCode);
		if (afterTokenized.equals(MethodLexer.ERROR_LEXER)) {
			System.err.println("Exception during lexing:");
			System.err.println(srcCode);
			return false;
		}

		return true;
	}
}