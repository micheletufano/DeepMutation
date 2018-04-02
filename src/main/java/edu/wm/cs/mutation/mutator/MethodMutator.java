package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.io.IOHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class MethodMutator {

    private static final String VOCAB_SOURCE = "vocab.after.txt";
    private static final String VOCAB_TARGET = "vocab.before.txt";
    private static final String TRAIN_OPTIONS = "train_options.json";

    private static String python = "python";
    private static boolean dumpingBeams = false;
    private static Integer numBeams = 2;
    private static String interpretBeams = "interpretBeams.py";

    private static Map<String, LinkedHashMap<String,List<String>>> mutantsMap;

    /**
     * Mutates methods given abstracted methods and model directories.
     *
     * For file dependencies, refer to {@link MethodMutator#foundFileDeps(List)}.
     * @param absMethodsMap
     * @param modelPaths
     */
    public static void mutateMethods(String outPath, LinkedHashMap<String, String> absMethodsMap, List<String> modelPaths) {
        System.out.println("Mutating methods... ");

        if (absMethodsMap == null) {
            System.err.println("  ERROR: null input map");
            return;
        }

        if (dumpingBeams) {
            System.out.println("    Dumping beams is set.");
            if (numBeams < 2) {
                System.err.println("  ERROR: to dump beams, the number of beams must be >= 2");
                return;
            } else if (!new File(interpretBeams).exists()) {
                System.err.println("  ERROR: cannot find " + interpretBeams);
                return;
            }
        }

        // Write abstracted methods
        IOHandler.writeMethods(outPath, absMethodsMap, true);

        // Check for train_options.json and vocab files
        if (!foundFileDeps(modelPaths)) {
            System.err.println("ERROR: cannot find file dependencies");
            return;
        }

        // Run all models on all revisions
        mutantsMap = new HashMap<>();
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Running model " + modelName + "... ");

            long start = System.nanoTime();

            // Get absolute paths to input file
            File outFile = new File(outPath);
            String input = outFile.getAbsolutePath() + "/" + IOHandler.METHODS + IOHandler.ABS_SUFFIX;

            // Check that input file exists
            if (!new File(input).isFile()) {
                System.err.println("    ERROR: cannot find '" + input + "'");
                continue;
            }

            // Generate mutants
            List<List<String>> mutants = runModel(modelFile, input);
            if (mutants == null) {
                continue;
            }

            // Save mutants
            LinkedHashMap<String,List<String>> modelMap = new LinkedHashMap<>();
            int i=0;
            for (String s : absMethodsMap.keySet()) {
                if (dumpingBeams) {
                    // just put them all in -- may not have changed some
                    modelMap.put(s, mutants.get(i++));
                } else {
                    // put it in only if it was mutated
                    String mutant = mutants.get(i).get(0);
                    if (!mutant.trim().equals(absMethodsMap.get(s).trim())) {
                        modelMap.put(s, mutants.get(i++));
                    }
                }
            }
            mutantsMap.put(modelName, modelMap);

            System.out.println("    Took " + (System.nanoTime() - start) / 1000000000.0 + " seconds.");
            System.out.println("  done.");
        }
        System.out.println("done.");
    }

    /**
     * Returns true if all file dependencies were found for in all model directories.
     *
     * File dependencies:
     * - {@link MethodMutator#VOCAB_SOURCE}
     * - {@link MethodMutator#VOCAB_TARGET}
     * - {@link MethodMutator#TRAIN_OPTIONS}
     *
     * @param modelPaths
     * @return
     */
    private static boolean foundFileDeps(List<String> modelPaths) {
        List<String> fileDeps = new ArrayList<>();
        for (String modelPath : modelPaths) {
            fileDeps.add(modelPath + VOCAB_SOURCE);
            fileDeps.add(modelPath + VOCAB_TARGET);
            fileDeps.add(modelPath + TRAIN_OPTIONS);
        }

        for (String file : fileDeps) {
            if (!new File(file).isFile()) {
                System.err.println("  Cannot find '" + file + "'");
                return false;
            }
        }

        return true;
    }

    private static List<List<String>> runModel(File modelFile, String input) {
        try {
            List<String> cmd = buildCommand(input);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(modelFile);
            Process p = pb.start();

            // Write output
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<List<String>> mutants = new ArrayList<>();
            String line;

            if (dumpingBeams) {
                p.waitFor();
                interpretBeams(modelFile, mutants);
            } else {
                int i=0;
                while ((line = br.readLine()) != null) {
                    List<String> mutant = new ArrayList<>(1);
                    mutant.add(line);
                    mutants.add(mutant);
                }
                p.waitFor();
            }

            if (mutants.size() == 0) {
                System.err.println("    ERROR: could not run model " + modelFile.getPath() + " on " + input + " using command:");
                System.err.println(String.join(" ", cmd));

                br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = br.readLine()) != null) {
                    System.err.println(line);
                }

                return null;
            }
            return mutants;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> buildCommand(String input) {
        List<String> cmd = new ArrayList<>();
        cmd.add(python); cmd.add("-m"); cmd.add("bin.infer");
        cmd.add("--tasks");
        if (dumpingBeams) {
            cmd.add("- class: DecodeText\n- class: DumpBeams\n  params:\n    file: beams.npz");
            cmd.add("--model_params"); cmd.add("inference.beam_search.beam_width: " + numBeams);
        } else {
            cmd.add("- class: DecodeText");
        }
        cmd.add("--model_dir"); cmd.add(".");
        cmd.add("--input_pipeline");
        cmd.add("class: ParallelTextInputPipeline\nparams:\n  source_files:\n  - " + input);
        return cmd;
    }

    private static void interpretBeams(File modelFile, List<List<String>> mutants) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(python); cmd.add(new File(interpretBeams).getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(modelFile);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String mutant;
            int i=0;
            int beam=0;

            List<String> mutatedMethod = new ArrayList<>();
            while ((mutant = br.readLine()) != null) {
                mutatedMethod.add(mutant);
                beam++;
                if (beam >= numBeams) {
                    mutants.add(mutatedMethod);
                    mutatedMethod = new ArrayList<>();
                    beam = 0;
                }
            }
            p.waitFor();

            // clean up beams.npz
            File beams = new File(modelFile.getAbsolutePath() + "/beams.npz");
            if (!beams.delete()) {
                System.err.println("    WARNING: could not clean up " + beams.getPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, LinkedHashMap<String, List<String>>> getMutantsMap() {
        return mutantsMap;
    }

    public static void setMutantsMap(Map<String, LinkedHashMap<String, List<String>>> mutantsMap) {
        MethodMutator.mutantsMap = mutantsMap;
    }

    public static void setPython(String python) {
        MethodMutator.python = python;
    }

    public static void setDumpingBeams(boolean dumpBeams) {
        MethodMutator.dumpingBeams = dumpBeams;
    }

    public static boolean isDumpingBeams() {
        return MethodMutator.dumpingBeams;
    }

    public static void setNumBeams(Integer numBeams) {
        MethodMutator.numBeams = numBeams;
    }
}
