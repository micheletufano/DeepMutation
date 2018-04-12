package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

import java.util.ArrayList;
import java.util.List;

public class FastPipelineTest {

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

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, null);
        MethodExtractor.writeMethods(outPath);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        MethodAbstractor.writeMethods(outPath);
        MethodAbstractor.writeMappings(outPath);

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelPaths, true);               // abstract mutants

        MethodTranslator.translateMethods(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelPaths);
        IOHandler.writeMutants(outPath, MethodTranslator.getTranslatedMutantsMap(), modelPaths, false); // mutants

        IOHandler.createMutantFiles(outPath, MethodTranslator.getTranslatedMutantsMap(),    // mutant files
                MethodExtractor.getMethods(), modelPaths);
    }

}
