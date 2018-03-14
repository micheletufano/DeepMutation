package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

import java.util.ArrayList;
import java.util.List;

public class PipelineDefects4JTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projBasePath = dataPath + "Chart/1/b/";
        String outBasePath = dataPath + "out/Chart/1/b/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;

        String idiomPath = dataPath + "idioms.csv";

        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractFromDefects4J(input, libPath, compiled);
            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
            MethodMutator.mutateMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), modelDirs);
            MethodTranslator.translateMethods(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelDirs);

            IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);
            IOHandler.writeMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), true);
            IOHandler.writeMappings(input.getOutPath(), MethodAbstractor.getMappings());
            IOHandler.writeMutants(input.getOutPath(), MethodMutator.getMutantsMap(), modelDirs, true);
            IOHandler.writeMutants(input.getOutPath(), MethodTranslator.getTranslatedMutantsMap(), modelDirs, false);

            IOHandler.createMutantFiles(input.getOutPath(), input.getSrcPath(), MethodTranslator.getTranslatedMutantsMap(),    // mutant files
                    MethodExtractor.getMethods(), modelDirs);
        }
    }
}
