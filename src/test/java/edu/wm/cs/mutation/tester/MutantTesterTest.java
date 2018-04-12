package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MutantTesterTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projPath = dataPath + "Chart/1/b/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libPath = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        String defects4j = System.getProperty("user.home") + "/defects4j/framework/bin/defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");
        MutantTester.setCompileFailStrings("FAIL");
        MutantTester.setTestFailStrings("Failing");
        MutantTester.useBaseline(false);

        String idiomPath = dataPath + "idioms.csv";
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        String inputMethodsPath = dataPath + "methods.input";

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsPath);
        MethodExtractor.writeMethods(outPath);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        MethodAbstractor.writeMethods(outPath);
        MethodAbstractor.writeMappings(outPath);

        MethodMutator.setPython("python3");
        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        MethodMutator.writeMutants(outPath, modelPaths);

        MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
        MethodTranslator.writeMutants(outPath, modelPaths);

        MethodTranslator.createMutantFiles(outPath, modelPaths, MethodExtractor.getMethods());

//        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantsMap(),
//                MethodExtractor.getMethods(), modelPaths);
//
//        if (MutantTester.usingBaseline()) {
//            IOHandler.writeBaseline(outPath, MutantTester.getCompileBaseline(), "compile");
//            IOHandler.writeBaseline(outPath, MutantTester.getTestBaseline(), "test");
//        }
//
//        IOHandler.writeLogs(outPath, MutantTester.getCompileLogs(), modelPaths, "compile");
//        IOHandler.writeLogs(outPath, MutantTester.getTestLogs(), modelPaths, "test");
//        IOHandler.writeResults(outPath, MutantTester.getCompilable(), modelPaths, "compile");
//        IOHandler.writeResults(outPath, MutantTester.getSuccessful(), modelPaths, "test");
    }

}
