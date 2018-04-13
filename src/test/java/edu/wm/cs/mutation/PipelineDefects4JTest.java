package edu.wm.cs.mutation;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.Defects4JInput;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;
import java.util.ArrayList;
import java.util.List;


public class PipelineDefects4JTest {

    public static void main(String[] args) {
        String dataPath = "data/";
        String projBasePath = dataPath + "Chart/";
        String outBasePath = dataPath + "out/Chart/";
        String modelConfigPath = dataPath + "spoonModel/model/Chart.json";
        String libPath = dataPath + "spoonModel/lib/Chart";
        String inputMethodsPath = dataPath + "methods.input";
        
        boolean compiled = true;
        boolean specified = false;
        int tokenThreshold = 100;

        String idiomPath = dataPath + "idioms.csv";
        
        List<String> modelPaths = new ArrayList<>();
        modelPaths.add(dataPath + "models/50len_ident_lit/");

        String defects4j = "defects4j";
        MethodAbstractor.setInputMode(specified);
        MethodAbstractor.setTokenThreshold(tokenThreshold);
        MethodMutator.useBeams(true);
        MutantTester.setCompileCmd(defects4j, "compile");
        MutantTester.setTestCmd(defects4j, "test");

        List<Defects4JInput> inputs = MethodExtractor.generateDefect4JInputs(projBasePath, outBasePath, modelConfigPath);
        for (Defects4JInput input : inputs) {
            MethodExtractor.extractMethods(input, libPath, compiled, inputMethodsPath);
            MethodExtractor.writeMethods(input.getOutPath());
             
            MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
            MethodAbstractor.writeMethods(input.getOutPath());
            MethodAbstractor.writeMappings(input.getOutPath());

            MethodMutator.mutateMethods(input.getOutPath(), MethodAbstractor.getAbstractedMethods(), modelPaths);
            MethodMutator.writeMutants(input.getOutPath(), modelPaths);

            MethodTranslator.translateMethods(MethodMutator.getMutantMaps(), MethodAbstractor.getMappings(), modelPaths);
            MethodTranslator.writeMutants(input.getOutPath(), modelPaths);
            MethodTranslator.createMutantFiles(input.getOutPath(), modelPaths, MethodExtractor.getMethods());

            MutantTester.testMutants(input.getProjPath(), MethodTranslator.getTranslatedMutantMaps(),
                    MethodExtractor.getMethods(), modelPaths);

            if (MutantTester.usingBaseline()) {
                MutantTester.writeBaseline(input.getOutPath());
            }
            MutantTester.writeLogs(input.getOutPath(), modelPaths);
            MutantTester.writeResults(input.getOutPath(), modelPaths);
        }
    }
}
