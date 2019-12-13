package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.tester.MutantTester;

import java.util.ArrayList;
import java.util.List;

public class RestartTest {

    public static void main(String[] args) {

        String dataPath = "data/";
        String projPath = dataPath + "in/Chart/1f/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1f/";
        String libPath = dataPath + "spoonLibs/Chart";
        int complianceLvl = 4;
        boolean compiled = true;
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");
        String wrapperLibFile = "dist/libWrapper.so";

        String defects4j = "defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");

        MethodExtractor.buildModel(projPath, srcPath, libPath, complianceLvl, compiled);
        MethodTranslator.readMutants(outPath, modelPaths);

        MutantTester.parallel = false;
        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantMaps(),
                MethodExtractor.getMethods(), modelPaths, wrapperLibFile);

        if (MutantTester.usingBaseline()) {
            MutantTester.writeBaseline(outPath);
        }
        MutantTester.writeLogs(outPath, modelPaths);
        MutantTester.writeResults(outPath, modelPaths);
    }
}
