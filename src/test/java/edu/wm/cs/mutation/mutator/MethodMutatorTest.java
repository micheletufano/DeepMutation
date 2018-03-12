package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

import java.util.ArrayList;
import java.util.List;

public class MethodMutatorTest {

    public static void main(String[] args) {
        //Chart
        String dataPath = "data/";
        String rootPath = dataPath + "Chart/1/b/";
        String sourcePath = rootPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libDir = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractMethods(rootPath, sourcePath, libDir, complianceLvl, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);

//        IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
//        IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());

        // MethodMutator
        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelDirs);
        IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelDirs, true);
    }

}
