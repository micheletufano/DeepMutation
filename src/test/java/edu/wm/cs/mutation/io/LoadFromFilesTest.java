package edu.wm.cs.mutation.io;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        String idiomPath = dataPath + "idioms.csv";
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");
        String inputMethodsPath = dataPath + "methods.input";

        String defects4j = "defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");
        MutantTester.setCompileFailStrings("FAIL");
        MutantTester.setTestFailStrings("FAIL", "Failing");
        MutantTester.useBaseline(false);

        String tmpPath;
        try {
            tmpPath = Files.createTempDirectory(Paths.get(System.getProperty("user.home")), null)
                    .toAbsolutePath().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // MethodExtractor cannot read from file due to multi-line source code
        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethodsPath);
        MethodExtractor.writeMethods(outPath);

        MethodExtractor.writeMethods(tmpPath);

        // MethodAbstractor
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        MethodAbstractor.writeMethods(outPath);
        MethodAbstractor.writeMappings(outPath);

        MethodAbstractor.readMethods(outPath);
        MethodAbstractor.readMappings(outPath);
        MethodAbstractor.writeMethods(tmpPath);
        MethodAbstractor.writeMappings(tmpPath);

        // MethodMutator
        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        MethodMutator.writeMutants(outPath, modelPaths);

        MethodMutator.readMutants(outPath, modelPaths);
        MethodMutator.writeMutants(tmpPath, modelPaths);

        // MethodTranslator
        MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
        MethodTranslator.writeMutants(outPath, modelPaths);
        MethodTranslator.createMutantFiles(outPath, modelPaths, MethodExtractor.getMethods());

        MethodTranslator.readMutants(outPath, modelPaths);
        MethodTranslator.writeMutants(tmpPath, modelPaths);
        MethodTranslator.createMutantFiles(tmpPath, modelPaths, MethodExtractor.getMethods());

        // MutantTester (check with `diff -rq outPath tmpPath` on UNIX)
        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                MethodExtractor.getMethods(), modelPaths);

        if (MutantTester.usingBaseline()) {
            MutantTester.writeBaseline(outPath);
        }
        MutantTester.writeLogs(outPath, modelPaths);
        MutantTester.writeResults(outPath, modelPaths);

        if (MutantTester.usingBaseline()) {
            MutantTester.writeBaseline(tmpPath);
        }
        MutantTester.writeLogs(tmpPath, modelPaths);
        MutantTester.writeResults(tmpPath, modelPaths);
    }
}
