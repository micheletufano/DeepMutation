package edu.wm.cs.mutation.testbed.defects4j;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;
import spoon.reflect.declaration.CtMethod;

public class IntroClassJavaAnalyzer {

    private static final String SRC = "src/main/java/introclassJava/";
    private static final String LIB_PATH = null;
    private static final int complianceLvl = 7;
    private static final boolean compiled = false;

    private static final String EXEC_METHOD = "exec";


    public static void analyze(String programsRoot, String mutationModelRoot, String out, String idiomPath, String mvnBin, int beamWidth) {

        //Mutation models settings
        List<String> modelPaths = IOHandler.listDirectoriesPaths(mutationModelRoot);

        //MutantTester settings
        MutantTester.setCompileCmd(mvnBin, "compile");
        MutantTester.setTestCmd(mvnBin, "test");
        MutantTester.setCompileFailStrings("FAIL");
        MutantTester.setTestFailStrings("ailure", "Failing", "FAIL");
        MutantTester.useBaseline(false);
        MutantTester.setParallel(false);
        String wrapperLibFile = "dist/libWrapper.so";

        //MethodMutator settings
        MethodMutator.useBeams(true);
        MethodMutator.setNumBeams(beamWidth);
        MethodMutator.setPython("python3");

        MethodAbstractor.setInputMode(true);

        PrintStream console = System.out;

        List<String> programs = IOHandler.listDirectoriesPaths(programsRoot);

        for (String program : programs) {

            List<String> versions = IOHandler.listDirectoriesPaths(program);

            for (String programVersion : versions) {

                String projPath = programVersion;
                String srcPath = projPath + SRC;

                String[] pathInfo = programVersion.split("/");
                String versionName = pathInfo[pathInfo.length - 1];
                String programID = pathInfo[pathInfo.length - 2];
                String programName = pathInfo[pathInfo.length - 3];
                String outPath = out + "/" + programName + "/" + programID + "/" + versionName + "/";
                String outProjectLog = out + "/log/" + programName + "/" + programID;
                IOHandler.createDirectories(outProjectLog);

                System.out.println(programName + " - " + programID + " - " + versionName);

                String logFile = outProjectLog + "/" + versionName + ".log";
                IOHandler.setOutputStream(logFile);


                //Extract all methods
                MethodExtractor.extractMethods(projPath, srcPath, LIB_PATH, complianceLvl, compiled, null);
                MethodExtractor.writeMethods(outPath);

                //Find exec method
                findExecMethod();

                //Abstract exec method
                MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
                MethodAbstractor.writeMethods(outPath);
                MethodAbstractor.writeMappings(outPath);

                MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
                MethodMutator.writeMutants(outPath, modelPaths);

                MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
                MethodTranslator.writeMutants(outPath, modelPaths);
                MethodTranslator.createMutantFiles(outPath, modelPaths, MethodExtractor.getMethods());

                MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                        MethodExtractor.getMethods(), modelPaths, wrapperLibFile);

                if (MutantTester.usingBaseline()) {
                    MutantTester.writeBaseline(outPath);
                }
                MutantTester.writeLogs(outPath, modelPaths);
                MutantTester.writeResults(outPath, modelPaths);

                System.setOut(console);
            }

        }


    }


    private static void findExecMethod() {
        LinkedHashMap<String, String> execRawMethod = new LinkedHashMap<>();
        LinkedHashMap<String, String> extractedRawMethods = MethodExtractor.getRawMethodsMap();

        for (String key : extractedRawMethods.keySet()) {
            if (key.contains(EXEC_METHOD)) {
                System.out.println("Found method exec!");
                execRawMethod.put(key, extractedRawMethods.get(key));
            }
        }

        List<CtMethod> execMethod = new ArrayList<>();
        List<CtMethod> extractedMethods = MethodExtractor.getMethods();
        for (CtMethod m : extractedMethods) {
            if (m.getSimpleName().contains(EXEC_METHOD)) {
                execMethod.add(m);
            }
        }

        MethodExtractor.setRawMethodsMap(execRawMethod);
        MethodExtractor.setMethods(execMethod);
    }


}
