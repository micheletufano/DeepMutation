package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

import java.util.HashSet;
import java.util.List;

public class AbstractDefects4JTest {
	public static void main(String[] args) {

		String dataPath = "data/";

		// Chart
		String projBasePath = dataPath + "Chart/";
		String outBasePath = dataPath + "out/Chart/";
		String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
		String libPath = dataPath + "spoonModel/lib/Chart";
		String inputMethodPath = dataPath + "methods.input";
		boolean compiled = true;
		boolean specified = false;
		HashSet<String> inputMethods = null;

		// Idiom path
		String idiomPath = dataPath + "idioms.csv";

		List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath,
				modelConfigPath);
		if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}
		for (Defects4JInput input : inputs) {
			MethodExtractor.extractMethods(input, libPath, compiled, inputMethods);
			IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);

			MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
			IOHandler.writeMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), true);
			IOHandler.writeMappings(input.getOutPath(), MethodAbstractor.getMappings());
		}
	}

}
