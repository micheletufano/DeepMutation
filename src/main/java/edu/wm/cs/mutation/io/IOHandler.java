package edu.wm.cs.mutation.io;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

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

    public static final String MUTANT_DIR = "mutants/";
    public static final String LOG_DIR = "logs/";

    public static void writeMethods(String outPath, LinkedHashMap<String, String> map, boolean abstracted) {
        List<String> signatures = new ArrayList<>(map.keySet());
        List<String> bodies = new ArrayList<>(map.values());

        try {
            Files.createDirectories(Paths.get(outPath));
            Files.write(Paths.get(outPath + METHODS + KEY_SUFFIX), signatures);
            if (abstracted) {
                Files.write(Paths.get(outPath + METHODS + ABS_SUFFIX), bodies);
            } else {
                Files.write(Paths.get(outPath + METHODS + SRC_SUFFIX), bodies);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static LinkedHashMap<String,String> readMethods(String outPath, boolean abstracted) {
        if (abstracted) {
            System.out.println("Reading abstracted methods from file... ");
        } else {
            System.out.println("Reading methods from file... ");
        }

        LinkedHashMap<String,String> map = new LinkedHashMap<>();

        List<String> signatures = null;
        List<String> bodies = null;
        try {
            signatures = Files.readAllLines(Paths.get(outPath + METHODS + KEY_SUFFIX));
            if (abstracted) {
                bodies = Files.readAllLines(Paths.get(outPath + METHODS + ABS_SUFFIX));
            } else {
                bodies = Files.readAllLines(Paths.get(outPath + METHODS + SRC_SUFFIX));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (signatures == null || bodies == null) {
            System.err.println("ERROR: could not load map from files");
            return null;
        }

        for (int i=0; i<signatures.size(); i++) {
            map.put(signatures.get(i), bodies.get(i));
        }

        System.out.println("done.");
        return map;
    }

    public static void writeMutants(String outPath, Map<String, LinkedHashMap<String, String>> modelsMap,
                                    List<String> modelPaths, boolean abstracted) {
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);

            List<String> signatures = new ArrayList<>(mutantsMap.keySet());
            List<String> bodies = new ArrayList<>(mutantsMap.values());

            try {
                String modelOutPath = outPath + modelName + "/";
                Files.createDirectories(Paths.get(modelOutPath));
                Files.write(Paths.get(modelOutPath + MUTANTS + KEY_SUFFIX), signatures);
                if (abstracted) {
                    Files.write(Paths.get(modelOutPath + MUTANTS + ABS_SUFFIX), bodies);
                } else {
                    Files.write(Paths.get(modelOutPath + MUTANTS + SRC_SUFFIX), bodies);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, LinkedHashMap<String,String>> readMutants(String outPath, List<String> modelPaths,
                                                                        boolean abstracted) {
        if (abstracted) {
            System.out.println("Reading abstracted mutants from files... ");
        } else {
            System.out.println("Reading mutants from files... ");
        }

        Map<String, LinkedHashMap<String,String>> modelsMap = new HashMap<>();

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            String modelOutPath = outPath + modelName + "/";

            List<String> signatures = null;
            List<String> bodies = null;

            try {
                signatures = Files.readAllLines(Paths.get(modelOutPath + MUTANTS + KEY_SUFFIX));
                if (abstracted) {
                    bodies = Files.readAllLines(Paths.get(modelOutPath + MUTANTS + ABS_SUFFIX));
                } else {
                    bodies = Files.readAllLines(Paths.get(modelOutPath + MUTANTS + SRC_SUFFIX));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (signatures == null || bodies == null) {
                System.err.println("ERROR: could not load map from files");
                return null;
            }

            LinkedHashMap<String,String> mutantsMap = new LinkedHashMap<>();
            for (int i=0; i<signatures.size(); i++) {
                mutantsMap.put(signatures.get(i), bodies.get(i));
            }

            modelsMap.put(modelName, mutantsMap);
        }
        System.out.println("done.");
        return modelsMap;
    }

    public static void createMutantFiles(String outPath, String srcPath, Map<String, LinkedHashMap<String, String>> modelsMap,
                                         List<CtMethod> methods, List<String> modelPaths) {
        System.out.println("Creating mutant files... ");

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, String> mutantsMap = modelsMap.get(modelName);

            // create directory
            String mutantPath = outPath + modelName + "/" + MUTANT_DIR;
            if (!Files.exists(Paths.get(mutantPath))) {
                try {
                    Files.createDirectories(Paths.get(mutantPath));
                } catch (IOException e) {
                    System.out.println("    Error in creating mutant directory: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }

            // create format for padded mutantIDs
            int num_digits = Integer.toString(mutantsMap.keySet().size()).length();
            StringBuilder format = new StringBuilder();
            format.append("%0").append(num_digits).append("d");

            // replace original methods with mutants
            int counter = 1; // counter for mutated file
            for (CtMethod method : methods) {
                String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
                if (!mutantsMap.containsKey(signature)) {
                    continue;
                }

                SourcePosition sp = method.getPosition();
                String original = sp.getCompilationUnit().getOriginalSourceCode();

                // XXXX_relative-path-to-file.java
                String fileName = String.format(format.toString(), counter) + "_" +
                        sp.getCompilationUnit().getFile().getPath()
                                .replaceFirst(System.getProperty("user.dir") + "/", "")
                                .replace("/","-");

                // construct and format mutated class
                StringBuilder sb = new StringBuilder();
                sb.append(original.substring(0, sp.getSourceStart()));
                sb.append(mutantsMap.get(signature));
                sb.append(original.substring(sp.getSourceEnd() + 1));

                try {
                    String formattedSrc = new Formatter().formatSource(sb.toString());
                    Files.write(Paths.get(mutantPath + fileName), formattedSrc.getBytes());
                } catch (FormatterException e) {
                    System.err.println("    Error in formatting mutant " + counter + ": " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("    Error in writing mutant " + counter + ": " + e.getMessage());
                }
                counter++;
            }
        }
        System.out.println("done.");
    }

    public static void writeMappings(String outPath, List<String> mappings) {
        try {
            Files.write(Paths.get(outPath + METHODS + MAP_SUFFIX), mappings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readMappings(String outPath) {
        System.out.println("Reading mappings from file... ");
        List<String> mappings = null;

        try {
            mappings = Files.readAllLines(Paths.get(outPath + METHODS + MAP_SUFFIX));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mappings == null) {
            System.err.println("ERROR: could not load mappings from file");
        }
        System.out.println("done.");
        return mappings;
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
