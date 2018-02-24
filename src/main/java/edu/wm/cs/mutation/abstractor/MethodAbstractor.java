package edu.wm.cs.mutation.abstractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

			String outPath = revPath; // output path for each revision
			List<String> signatures = new ArrayList<>();
			List<String> absMethods = new ArrayList<>();
			List<String> mappingList = new ArrayList<>();

			System.out.print("  Processing " + outPath + "... ");
			for (String signature : revMethodMap.keySet()) {
				String srcCode = revMethodMap.get(signature);

				String absCode = abstractCode(srcCode, mappingList);
				signatures.add(signature);
				absMethods.add(absCode);
				revMethodMap.put(signature, absCode); // replace srcCode with absCode
			}
			absMethodsMap.put(outPath, revMethodMap);
			mappings.put(outPath, mappingList);

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
		mappingList.add(mappings);
		// System.out.println("Signiture: "+signatrue);
		// System.out.println("AfterTokenized: "+afterTokenized);

		return afterTokenized;
	}

	public static Map<String, LinkedHashMap<String, String>> getAbstractedMethods() {
		return absMethodsMap;
	}

	public static Map<String, List<String>> getMappings() {
		return mappings;
	}

}
