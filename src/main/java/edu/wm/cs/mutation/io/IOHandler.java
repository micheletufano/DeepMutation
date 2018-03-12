package edu.wm.cs.mutation.io;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOHandler {

    public static final String METHODS = "methods";
    public static final String MUTANTS = "mutants";

    public static final String KEY_SUFFIX = ".key";
    public static final String SRC_SUFFIX = ".src";
    public static final String MAP_SUFFIX = ".map";
    public static final String ABS_SUFFIX = ".abs";
    public static final String PATH_SUFFIX = ".path";

    public static final String MUTANT_DIR = "mutants/";

    public static void writeMethods(String outDir, LinkedHashMap<String, String> map, boolean abstracted) {
        List<String> signatures = new ArrayList<>(map.keySet());
        List<String> bodies = new ArrayList<>(map.values());

        try {
            Files.createDirectories(Paths.get(outDir));
            Files.write(Paths.get(outDir + METHODS + KEY_SUFFIX), signatures);
            if (abstracted) {
                Files.write(Paths.get(outDir + METHODS + ABS_SUFFIX), bodies);
            } else {
                Files.write(Paths.get(outDir + METHODS + SRC_SUFFIX), bodies);
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

    public static void createMutantFiles(String outDir, String srcPath, Map<String, LinkedHashMap<String, String>> modelsMap,
                                         List<CtMethod> methods, List<String> modelDirs) {
        System.out.println("Creating mutant files... ");

        for (String modelDir : modelDirs) {
            File modelFile = new File(modelDir);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);

            // create directory
            String mutantDir = outDir + modelName + "/" + MUTANT_DIR;
            if (!Files.exists(Paths.get(mutantDir))) {
                try {
                    Files.createDirectories(Paths.get(mutantDir));
                } catch (IOException e) {
                    System.out.println("    Error in creating mutant directory: " + e.getMessage());
                    continue;
                }
            }

            // create format for padded mutantIDs
            int num_digits = Integer.toString(mutantsMap.keySet().size()).length();
            StringBuilder format = new StringBuilder();
            format.append("%0").append(num_digits).append("d");

            // replace original methods with mutants
            int counter = 0; // counter for mutated file
            for (CtMethod method : methods) {
                String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
                if (!mutantsMap.containsKey(signature)) {
                    continue;
                }

                SourcePosition sp = method.getPosition();
                String original = sp.getCompilationUnit().getOriginalSourceCode();

                // XXXX_path-to-src-path-to-file.java
                String fileName = String.format(format.toString(), counter) + "_" +
                        srcPath.replace("/","-") + signature.split("#")[0].replace(".","-") + ".java";

                // construct and format mutated class
                StringBuilder sb = new StringBuilder();
                sb.append(original.substring(0, sp.getSourceStart()));
                sb.append(mutantsMap.get(signature));
                sb.append(original.substring(sp.getSourceEnd() + 1));

                try {
                    String formattedSrc = new Formatter().formatSource(sb.toString());
                    Files.write(Paths.get(mutantDir + fileName), formattedSrc.getBytes());
                } catch (FormatterException e) {
                    System.err.println("    Error in formating mutant " + counter + ": " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("    Error in writing mutant " + counter + ": " + e.getMessage());
                }
                counter++;
            }
        }
        System.out.println("done.");
    }

    public static void writeMappings(String outDir, List<String> mappings) {
        try {
            Files.write(Paths.get(outDir + METHODS + MAP_SUFFIX), mappings);
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

    public static void writeMethodsFromDefects4J(Map<String, LinkedHashMap<String, String>> map, boolean abstracted) {
        for (String outDir : map.keySet()) {
            LinkedHashMap<String, String> submap = map.get(outDir);

            List<String> signatures = new ArrayList<>(submap.keySet());
            List<String> bodies = new ArrayList<>(submap.values());

            try {
                Files.createDirectories(Paths.get(outDir));
                Files.write(Paths.get(outDir + METHODS + KEY_SUFFIX), signatures);
                if (abstracted) {
                    Files.write(Paths.get(outDir + METHODS + ABS_SUFFIX), bodies);
                } else {
                    Files.write(Paths.get(outDir + METHODS + SRC_SUFFIX), bodies);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMappingsFromDefects4J(Map<String, List<String>> map) {
        for (String outDir : map.keySet()) {
            try {
                Files.write(Paths.get(outDir + METHODS + MAP_SUFFIX), map.get(outDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
