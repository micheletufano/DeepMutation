package edu.wm.cs.mutation.abstractor;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.wm.cs.mutation.extractor.MethodExtractor;

public class MethodAbstractorTest {
    public static void main(String[] args) {
        String dataPath = "data/";
        //Chart
        String srcRootPath = dataPath + "Chart/";
        String outRootPath = dataPath + "out/Chart/";
        String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
        String libDir = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;

        MethodExtractor.extractMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
        Map<String, LinkedHashMap<String, String>> rawMethods = MethodExtractor.getRawMethods();
        MethodAbstractor.abstractMethods(rawMethods);
        Map<String, LinkedHashMap<String, String>> absMethods = MethodAbstractor.getAbstractedMethods();
    }

}
