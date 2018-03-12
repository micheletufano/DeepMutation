package edu.wm.cs.mutation.mutator;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;

import java.util.ArrayList;
import java.util.List;

public class FastMutatorTest {

    public static void main(String[] args) {

        String dataPath = "data/";
        String rootPath = dataPath + "WebServer/";
        String sourcePath = rootPath + "src/";
        String outPath = dataPath + "out/WebServer/";
        String libDir = null;
        int complianceLvl = 4;
        boolean compiled = false;

        //Idiom path
        String idiomPath = dataPath + "idioms.csv";

        MethodExtractor.extractMethods(rootPath, sourcePath, libDir, complianceLvl, compiled);
        MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);

        IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

        // MethodMutator
        List<String> modelDirs = new ArrayList<>();
        modelDirs.add(dataPath + "models/50len_ident_lit/");

        MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelDirs);
        IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelDirs, true);
    }

}
