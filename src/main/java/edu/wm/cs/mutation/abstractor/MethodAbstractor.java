package edu.wm.cs.mutation.abstractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.wm.cs.mutation.Consts;
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
		idioms = readIdioms(idiomPath);
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

	public static void writeMethods(String outPath) {
        System.out.println("Writing abstracted methods... ");

		if (absMethodsMap == null) {
			System.err.println("  ERROR: cannot write null map");
			return;
		}

		List<String> signatures = new ArrayList<>(absMethodsMap.keySet());
		List<String> bodies = new ArrayList<>(absMethodsMap.values());

		try {
			Files.createDirectories(Paths.get(outPath));
			Files.write(Paths.get(outPath + File.separator + Consts.METHODS + Consts.KEY_SUFFIX), signatures);
            Files.write(Paths.get(outPath + File.separator + Consts.METHODS + Consts.ABS_SUFFIX), bodies);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}

	public static void writeMappings(String outPath) {
		System.out.println("Writing mappings... ");

		if (dictMap == null) {
			System.err.println("ERROR: cannot write null input mappings");
			return;
		}
		List<String> mappings = new ArrayList<>(dictMap.values());
		try {
			Files.write(Paths.get(outPath + File.separator + Consts.METHODS + Consts.MAP_SUFFIX), mappings);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("done.");
	}

	public static void readMethods(String outPath) {
        System.out.println("Reading abstracted methods from file... ");

		absMethodsMap.clear();

		List<String> signatures = null;
		List<String> bodies = null;
		try {
			signatures = Files.readAllLines(Paths.get(outPath + File.separator + Consts.METHODS + Consts.KEY_SUFFIX));
            bodies = Files.readAllLines(Paths.get(outPath + File.separator + Consts.METHODS + Consts.ABS_SUFFIX));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (signatures == null || bodies == null) {
			System.err.println("  ERROR: could not load map from files");
			return;
		}

		if (signatures.size() != bodies.size()) {
			System.err.println("  ERROR: unequal number of keys and values");
			return;
		}

		for (int i = 0; i < signatures.size(); i++) {
			absMethodsMap.put(signatures.get(i), bodies.get(i));
		}

		System.out.println("  Read " + absMethodsMap.size() + " methods.");
		System.out.println("done.");
		return;
	}

	public static void readMappings(String outPath) {
		System.out.println("Reading mappings from file... ");
		dictMap = new LinkedHashMap<>();

		List<String> signatures = null;
		List<String> mappings = null;
		try {
			signatures = Files.readAllLines(Paths.get(outPath + File.separator + Consts.METHODS + Consts.KEY_SUFFIX));
			mappings = Files.readAllLines(Paths.get(outPath + File.separator + Consts.METHODS + Consts.MAP_SUFFIX));

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (signatures == null || mappings == null) {
			System.err.println("  ERROR: could not load map from files");
			return;
		}

		if (signatures.size() != mappings.size()) {
			System.err.println("  ERROR: unequal number of keys and values");
			return;
		}

		for (int i = 0; i < signatures.size(); i++) {
			dictMap.put(signatures.get(i), mappings.get(i));
		}

		System.out.println("  Read " + dictMap.size() + " mappings.");
		System.out.println("done.");
		return;
	}

	private static Set<String> readIdioms(String filePath) {
		try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
			return stream.collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
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
