package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodAbstractorTest;
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

        String defects4j = "defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");
        MutantTester.setCompileFailStrings("FAIL");
        MutantTester.setTestFailStrings("FAIL", "Failing");
        MutantTester.useBaseline(false);

        String idiomPath = dataPath + "idioms.csv";
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        String inputMethodsPath = dataPath + "methods.input";

        MethodMutator.setPython("python3");

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsPath);
        MethodExtractor.writeMethods(outPath);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
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

}
