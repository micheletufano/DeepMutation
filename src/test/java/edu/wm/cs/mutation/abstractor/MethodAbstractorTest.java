package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

public class MethodAbstractorTest {

    public static void main(String[] args) {
        //Chart
        String dataPath = "data/";
        String projPath = dataPath + "/Chart/1/b/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libPath = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled);
        IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
        IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());
    }
}
