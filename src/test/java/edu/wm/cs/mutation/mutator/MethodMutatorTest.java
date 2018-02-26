package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.extractor.MethodExtractor;

import java.util.ArrayList;
import java.util.List;

public class MethodMutatorTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String srcRootPath = dataPath + "Chart/";
        String outRootPath = dataPath + "out/Chart/";
        String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
        String libDir = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethods(), idiomPath);

        // MethodMutator
        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        MethodMutator.mutateMethods(MethodAbstractor.getAbstractedMethods(), modelDirs);
    }

}
