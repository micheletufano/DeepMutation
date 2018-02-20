package edu.wm.cs.mutation.abstractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;

public class MethodAbstractor {
	private static final String KEY_OUTPUT = "methods.key1";
	private static final String SRC_OUTPUT = "methods.abstract";
	private static Map<String, LinkedHashMap<String, String>> absMethodsMap;
	private static Set<String> idioms;

	public static void abstractMethods(Map<String, LinkedHashMap<String, String>> rawMethods, String idiomPath) {

		System.out.println("Abstracting methods... ");

		// Set up Idioms
		idioms = new HashSet<>();
		try (Stream<String> stream = Files.lines(Paths.get(idiomPath))) {
			idioms = stream.collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}

		absMethodsMap = new HashMap<>();
		for (String revPath : rawMethods.keySet()) {
			LinkedHashMap<String, String> revMethodMap = new LinkedHashMap<>(rawMethods.get(revPath));

			String outPath = revPath; // output path for each revision
			List<String> signatures = new ArrayList<>();
			List<String> absMethods = new ArrayList<>();

			System.out.println("  Processing " + outPath);
			for (String signature : revMethodMap.keySet()) {
				String srcCode = revMethodMap.get(signature);
				String absCode = abstractCode(srcCode);
				signatures.add(signature);
				absMethods.add(absCode);
				revMethodMap.put(signature, absCode); // replace srcCode with absCode
			}
			absMethodsMap.put(outPath, revMethodMap);

			System.out.println("    Writing files... ");
			try {
				Files.write(Paths.get(outPath + KEY_OUTPUT), signatures);
				Files.write(Paths.get(outPath + SRC_OUTPUT), absMethods);
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("  done.");
		}
		System.out.println("done.");
	}

	private static String abstractCode(String srcCode) {
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
		// System.out.println("Signiture: "+signatrue);
		// System.out.println("AfterTokenized: "+afterTokenized);

		// Add to result list for each revision
		return afterTokenized;
	}

	public static Map<String, LinkedHashMap<String, String>> getAbstractedMethods() {
		return absMethodsMap;
	}

}
