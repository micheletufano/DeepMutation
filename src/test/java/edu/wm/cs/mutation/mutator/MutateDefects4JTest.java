package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

import java.util.ArrayList;
import java.util.List;

public class MutateDefects4JTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projBasePath = dataPath + "Chart/";
        String outBasePath = dataPath + "out/Chart/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;
        String idiomPath = dataPath + "idioms.csv";

        // MethodMutator
        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractFromDefects4J(input, libPath, compiled);
            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
            MethodMutator.mutateMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), modelDirs);

            IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);
            IOHandler.writeMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), true);
            IOHandler.writeMappings(input.getOutPath(), MethodAbstractor.getMappings());
            IOHandler.writeMutants(input.getOutPath(), MethodMutator.getMutantsMap(), modelDirs, true);
        }
    }

}
