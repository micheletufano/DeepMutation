package edu.wm.cs.mutation.abstractor;

import java.util.*;
import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractor {
	private static Map<String, LinkedHashMap<String, String>> defects4jMap;
	private static Map<String, List<String>> defects4jMappings;

	private static LinkedHashMap<String, String> absMethodsMap;
	private static List<String> mappingList;

	private static Set<String> idioms;

	public static void abstractMethods(LinkedHashMap<String, String> rawMethods, String idiomPath) {

		System.out.println("Abstracting methods... ");

		// Set up Idioms
		idioms = IOHandler.readIdioms(idiomPath);
		if (idioms == null) {
			System.err.println("Could not load idioms");
			return;
		}

		absMethodsMap = new LinkedHashMap<>();

		mappingList = new ArrayList<>();
        for (String signature : rawMethods.keySet()) {
            String srcCode = rawMethods.get(signature);

            String absCode = abstractCode(signature, srcCode, mappingList);
			if (absCode == null) {
				continue;
			}
            absMethodsMap.put(signature, absCode); // replace srcCode with absCode
        }

		System.out.println("done.");
	}

	public static void abstractMethodsFromDefects4J(Map<String, LinkedHashMap<String, String>> rawMethods, String idiomPath) {

		System.out.println("Abstracting methods... ");

		// Set up Idioms
		idioms = IOHandler.readIdioms(idiomPath);
		if (idioms == null) {
			System.err.println("Could not load idioms");
			return;
		}

		defects4jMap = new HashMap<>();
		defects4jMappings = new HashMap<>();
		for (String revPath : rawMethods.keySet()) {
			System.out.println("  Processing " + revPath + "... ");
			LinkedHashMap<String, String> revMethodMap = new LinkedHashMap<>(rawMethods.get(revPath));
			List<String> mappingList = new ArrayList<>();
			for (String signature : revMethodMap.keySet()) {
				String srcCode = revMethodMap.get(signature);

				String absCode = abstractCode(signature, srcCode, mappingList);
				if (absCode == null) {
					continue;
				}
				revMethodMap.put(signature, absCode); // replace srcCode with absCode
			}
			defects4jMap.put(revPath, revMethodMap);
			defects4jMappings.put(revPath, mappingList);

			System.out.println("done.");
		}
		System.out.println("done.");
	}

	private static String abstractCode(String signature, String srcCode, List<String> mappingList) {
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
		mappingList.add(mappings);
		// System.out.println("Signiture: "+signatrue);
		// System.out.println("AfterTokenized: "+afterTokenized);

		return afterTokenized;
	}

	public static Map<String, LinkedHashMap<String, String>> getAbstractedDefects4JMethods() {
		return defects4jMap;
	}

	public static Map<String, List<String>> getDefects4jMappings() {
		return defects4jMappings;
	}

	public static LinkedHashMap<String, String> getAbstractedMethods() {
		return absMethodsMap;
	}

	public static List<String> getMappings() {
		return mappingList;
	}
}
