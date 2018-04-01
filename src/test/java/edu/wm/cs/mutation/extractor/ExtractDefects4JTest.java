package edu.wm.cs.mutation.extractor;

import edu.wm.cs.mutation.io.IOHandler;

import java.util.HashSet;
import java.util.List;

public class ExtractDefects4JTest {
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
       
        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        
		if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractMethods(input, libPath, compiled, inputMethods);
            IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);
        }
    }

}
