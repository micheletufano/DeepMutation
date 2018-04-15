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
		String inputMethodsPath = dataPath + "methods.input";
		boolean compiled = true;
		boolean specified = false;
		HashSet<String> inputMethods = null;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";
        
        // MethodMutator
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractMethods(input, libPath, compiled, inputMethodsPath);
            MethodExtractor.writeMethods(input.getOutPath());

            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
            MethodAbstractor.writeMethods(input.getOutPath());
            MethodAbstractor.writeMappings(input.getOutPath());

            MethodMutator.mutateMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), modelPaths);
            MethodMutator.writeMutants(input.getOutPath(), modelPaths);

            MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
            MethodTranslator.writeMutants(input.getOutPath(), modelPaths);
        }
	}
}
