package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DeepMutation {

    private static String projPath;
    private static String srcPath;
    private static String outPath;
    private static String libPath;
    private static Integer complianceLvl;
    private static Boolean compiled;
    private static String inputMethodsFile;
    private static String wrapperLibFile;

    private static String idiomsFile;
    private static List<String> modelPaths;

    private static Boolean extractor = false;
    private static Boolean abstractor = false;
    private static Boolean mutator = false;
    private static Boolean translator = false;
    private static Boolean tester = false;

    public static void main(String[] args) {
        parseArgs(args);

        if (extractor) {
            MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsFile);
            MethodExtractor.writeMethods(outPath);
        } else { 
            System.out.println("MethodExtractor disabled. Stopping.");
            return; 
        }

        if (abstractor) {
            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomsFile);
            MethodAbstractor.writeMethods(outPath);
            MethodAbstractor.writeMappings(outPath);
        } else { 
            System.out.println("MethodAbstractor disabled. Stopping.");
            return;
        }

        if (mutator) {
            MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
            MethodMutator.writeMutants(outPath, modelPaths);
        } else { 
            System.out.println("MethodMutator disabled. Stopping.");
            return; 
        }

        if (translator) {
            MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
            MethodTranslator.writeMutants(outPath, modelPaths);
            MethodTranslator.createMutantFiles(outPath, modelPaths, MethodExtractor.getMethods());
        } else { 
            System.out.println("MethodTranslator disabled. Stopping.");
            return; 
        }

        if (tester) {
            MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                                     MethodExtractor.getMethods(), modelPaths, wrapperLibFile);
            if (MutantTester.usingBaseline()) {
                MutantTester.writeBaseline(outPath);
            }
            MutantTester.writeLogs(outPath, modelPaths);
            MutantTester.writeTimeouts(outPath, modelPaths);
            MutantTester.writeResults(outPath, modelPaths);
        } else { 
            System.out.println("MutantTester disabled. Stopping.");
            return; 
        }
    }

    private static void parseArgs(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java -jar DeepMutation.jar config_file");
            System.exit(1);
        }

        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (String line : lines) {
            if (line.matches("^ *#.*")) {
                continue;
            }
            if (line.contains("=")) {
                String key = line.split("=")[0];
                String val = line.split("=")[1];

                switch (key) {
                    case "extractor.enable":
                        extractor = Boolean.parseBoolean(val);
                        break;
                    case "abstractor.enable":
                        abstractor = Boolean.parseBoolean(val);
                        break;
                    case "mutator.enable":
                        mutator = Boolean.parseBoolean(val);
                        break;
                    case "translator.enable":
                        translator = Boolean.parseBoolean(val);
                        break;
                    case "tester.enable":
                        tester = Boolean.parseBoolean(val);
                        break;
                    case "project.path":
                        projPath = val;
                        break;
                    case "source.path":
                        srcPath = val;
                        break;
                    case "output.path":
                        outPath = val;
                        break;
                    case "library.path":
                        libPath = val;
                        break;
                    case "idioms.file":
                        idiomsFile = val;
                        break;
                    case "model.paths":
                        modelPaths = Arrays.asList(val.split(","));
                        break;
                    case "input.methods.file":
                        inputMethodsFile = val;
                        MethodAbstractor.setInputMode(true);
                        break;
                    case "wrapper.library.file":
                        wrapperLibFile = val;
                        break;
                    case "compliance.level":
                        complianceLvl = Integer.parseInt(val);
                        break;
                    case "compiled":
                        compiled = Boolean.parseBoolean(val);
                        break;
                    case "token.threshold":
                        MethodAbstractor.setTokenThreshold(Integer.parseInt(val));
                        break;
                    case "python":
                        MethodMutator.setPython(val);
                        break;
                    case "verbose":
                        MethodMutator.verbose(Boolean.parseBoolean(val));
                        break;
                    case "use.beams":
                        MethodMutator.useBeams(Boolean.parseBoolean(val));
                        break;
                    case "num.beams":
                        MethodMutator.setNumBeams(Integer.parseInt(val));
                        break;
                    case "max.threads":
                        MutantTester.setMaxThreads(Integer.parseInt(val));
                        break;
                    case "compile.command":
                        MutantTester.setCompileCmd(val.split(" "));
                        break;
                    case "test.command":
                        MutantTester.setTestCmd(val.split(" "));
                        break;
                    case "timeout":
                        MutantTester.setTimeout(Integer.parseInt(val));
                        break;
                    case "use.baseline":
                        MutantTester.useBaseline(Boolean.parseBoolean(val));
                        break;
                    case "compile.fail.strings":
                        MutantTester.setCompileFailStrings(val.split(","));
                        break;
                    case "test.fail.strings":
                        MutantTester.setTestFailStrings(val.split(","));
                        break;
                    case "clean.up":
                        MutantTester.setCleanUp(Boolean.parseBoolean(val));
                        break;
                }
            }
        }
    }

}
