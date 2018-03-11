package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class AbstractDefects4JTest {
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
        MethodAbstractor.abstractMethodsFromDefects4J(MethodExtractor.getDefects4jMap(), idiomPath);
        IOHandler.writeMethodsFromDefects4J(MethodExtractor.getDefects4jMap(), false);
        IOHandler.writeMethodsFromDefects4J(MethodAbstractor.getAbstractedDefects4JMethods(), abstracted);
        IOHandler.writeMappingsFromDefects4J(MethodAbstractor.getDefects4jMappings());
    }

}
