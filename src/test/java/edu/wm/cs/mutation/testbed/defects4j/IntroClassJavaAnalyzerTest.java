package edu.wm.cs.mutation.testbed.defects4j;

public class IntroClassJavaAnalyzerTest {

    public static void main(String[] args) {

//		String programsRoot = "/scratch/mtufano.scratch/projects/learningFixes/IntroClassJava/IntroClassJava/dataset/checksum/";
//		String mutationModelRoot = "/scratch/mtufano.scratch/tmp/IntroClassJava_Test/models/";
//		String out = "/scratch/mtufano.scratch/tmp/IntroClassJava_Test/";
//		String idiomPath = "/scratch/mtufano.scratch/projects/learningFixes/IntroClassJava/idioms.csv";
//		String mvnBin = "";
//		int beamWidth = 10;

        String programsRoot = args[0];
        String mutationModelRoot = args[1];
        String out = args[2];
        String idiomPath = args[3];
        String mvnBin = args[4];
        int beamWidth = Integer.parseInt(args[5]);


        IntroClassJavaAnalyzer.analyze(programsRoot, mutationModelRoot, out, idiomPath, mvnBin, beamWidth);

    }

}
