package edu.wm.cs.mutation.abstractor;

import java.util.*;
import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractor {
	private static LinkedHashMap<String, String> absMethodsMap = new LinkedHashMap<>();
	private static LinkedHashMap<String, String> dictMap =  new LinkedHashMap<>();
	private static int tokenThr = 50; // default maximum number of tokens in a method
	private static boolean specified = false;

	private static Set<String> idioms;

	public static void abstractMethods(LinkedHashMap<String, String> rawMethods, String idiomPath) {

		System.out.println("Abstracting methods... ");
		
		absMethodsMap.clear();
		
		if (rawMethods == null || rawMethods.size() == 0) {
			System.err.println("  ERROR: null/empty input map");
			return;
		}

		// Set up Idioms
		idioms = IOHandler.readIdioms(idiomPath);
		if (idioms == null) {
			System.err.println("  Could not load idioms");
			return;
		}

		int unparseable = 0;
		int tooLong = 0;
		dictMap.clear();

        for (String signature : rawMethods.keySet()) {
            String srcCode = rawMethods.get(signature);

            String absCode = abstractCode(signature, srcCode, dictMap);
			if (absCode == null) {
				unparseable++;
				continue;
			}
			if (!specified && absCode.split(" ").length > tokenThr) {
				tooLong++;
				continue;
			}
            absMethodsMap.put(signature, absCode); // replace srcCode with absCode
        }
		System.out.println("  There were " + unparseable + " unparseable methods.");
		System.out.println("  There were " + tooLong + " methods longer than " + tokenThr + " tokens.");
		System.out.println("  There are " + absMethodsMap.size() + " remaining methods.");

		System.out.println("done.");
	}

	private static String abstractCode(String signature, String srcCode, Map<String, String> dictMap) {
		// Parser
		MethodParser parser = new MethodParser();

		try {
			parser.parse(srcCode);
		} catch (Exception e) {
			System.err.println("  Exception while parsing " + signature + "; ignored method.");
			return null;
		} catch (StackOverflowError e) {
			System.err.println("  StackOverflowError while parsing " + signature + "; ignored method.");
			return null;
		}

		// Tokenizer
		MethodLexer tokenizer = new MethodLexer();

		// System.out.println("Types: "+parser.getTypes());
		// System.out.println("Methods: "+parser.getMethods());

		tokenizer.setTypes(parser.getTypes());
		tokenizer.setMethods(parser.getMethods());
		tokenizer.setIdioms(idioms);

		String afterTokenized = tokenizer.tokenize(srcCode);
		String mappings = tokenizer.getMapping();
		if (specified || afterTokenized.split(" ").length <= tokenThr)
		    dictMap.put(signature, mappings);

		return afterTokenized;
	}

	public static LinkedHashMap<String, String> getAbstractedMethods() {
		return absMethodsMap;
	}

	public static LinkedHashMap<String, String> getMappings() {
		return dictMap;
	}

	public static void setAbstractedMethods(LinkedHashMap<String, String> absMethodsMap) {
		MethodAbstractor.absMethodsMap = absMethodsMap;
	}

	public static void setMappings(LinkedHashMap<String, String> dictMap) {
		MethodAbstractor.dictMap = dictMap;
	}
	
	public static void setInputMode(boolean specified) {
		MethodAbstractor.specified = specified;
	}
	
	public static void setTokenThreshold(int tokenThr) {
		MethodAbstractor.tokenThr = tokenThr;
	}
}
