package edu.wm.cs.mutation.io;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;

import java.util.ArrayList;
import java.util.List;

public class LoadFromFilesTest {

    public static void main(String[] args) {

        String dataPath = "data/";
        String projPath = dataPath + "Chart/1/b/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libPath = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        String defects4j = "defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");
        MutantTester.setCompileFailStrings("FAIL");
        MutantTester.setTestFailStrings("FAIL", "Failing");
        MutantTester.useBaseline(false);

        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        String inputMethodsPath = dataPath + "methods.input";

//        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsPath);
//        MethodExtractor.writeMethods(outPath);

//        MethodAbstractor.readMethods(outPath);
//        MethodAbstractor.readMappings(outPath);
//        MethodAbstractor.writeMethods(System.getProperty("user.home"));
//        MethodAbstractor.writeMappings(System.getProperty("user.home"));

//        MethodMutator.readMutants(outPath, modelPaths);
//        MethodMutator.writeMutants(System.getProperty("user.home"), modelPaths);

        MethodTranslator.readMutants(outPath, modelPaths);
        MethodTranslator.writeMutants(System.getProperty("user.home"), modelPaths);

        MethodExtractor.buildModel(projPath, srcPath, libPath, complianceLvl, compiled);
        MethodTranslator.createMutantFiles(System.getProperty("user.home"), modelPaths, MethodExtractor.getMethods());

        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                MethodExtractor.getMethods(), modelPaths);

        if (MutantTester.usingBaseline()) {
            IOHandler.writeBaseline(outPath, MutantTester.getCompileBaseline(), "compile");
            IOHandler.writeBaseline(outPath, MutantTester.getTestBaseline(), "test");
        }

        IOHandler.writeLogs(outPath, MutantTester.getCompileLogs(), modelPaths, "compile");
        IOHandler.writeLogs(outPath, MutantTester.getTestLogs(), modelPaths, "test");
        IOHandler.writeResults(outPath, MutantTester.getCompilable(), modelPaths, "compile");
        IOHandler.writeResults(outPath, MutantTester.getSuccessful(), modelPaths, "test");
    }
}
