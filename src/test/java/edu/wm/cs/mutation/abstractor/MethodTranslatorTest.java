package edu.wm.cs.mutation.abstractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;

import java.util.ArrayList;
import java.util.List;

public class MethodTranslatorTest {

    public static void main(String[] args) {

        String dataPath = "data/";
        String rootPath = dataPath + "Chart/1/b/";
        String sourcePath = rootPath + "source/";
        String outPath = dataPath + "out/Chart/1/b/";
        String libDir = dataPath + "spoonModel/lib/Chart";
        int complianceLvl = 4;
        boolean compiled = true;

        String idiomPath = dataPath + "idioms.csv";

        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        MethodExtractor.extractMethods(rootPath, sourcePath, libDir, complianceLvl, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelDirs);

        IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

        MethodTranslator.translate(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelDirs);
        IOHandler.writeMutants(outPath, MethodTranslator.getTranslatedMutantsMap(), modelDirs, false);
    }

}
