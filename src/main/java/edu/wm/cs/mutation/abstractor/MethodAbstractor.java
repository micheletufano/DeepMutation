package edu.wm.cs.mutation.abstractor;

import java.util.*;
import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractor {
	private static Map<String, LinkedHashMap<String, String>> absMethodsMap;
	private static Map<String, List<String>> mappings;
	private static Set<String> idioms;

	public static void abstractMethods(Map<String, LinkedHashMap<String, String>> rawMethods, String idiomPath) {

		System.out.println("Abstracting methods... ");

		// Set up Idioms
		idioms = IOHandler.readIdioms(idiomPath);
		if (idioms == null) {
			System.err.println("Could not load idioms");
			return;
		}

		absMethodsMap = new HashMap<>();
		mappings = new HashMap<>();
		for (String revPath : rawMethods.keySet()) {
			LinkedHashMap<String, String> revMethodMap = new LinkedHashMap<>(rawMethods.get(revPath));
			LinkedHashMap<String, String> subAbsMap = new LinkedHashMap<>();
			List<String> mappingList = new ArrayList<>();
			for (String signature : revMethodMap.keySet()) {
				String srcCode = revMethodMap.get(signature);

				String absCode = abstractCode(srcCode, mappingList);

				// filter out methods that are longer than 100 words
				if (absCode.split(" ").length > 100)
					continue;
				subAbsMap.put(signature, absCode); // signature and abstract code of filtered method
//				System.out.println("Signiture: " + signature);
//				System.out.println("AfterTokenized: " + absCode);
			}
			absMethodsMap.put(revPath, subAbsMap);
			mappings.put(revPath, mappingList);

			System.out.println("done.");
		}
		System.out.println("done.");
	}

	private static String abstractCode(String srcCode, List<String> mappingList) {
		// Parser
		MethodParser parser = new MethodParser();

		try {
			parser.parse(srcCode);
		} catch (Exception e) {
			System.err.println("Exception during parsing!");
		} catch (StackOverflowError e) {
			System.err.println("StackOverflow during parsing!");
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
		if (afterTokenized.split(" ").length <= 100)
			mappingList.add(mappings);

		return afterTokenized;
	}

	public static Map<String, LinkedHashMap<String, String>> getAbstractedMethods() {
		return absMethodsMap;
	}

	public static Map<String, List<String>> getMappings() {
		return mappings;
	}
}
