package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractorTest {
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
        MethodExtractor.extractMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethods(), idiomPath);
        IOHandler.writeMethods(MethodExtractor.getRawMethods(), false);
        IOHandler.writeMethods(MethodAbstractor.getAbstractedMethods(), abstracted);
        IOHandler.writeMappings(MethodAbstractor.getMappings());
    }

}
