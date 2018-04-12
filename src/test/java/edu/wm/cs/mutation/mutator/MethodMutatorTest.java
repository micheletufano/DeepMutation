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
        String projPath = dataPath + "Chart/1/b/";
        String srcPath = projPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libPath = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        // MethodMutator
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, null);
        MethodExtractor.writeMethods(outPath);

        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        MethodAbstractor.writeMethods(outPath);
        MethodAbstractor.writeMappings(outPath);

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
        IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelPaths, true);
    }

}
