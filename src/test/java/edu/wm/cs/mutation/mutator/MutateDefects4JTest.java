package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.extractor.MethodExtractor;

import java.util.ArrayList;
import java.util.List;

public class MutateDefects4JTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String srcRootPath = dataPath + "Chart/";
        String outRootPath = dataPath + "out/Chart/";
        String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
        String libDir = dataPath + "spoonModel/lib/Chart";
        boolean compiled = true;
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractFromDefects4J(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
        MethodAbstractor.abstractMethodsFromDefects4J(MethodExtractor.getDefects4jMap(), idiomPath);

        // MethodMutator
        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        MethodMutator.mutateMethodsFromDefects4J(MethodAbstractor.getAbstractedDefects4JMethods(), modelDirs);
    }

}
