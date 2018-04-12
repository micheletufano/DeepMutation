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

import edu.wm.cs.mutation.Consts;
import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;

public class MethodTranslator {

	private static final String VAR_PREFIX = "VAR_";
	private static final String TYPE_PREFIX = "TYPE_";
	private static final String METHOD_PREFIX = "METHOD_";

	private static final String STRING_PREFIX = "STRING_";
	private static final String CHAR_PREFIX = "CHAR_";
	private static final String INT_PREFIX = "INT_";
	private static final String FLOAT_PREFIX = "FLOAT_";

	private static final String ERROR = "error";

	private static Map<String, LinkedHashMap<String, List<String>>> translatedMutantMaps = new HashMap<>();;

	public static void translateMethods(Map<String, LinkedHashMap<String, List<String>>> mutantsMap,
			LinkedHashMap<String, String> dictMap, List<String> modelPaths) {

		System.out.println("Translating abstract mutants...");
        
		translatedMutantMaps.clear();
		
		if (mutantsMap == null || mutantsMap.size() == 0) {
			System.err.println("  ERROR: null/empty input map");
			return;
		}
		
		if (dictMap.size() == 0) {
			System.err.println("  ERROR: null dictionary map");
			return;
		}

		for (String modelPath : modelPaths) {
			File modelFile = new File(modelPath);
			String modelName = modelFile.getName();
			System.out.println("  Translating results from model " + modelName + "... ");

			LinkedHashMap<String, List<String>> mutantMap = mutantsMap.get(modelName);
			LinkedHashMap<String, List<String>> modelMap = new LinkedHashMap<>();

			if (mutantMap == null || mutantMap.size() == 0) {
				System.err.println("    WARNING: cannot translate null/empty map for model " + modelName);
				continue;
			}

			int untranslatable=0;
			int numMutants=0;
			for (String signature : mutantMap.keySet()) {
				
				List<String> predictions = mutantMap.get(signature);
                String[] dicts = dictMap.get(signature).split(";", -1); // 0: VAR,1: TYPE, 2: METHOD, 3: STR, 4: CHAR,
				// 5:INT, 6:FLOAT
				for (String srcCode : predictions) {
					String transCode = translateCode(dicts, srcCode);
					if (!transCode.equals(ERROR) && checkCode(signature, transCode)) {
						if (!modelMap.containsKey(signature))
							modelMap.put(signature, new ArrayList<String>());
						modelMap.get(signature).add(transCode);
						numMutants++;
//						System.out.println("Abs Code: " + srcCode);
//						System.out.println("Trans Code: " + transCode);
//						System.out.println("Mapping: " + dictMap.get(signature));
					} else {
						untranslatable++;
					}
				}
			}
			System.out.println("    Removed " + untranslatable + " untranslatable mutants.");
			System.out.println("    There are " + modelMap.size() + " methods and " + numMutants + " mutants remaining.");

			translatedMutantMaps.put(modelName, modelMap);
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
			// System.err.println(" Exception while parsing " + signature + "; ignored
			// method.");
			return false;
		} catch (StackOverflowError e) {
			// System.err.println(" StackOverFlowError while parsing " + signature + ";
			// ignored method.");
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

	public static void writeMutants(String outPath, List<String> modelPaths) {
        System.out.println("Writing translated mutants... ");

		if (translatedMutantMaps == null || translatedMutantMaps.size() == 0) {
			System.err.println("ERROR: cannot write null/empty map");
			return;
		}

		for (String modelPath : modelPaths) {
			File modelFile = new File(modelPath);
			String modelName = modelFile.getName();
			System.out.println("  Processing model " + modelName + "... ");

			LinkedHashMap<String, List<String>> mutantsMap = translatedMutantMaps.get(modelName);
			if (mutantsMap == null) {
				System.err.println("    WARNING: cannot write null map for model " + modelName);
				continue;
			}

			List<String> signatures = new ArrayList<>(mutantsMap.keySet());
			List<String> bodies = new ArrayList<>();
			// join multiple predictions for each method
			for (String signature : signatures) {
				StringBuilder sb = new StringBuilder();
				List<String> predictions = new ArrayList<>(mutantsMap.get(signature));

				for (String pred : predictions) {
					sb.append(pred).append("<SEP>");
				}
				sb.setLength(sb.length() - 5);
				bodies.add(sb.toString());
			}

			try {
				String modelOutPath = outPath + File.separator + modelName + File.separator;
				Files.createDirectories(Paths.get(modelOutPath));
				Files.write(Paths.get(modelOutPath + Consts.MUTANTS + Consts.KEY_SUFFIX), signatures);
                Files.write(Paths.get(modelOutPath + Consts.MUTANTS + Consts.SRC_SUFFIX), bodies);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("  done.");
		}
		System.out.println("done.");
	}

	public static void readMutants(String outPath, List<String> modelPaths) {
        System.out.println("Reading translated mutants from files... ");

		translatedMutantMaps.clear();

		for (String modelPath : modelPaths) {
			File modelFile = new File(modelPath);
			String modelName = modelFile.getName();
			System.out.println("  Processing model " + modelName + "... ");

			String modelOutPath = outPath + File.separator + modelName + File.separator;
			List<String> signatures = null;
			List<String> bodies = null;

			try {
                signatures = Files.readAllLines(Paths.get(modelOutPath + File.separator + Consts.MUTANTS + Consts.KEY_SUFFIX));
                bodies = Files.readAllLines(Paths.get(modelOutPath + Consts.MUTANTS + Consts.SRC_SUFFIX));
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (signatures == null || bodies == null) {
				System.err.println("  ERROR: could not load map from files");
				return;
			}

			if (signatures.size() == 0 || bodies.size() == 0) {
				System.err.println("  ERROR: unequal number of keys and values");
				return;
			}

			int index = 0;
			LinkedHashMap<String, List<String>> mutantMap = new LinkedHashMap<>();
			for (String sign : signatures) {
				mutantMap.put(sign, new ArrayList<>());
				String[] predictions = bodies.get(index++).split("<SEP>");
				for (String pred : predictions) {
					mutantMap.get(sign).add(pred);
				}
			}
			translatedMutantMaps.put(modelName, mutantMap);
			System.out.println("  done.");
		}
		System.out.println("done.");
		return;
	}

	public static Map<String, LinkedHashMap<String, List<String>>> getTranslatedMutantMaps() {
		return translatedMutantMaps;
	}

	public static void setTranslatedMutantMaps(Map<String, LinkedHashMap<String, List<String>>> translatedMutantMaps) {
		MethodTranslator.translatedMutantMaps = translatedMutantMaps;
	}
}