package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.io.IOHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MethodMutator {

    private static String VOCAB_SOURCE = "vocab.after.txt";
    private static String VOCAB_TARGET = "vocab.before.txt";
    private static String TRAIN_OPTIONS = "train_options.json";

    private static Map<String, Map<String, LinkedHashMap<String,String>>> defects4jMap;
    private static Map<String, LinkedHashMap<String,String>> mutantsMap;

    public static void mutateMethods(String outPath, LinkedHashMap<String, String> absMethodsMap, List<String> modelDirs) {
        System.out.println("Mutating methods... ");

        // Write abstracted methods
        IOHandler.writeMethods(outPath, absMethodsMap, true);

        // Check for train_options.json and vocab files
        if (!foundFileDeps(modelDirs)) {
            System.err.println("ERROR: cannot find file dependencies");
            return;
        }

        // Run all models on all revisions
        mutantsMap = new HashMap<>();
        for (String modelDir : modelDirs) {
            File modelFile = new File(modelDir);
            String modelName = modelFile.getName();
            System.out.print("  Running model " + modelName + "... ");

            // Create new map to store mutants
            LinkedHashMap<String,String> modelMap = new LinkedHashMap<>(absMethodsMap);

            // Get absolute paths to input and output files
            File outFile = new File(outPath);
            String input = outFile.getAbsolutePath() + "/" + IOHandler.METHODS + IOHandler.ABS_SUFFIX;

            // Check that input file exists
            if (!new File(input).isFile()) {
                System.err.println("    ERROR: cannot find '" + input + "'");
                continue;
            }

            // Run a model on a single revision
            List<String> mutants = runModel(modelFile, input);
            if (mutants == null) {
                System.err.println("    ERROR: could not run model " + modelDir + " on " + input);
                continue;
            }

            int i=0;
            for (String s : modelMap.keySet()) {
                modelMap.put(s, mutants.get(i++));
            }

            mutantsMap.put(modelName, modelMap);

//            // Write mutants
//            try {
//                Files.write(Paths.get(output), mutants);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            System.out.println("done.");
        }
        System.out.println("done.");
    }

    /**
     * Mutates methods given abstracted methods and model directories.
     *
     * For file dependencies, refer to {@link MethodMutator#foundFileDeps(List)}.
     * @param absMethodsMap
     * @param modelDirs
     */
    public static void mutateMethodsFromDefects4J(Map<String, LinkedHashMap<String, String>> absMethodsMap, List<String> modelDirs) {
        System.out.println("Mutating methods... ");

        // Write maps to file
        IOHandler.writeMethodsFromDefects4J(absMethodsMap, true);

        // Check for train_options.json and vocab files
        if (!foundFileDeps(modelDirs)) {
            System.err.println("ERROR: cannot find file dependencies");
            return;
        }

        // Run all models on all revisions
        defects4jMap = new HashMap<>();
        for (String revPath : absMethodsMap.keySet()) {
            System.out.println("  Mutating " + revPath + "... ");

            Map<String, LinkedHashMap<String,String>> map = new HashMap<>();
            for (String modelDir : modelDirs) {
                File modelFile = new File(modelDir);
                String modelName = modelFile.getName();
                System.out.print("    Running model " + modelName + "... ");

                // Create new map to store mutants
                LinkedHashMap<String,String> modelMap = new LinkedHashMap<>(absMethodsMap.get(revPath));

                // Get absolute paths to input and output files
                File revFile = new File(revPath);
                String input = revFile.getAbsolutePath() + "/" + IOHandler.METHODS + IOHandler.ABS_SUFFIX;

                // Check that input file exists
                if (!new File(input).isFile()) {
                    System.err.println("    ERROR: cannot find '" + input + "'");
                    continue;
                }

                // Run a model on a single revision
                List<String> mutants = runModel(modelFile, input);
                if (mutants == null) {
                    System.err.println("    ERROR: could not run model " + modelDir + " on " + input);
                    continue;
                }

                int i=0;
                for (String s : modelMap.keySet()) {
                    modelMap.put(s, mutants.get(i++));
                }
                map.put(modelName, modelMap);

                System.out.println("done.");
            }
            defects4jMap.put(revPath, map);

            System.out.println("done.");
        }
    }

    /**
     * Returns true if all file dependencies were found for in all model directories.
     *
     * File dependencies:
     * - {@link MethodMutator#VOCAB_SOURCE}
     * - {@link MethodMutator#VOCAB_TARGET}
     * - {@link MethodMutator#TRAIN_OPTIONS}
     *
     * @param modelDirs
     * @return
     */
    private static boolean foundFileDeps(List<String> modelDirs) {
        List<String> fileDeps = new ArrayList<>();
        for (String dir : modelDirs) {
            fileDeps.add(dir + VOCAB_SOURCE);
            fileDeps.add(dir + VOCAB_TARGET);
            fileDeps.add(dir + TRAIN_OPTIONS);
        }

        for (String file : fileDeps) {
            if (!new File(file).isFile()) {
                System.err.println("  Cannot find '" + file + "'");
                return false;
            }
        }

        return true;
    }

    private static List<String> runModel(File modelFile, String input) {
        try {
            List<String> cmd = buildCommand(input);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(modelFile);
            Process p = pb.start();

            // Write output
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> mutants = new ArrayList<>();
            String line;

            while ((line = br.readLine()) != null) {
                mutants.add(line);
            }
            p.waitFor();

            return mutants;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> buildCommand(String input) {
        List<String> cmd = new ArrayList<>();
        cmd.add("python"); cmd.add("-m"); cmd.add("bin.infer");
        cmd.add("--tasks");
        cmd.add("- class: DecodeText");
        cmd.add("--model_dir"); cmd.add(".");
        cmd.add("--input_pipeline");
        cmd.add("class: ParallelTextInputPipeline\nparams:\n  source_files:\n  - " + input);
        return cmd;
    }

    public static Map<String, LinkedHashMap<String, String>> getMutantsMap() {
        return mutantsMap;
    }

    public static Map<String, Map<String, LinkedHashMap<String, String>>> getDefects4jMap() {
        return defects4jMap;
    }
}