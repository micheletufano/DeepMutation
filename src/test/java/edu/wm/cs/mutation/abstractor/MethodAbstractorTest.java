package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractorTest {

    public static void main(String[] args) {
        //Chart
        String dataPath = "data/";
        String rootPath = dataPath + "/Chart/1/b/";
        String sourcePath = rootPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libDir = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractMethods(rootPath, sourcePath, libDir, complianceLvl, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);

        IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
        IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());
    }
}
