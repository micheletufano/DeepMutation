package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

import java.util.ArrayList;
import java.util.List;

public class MutantTesterTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projPath = dataPath + "WebServer/";
        String srcPath = projPath + "src/";
        String outPath = dataPath + "out/WebServer/";
        String libPath = null;
        int complianceLvl = 4;
        boolean compiled = false;

        String idiomPath = dataPath + "idioms.csv";

        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled);
        IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
        IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelPaths, true);

        MethodTranslator.translateMethods(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelPaths);
        IOHandler.writeMutants(outPath, MethodTranslator.getTranslatedMutantsMap(), modelPaths, false);

        IOHandler.createMutantFiles(outPath, srcPath, MethodTranslator.getTranslatedMutantsMap(),
                MethodExtractor.getMethods(), modelPaths);

        MutantTester.testMutants(outPath, projPath, srcPath, MethodTranslator.getTranslatedMutantsMap(), modelPaths);
    }

}
