package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.Consts;
import edu.wm.cs.mutation.io.IOHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MethodMutator {

    private static final String VOCAB_SOURCE = "vocab.after.txt";
    private static final String VOCAB_TARGET = "vocab.before.txt";
    private static final String TRAIN_OPTIONS = "train_options.json";

    private static String python = "python3";
    private static boolean usingBeams = false;
    private static Integer numBeams = 2;
    private static String interpretBeams = "interpretBeams.py";
    private static boolean verbose = false;

    private static Map<String, LinkedHashMap<String, List<String>>> mutantMaps = new HashMap<>();

    /**
     * Mutate methods given abstracted methods and model directories.
     * <p>
     * For file dependencies, refer to {@link MethodMutator#foundFileDeps(List)}.
     *
     * @param absMethodsMap Map of method signatures and their abstracted bodies
     * @param modelPaths    List of paths to each model directory
     */
    public static void mutateMethods(String outPath, LinkedHashMap<String, String> absMethodsMap, List<String> modelPaths) {
        System.out.println("Mutating methods... ");

        mutantMaps.clear();

        if (absMethodsMap == null || absMethodsMap.size() == 0) {
            System.err.println("  ERROR: null/empty input map");
            return;
        }

        if (usingBeams) {
            System.out.println("  Using " + numBeams + " beams.");
            if (numBeams < 2) {
                System.err.println("  ERROR: the number of beams must be >= 2");
                return;
            } else if (!new File(interpretBeams).exists()) {
                System.err.println("  ERROR: cannot find " + interpretBeams);
                return;
            }
        }

        // Check for train_options.json and vocab files in model directories
        if (!foundFileDeps(modelPaths)) {
            System.err.println("ERROR: cannot find file dependencies");
            return;
        }

        // Run all models
        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Running model " + modelName + "... ");

            long start = System.nanoTime();

            // Get absolute paths to input file
            File outFile = new File(outPath);
            String input = outFile.getAbsolutePath() + File.separator + Consts.METHODS + Consts.ABS_SUFFIX;

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
            LinkedHashMap<String, List<String>> modelMap = new LinkedHashMap<>();
            int i = 0;
            int numUnchangedMethods = 0;
            int numUnchangedMutants = 0;
            int numMutants = 0;
            for (String signature : absMethodsMap.keySet()) {
                List<String> mutatedMethod = mutants.get(i++);

                // get list of unchanged mutants
                List<String> unchanged = new ArrayList<>();
                for (String mutant : mutatedMethod) {
                    if (mutant.trim().equals(absMethodsMap.get(signature).trim())) {
                        unchanged.add(mutant);
                        numUnchangedMutants++;
                    } else {
                        numMutants++;
                    }
                }

                // remove them
                for (String mutant : unchanged) {
                    mutatedMethod.remove(mutant);
                }

                // if there are still non-trivial mutants, add them
                if (mutatedMethod.size() > 0) {
                    modelMap.put(signature, mutatedMethod);
                } else {
                    numUnchangedMethods++;
                }
            }
            MethodMutator.mutantMaps.put(modelName, modelMap);
            System.out.println("    Removed " + numUnchangedMutants + " unchanged mutants.");
            System.out.println("    Resulted in " + numUnchangedMethods + " removed methods.");
            System.out.println("    There are " + modelMap.size() + " methods and " + numMutants + " mutants remaining.");

            System.out.println("    Took " + (System.nanoTime() - start) / 1000000000.0 + " seconds.");
            System.out.println("  done.");
        }
        System.out.println("done.");
    }

    /**
     * Returns true if all file dependencies were found for in all model directories.
     * <p>
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

    /**
     * Run a model given a file with abstracted methods bodies.
     *
     * @param modelFile Model directory as a File
     * @param input     Path to file with abstracted method bodies
     * @return
     */
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
            int i = 0;

            if (usingBeams) {
                while (br.readLine() != null) {
                    if (i++ % 100 == 99) {
                        System.out.println("    Generated " + i + " mutated methods.");
                    }
                }
                System.out.println("    Generated " + i + " mutated methods.");
                p.waitFor();

                // Interpret model-generated beams.npz
                interpretBeams(modelFile, mutants);
            } else {
                while ((line = br.readLine()) != null) {
                    if (i++ % 100 == 99) {
                        System.out.println("    Generated " + i + " mutated methods.");
                    }
                    List<String> mutant = new ArrayList<>(1);
                    mutant.add(line);
                    mutants.add(mutant);
                }
                System.out.println("    Generated " + i + " mutated methods.");
                p.waitFor();
            }

            if (verbose) {
                System.err.println("    Using command:\n" + String.join(" ", cmd));

                br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = br.readLine()) != null) {
                    System.err.println(line);
                }
            }

            if (mutants.size() == 0) {
                System.err.println("    WARNING: could not generate mutated methods. " +
                        "Set MethodMutator.verbose(true) via config file for more details.");
                return null;
            }

            return mutants;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Return the command to run a model with correct formatting.
     *
     * @param input File with abstracted method bodies
     * @return
     */
    private static List<String> buildCommand(String input) {
        List<String> cmd = new ArrayList<>();
        cmd.add(python);
        cmd.add("-m");
        cmd.add("bin.infer");
        cmd.add("--tasks");
        if (usingBeams) {
            cmd.add("- class: DecodeText\n- class: DumpBeams\n  params:\n    file: beams.npz");
            cmd.add("--model_params");
            cmd.add("inference.beam_search.beam_width: " + numBeams);
        } else {
            cmd.add("- class: DecodeText");
        }
        cmd.add("--model_dir");
        cmd.add(".");
        cmd.add("--input_pipeline");
        cmd.add("class: ParallelTextInputPipeline\nparams:\n  source_files:\n  - " + input);
        return cmd;
    }

    /**
     * Interpret beams generated by a model using interpretBeams.py.
     *
     * @param modelFile Model directory as a File
     * @param mutants   Store results
     */
    private static void interpretBeams(File modelFile, List<List<String>> mutants) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(python);
            cmd.add(new File(interpretBeams).getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(modelFile);
            Process p = pb.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String mutant;
            int i = 0;
            int beam = 0;

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

    /**
     * Write mutant signatures to outPath/modelName/mutants.key and
     * respective bodies to outPath/modelName/mutants.abs.
     *
     * @param outPath    Path to output directory
     * @param modelPaths Paths to each model directory
     */
    public static void writeMutants(String outPath, List<String> modelPaths) {
        System.out.println("Writing abstracted mutants... ");

        if (mutantMaps == null || mutantMaps.size() == 0) {
            System.err.println("  ERROR: cannot write null/empty map");
            return;
        }

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            LinkedHashMap<String, List<String>> mutantsMap = mutantMaps.get(modelName);
            if (mutantsMap == null) {
                System.err.println("    WARNING: cannot write null map for model " + modelName);
                continue;
            }

            List<String> signatures = new ArrayList<>(mutantsMap.keySet());
            List<String> bodies = new ArrayList<>();
            // join multiple predictions for each method
            for (String signature : signatures) {
                StringBuilder sb = new StringBuilder();
                List<String> predictions = new ArrayList<>(mutantsMap.get(signature));

                for (String pred : predictions) {
                    sb.append(pred).append("<SEP>");
                }
                sb.setLength(sb.length() - 5);
                bodies.add(sb.toString());
            }

            try {
                String modelOutPath = outPath + File.separator + modelName + File.separator;
                Files.createDirectories(Paths.get(modelOutPath));
                Files.write(Paths.get(modelOutPath + Consts.MUTANTS + Consts.KEY_SUFFIX), signatures);
                Files.write(Paths.get(modelOutPath + Consts.MUTANTS + Consts.ABS_SUFFIX), bodies);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("  done.");
        }
        System.out.println("done.");
    }

    /**
     * Populate {@link MethodMutator#mutantMaps} from outPath/modelName/mutants.{key,abs}.
     *
     * @param outPath    Path to output directory
     * @param modelPaths Paths to each model directory
     */
    public static void readMutants(String outPath, List<String> modelPaths) {
        System.out.println("Reading abstracted mutants from files... ");

        mutantMaps.clear();

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            String modelOutPath = outPath + File.separator + modelName + File.separator;
            List<String> signatures = null;
            List<String> bodies = null;

            try {
                signatures = Files.readAllLines(Paths.get(outPath + File.separator + Consts.METHODS + Consts.KEY_SUFFIX));
                bodies = Files.readAllLines(Paths.get(modelOutPath + Consts.MUTANTS + Consts.ABS_SUFFIX));
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

            int index = 0;
            LinkedHashMap<String, List<String>> mutantMap = new LinkedHashMap<>();
            for (String sign : signatures) {
                mutantMap.put(sign, new ArrayList<>());
                String[] predictions = bodies.get(index++).split("<SEP>");
                for (String pred : predictions) {
                    mutantMap.get(sign).add(pred);
                }
            }
            mutantMaps.put(modelName, mutantMap);

            System.out.println("    Read " + mutantMap.size() + " mutants.");
            System.out.println("  done.");
        }
        System.out.println("done.");
        return;
    }

    public static Map<String, LinkedHashMap<String, List<String>>> getMutantMaps() {
        return mutantMaps;
    }

    public static void setMutantsMap(Map<String, LinkedHashMap<String, List<String>>> mutantMaps) {
        MethodMutator.mutantMaps = mutantMaps;
    }

    public static void setPython(String python) {
        MethodMutator.python = python;
    }

    public static void useBeams(boolean useBeams) {
        MethodMutator.usingBeams = useBeams;
    }

    public static boolean isUsingBeams() {
        return MethodMutator.usingBeams;
    }

    public static void setNumBeams(Integer numBeams) {
        MethodMutator.numBeams = numBeams;
    }

    public static void verbose(boolean verbose) {
        MethodMutator.verbose = verbose;
    }
}
