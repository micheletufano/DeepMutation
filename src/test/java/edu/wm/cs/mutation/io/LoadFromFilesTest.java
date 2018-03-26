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

        String defects4j = System.getProperty("user.home") + "/defects4j/framework/bin/defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");

        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        MethodExtractor.setRawMethodsMap(IOHandler.readMethods(outPath, false));
        MethodExtractor.buildModel(projPath, srcPath, libPath, complianceLvl, compiled);

        MethodAbstractor.setAbstractedMethods(IOHandler.readMethods(outPath, true));
        MethodAbstractor.setMappings(IOHandler.readMappings(outPath));

        MethodMutator.setMutantsMap(IOHandler.readMutants(outPath, modelPaths, true));

        MethodTranslator.setTranslatedMutantsMap(IOHandler.readMutants(outPath, modelPaths, false));

        IOHandler.createMutantFiles(outPath, MethodTranslator.getTranslatedMutantsMap(),
                MethodExtractor.getMethods(), modelPaths);

        MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantsMap(),
                MethodExtractor.getMethods(), modelPaths);
    }
}
