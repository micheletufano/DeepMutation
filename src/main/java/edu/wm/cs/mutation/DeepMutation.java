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

    private static String idiomsFile;
    private static List<String> modelPaths;

    public static void main(String[] args) {
        parseArgs(args);

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsFile);
        MethodExtractor.writeMethods(outPath);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomsFile);
        MethodAbstractor.writeMethods(outPath);
        MethodAbstractor.writeMappings(outPath);

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        MethodMutator.writeMutants(outPath, modelPaths);

        MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
        MethodTranslator.writeMutants(outPath, modelPaths);

        MethodTranslator.createMutantFiles(outPath, modelPaths, MethodExtractor.getMethods());

        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                MethodExtractor.getMethods(), modelPaths);

        if (MutantTester.usingBaseline()) {
            MutantTester.writeBaseline(outPath);
        }
        MutantTester.writeLogs(outPath, modelPaths);
        MutantTester.writeResults(outPath, modelPaths);

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
                    case "use.beams":
                        MethodMutator.useBeams(Boolean.parseBoolean(val));
                        break;
                    case "num.beams":
                        MethodMutator.setNumBeams(Integer.parseInt(val));
                        break;
                    case "parallel":
                        MutantTester.setParallel(Boolean.parseBoolean(val));
                        break;
                    case "compile.command":
                        MutantTester.setCompileCmd(val.split(" "));
                        break;
                    case "test.command":
                        MutantTester.setTestCmd(val.split(" "));
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
                    case "timeout":
                        MutantTester.setTimeout(Integer.parseInt(val));
                        break;
                }
            }
        }
    }

}
