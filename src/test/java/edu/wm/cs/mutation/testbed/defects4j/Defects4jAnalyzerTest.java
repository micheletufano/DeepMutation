package edu.wm.cs.mutation.testbed.defects4j;

public class Defects4jAnalyzerTest {

    public static void main(String[] args) {

        String systemsRoot = args[0];
        String projectName = args[1];
        String libRoot = args[2];
        String mutationModelRoot = args[3];
        String spoonModelRoot = args[4];
        String revisionsRoot = args[5];
        String out = args[6];
        boolean compiled = Boolean.parseBoolean(args[7]);
        String defects4j = args[8];
        int beamWidth = Integer.parseInt(args[9]);

        Defects4jAnalyzer.analyze(systemsRoot, projectName,
                libRoot, mutationModelRoot,
                spoonModelRoot, revisionsRoot,
                out, compiled, defects4j, beamWidth);

    }

}
