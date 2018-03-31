package edu.wm.cs.mutation.extractor;

import java.util.HashSet;

import edu.wm.cs.mutation.io.IOHandler;

public class MethodExtractorTest {

    public static void main(String[] args) {
        //Chart
        String dataPath = "data/";
        String projPath = dataPath + "/Chart/1/b/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libPath = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        String inputMethodPath = dataPath + "methods.input";
        boolean compiled = true;
        boolean specified = true;
        HashSet<String> inputMethods = null;
        
		if (specified) {
			inputMethods = IOHandler.readInputMethods(inputMethodPath);
		}

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethods);
        IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);
    }
}
