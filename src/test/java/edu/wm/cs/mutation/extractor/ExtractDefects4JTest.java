package edu.wm.cs.mutation.extractor;

import edu.wm.cs.mutation.io.IOHandler;

import java.util.List;

public class ExtractDefects4JTest {
    public static void main(String[] args) {

        String dataPath = "data/";
        String projBasePath = dataPath + "Chart/";
        String outBasePath = dataPath + "out/Chart/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractFromDefects4J(input, libPath, compiled);
            IOHandler.writeMethods(input.getOutPath(), MethodExtractor.getRawMethodsMap(), false);
        }
    }

}
