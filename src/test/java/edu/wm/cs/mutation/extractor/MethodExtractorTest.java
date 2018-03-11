package edu.wm.cs.mutation.extractor;

public class MethodExtractorTest {

    public static void main(String[] args) {
        //Chart
        String dataPath = "data/";
        String rootPath = dataPath + "/Chart/1/b/";
        String sourcePath = rootPath + "source/";
        String outPath = dataPath + "out/Chart/1/b";
        String libDir = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        MethodExtractor.extractMethods(rootPath, outPath, sourcePath, libDir, complianceLvl, compiled);
    }
}
