package edu.wm.cs.mutation.abstractor;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodTranslatorTest {
	public static void main(String[] args) {
        String dataPath = "data/";

        //Chart
        String srcRootPath = dataPath + "Chart/";
        String outRootPath = dataPath + "out/Chart/";
        String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
        String libDir = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        boolean abstracted = true;
        MethodExtractor.extractFromDefects4J(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getDefects4jMap(), idiomPath);
        IOHandler.writeMethods(MethodExtractor.getDefects4jMap(), false);
        IOHandler.writeMethods(MethodAbstractor.getAbstractedMethods(), abstracted);
        IOHandler.writeMappings(MethodAbstractor.getMappings());

// Feed the abstracted methods to predictor, generate mutated methods and put them to a LinkedHashMap		
//		Map<String, LinkedHashMap<String, String>> predMethods = MethodMutator.getmutatedMethods();
        Map<String, LinkedHashMap<String, String>> predMethods = MethodTranslator.getRewPredMethods(MethodAbstractor.getAbstractedMethods());
		MethodTranslator.translate(predMethods, MethodAbstractor.getMappings());
	}
}
