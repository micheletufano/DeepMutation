package edu.wm.cs.mutation.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOHandler {

    private static final String METHODS = "methods";
    private static final String MUTANTS = "mutants";

    private static final String KEY_SUFFIX = ".key";
    private static final String SRC_SUFFIX = ".src";
    private static final String MAP_SUFFIX = ".map";

    private static final String KEY_OUTPUT = "methods.key";
    private static final String SRC_OUTPUT = "methods.src";
    private static final String MAP_OUTPUT = "methods.map";

    public static final String ABS_OUTPUT = "methods.abs";
    public static final String ABS_SUFFIX = ".abs";

    public static void writeMethods(String outDir, LinkedHashMap<String, String> map, boolean abstracted) {
        List<String> signatures = new ArrayList<>(map.keySet());
        List<String> bodies = new ArrayList<>(map.values());

        try {
            Files.createDirectories(Paths.get(outDir));
            Files.write(Paths.get(outDir + KEY_OUTPUT), signatures);
            if (abstracted) {
                Files.write(Paths.get(outDir + ABS_OUTPUT), bodies);
            } else {
                Files.write(Paths.get(outDir + SRC_OUTPUT), bodies);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeMutants(String outDir, Map<String, LinkedHashMap<String, String>> modelsMap,
                                    List<String> modelDirs, boolean abstracted) {
        for (String modelDir : modelDirs) {
            File modelFile = new File(modelDir);
            String modelName = modelFile.getName();
            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);

            List<String> signatures = new ArrayList<>(mutantsMap.keySet());
            List<String> bodies = new ArrayList<>(mutantsMap.values());

            try {
                String modelOutDir = outDir + modelName + "/";
                Files.createDirectories(Paths.get(modelOutDir));
                Files.write(Paths.get(modelOutDir + MUTANTS + KEY_SUFFIX), signatures);
                if (abstracted) {
                    Files.write(Paths.get(modelOutDir + MUTANTS + ABS_SUFFIX), bodies);
                } else {
                    Files.write(Paths.get(modelOutDir + MUTANTS + SRC_SUFFIX), bodies);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMethodsFromDefects4J(Map<String, LinkedHashMap<String, String>> map, boolean abstracted) {
        for (String outDir : map.keySet()) {
            LinkedHashMap<String, String> submap = map.get(outDir);

            List<String> signatures = new ArrayList<>(submap.keySet());
            List<String> bodies = new ArrayList<>(submap.values());

            try {
                Files.createDirectories(Paths.get(outDir));
                Files.write(Paths.get(outDir + KEY_OUTPUT), signatures);
                if (abstracted) {
                    Files.write(Paths.get(outDir + ABS_OUTPUT), bodies);
                } else {
                    Files.write(Paths.get(outDir + SRC_OUTPUT), bodies);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMappingsFromDefects4J(Map<String, List<String>> map) {
        for (String outDir : map.keySet()) {
            try {
                Files.write(Paths.get(outDir + MAP_OUTPUT), map.get(outDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMappings(String outDir, List<String> mappings) {
        try {
            Files.write(Paths.get(outDir + MAP_OUTPUT), mappings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> readIdioms(String filePath) {
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            return stream.collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
