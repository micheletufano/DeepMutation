package edu.wm.cs.mutation.test;

import java.util.Map;

import edu.wm.cs.mutation.abstractor.MethodsAbstractor;
import edu.wm.cs.mutation.extractor.MethodExtractor;

public class MethodAbstractorTest {
	public static void main(String[] args) {
	 String dataPath = "data/";
		//Chart
		String srcRootPath = dataPath + "Chart/";
		String outRootPath = dataPath + "out/Chart/";
		String modelBuildingInfoPath = dataPath + "model/Chart.json";
		String libDir = dataPath + "lib/Chart";
		boolean compiled = true;
		
		MethodExtractor.extractMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
		Map<String, Map<String, String>> rawMethods = MethodExtractor.getRawMethods();
		MethodsAbstractor.generateAbsCode(rawMethods);
	}
		
}
