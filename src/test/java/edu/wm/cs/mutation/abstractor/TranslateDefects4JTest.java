package edu.wm.cs.mutation.abstractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

public class TranslateDefects4JTest {
	public static void main(String[] args) {
        String dataPath = "data/";

        //Chart
        String projBasePath = dataPath + "Chart/";
        String outBasePath = dataPath + "out/Chart/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
		String inputMethodPath = dataPath + "methods.input";
		boolean compiled = true;
		boolean specified = false;
		HashSet<String> inputMethods = null;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";
        
        if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}

        // MethodMutator
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

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
        }
	}
}
