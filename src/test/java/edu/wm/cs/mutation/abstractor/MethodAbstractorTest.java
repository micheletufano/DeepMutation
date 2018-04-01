package edu.wm.cs.mutation.abstractor;

import java.util.HashSet;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractorTest {

	public static void main(String[] args) {
		// Chart
		String dataPath = "data/";
		String projPath = dataPath + "/Chart/1/b/";
		String srcPath = projPath + "source/";
		String outPath = dataPath + "out/Chart/1/b/";
		String libPath = dataPath + "spoonModel/lib/Chart";
		String inputMethodPath = dataPath + "methods.input";
		int complianceLvl = 4;
		boolean compiled = true;
		boolean specified = false;
		HashSet<String> inputMethods = null;

		// Idiom path
		String idiomPath = dataPath + "idioms.csv";
        
		if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}

		MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethods);
		IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

		MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
		IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
		IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());
	}
}
