package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PipelineDefects4JTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projBasePath = dataPath + "Chart/";
        String outBasePath = dataPath + "out/Chart/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
        String inputMethodPath = dataPath + "methods.input";
        
        boolean compiled = true;
        boolean specified = true;
        HashSet<String> inputMethods = null;
        

        String idiomPath = dataPath + "idioms.csv";
        
        if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}

        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        String defects4j = System.getProperty("user.home") + "/defects4j/framework/bin/defects4j";
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractMethods(input, libPath, compiled, inputMethods);
            IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);

            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
            IOHandler.writeMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), true);
            IOHandler.writeMappings(input.getOutPath(), MethodAbstractor.getMappings());

            MethodMutator.mutateMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), modelPaths);
            IOHandler.writeMutants(input.getOutPath(), MethodMutator.getMutantsMap(), modelPaths, true);

            MethodTranslator.translateMethods(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelPaths);
            IOHandler.writeMutants(input.getOutPath(), MethodTranslator.getTranslatedMutantsMap(), modelPaths, false);

            IOHandler.createMutantFiles(input.getOutPath(), MethodTranslator.getTranslatedMutantsMap(),    // mutant files
                    MethodExtractor.getMethods(), modelPaths);

            MutantTester.testMutants(input.getProjPath(), MethodTranslator.getTranslatedMutantsMap(),
                    MethodExtractor.getMethods(), modelPaths);
        }
    }
}
