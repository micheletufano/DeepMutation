package edu.wm.cs.mutation.testbed.defects4j;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;

import edu.wm.cs.mutation.abstractor.MethodAbstractor;
import edu.wm.cs.mutation.abstractor.MethodTranslator;
import edu.wm.cs.mutation.extractor.MethodExtractor;
import edu.wm.cs.mutation.extractor.ModelConfig;
import edu.wm.cs.mutation.io.IOHandler;
import edu.wm.cs.mutation.mutator.MethodMutator;
import edu.wm.cs.mutation.tester.MutantTester;

public class Defects4jAnalyzer {

	private final static String BUGGY_DIR = "/b/";

	public static void analyze(String systemsRoot, String projectName, String libRoot, String mutationModelRoot, String spoonModelRoot, String revisionsRoot, String out, boolean compiled, String defects4j, int beamWidth) {

		String projPath = systemsRoot + projectName + "/";
		String libPath = libRoot + projectName + "/";
		String outProject = out + projectName + "/";
		String outProjectLog = out + "/log/" + projectName;
		String modelBuildingInfo = spoonModelRoot + projectName + ".json";
		String revisionFile = revisionsRoot + projectName + "/revs.list";
		String idiomPath = mutationModelRoot + "/idioms.csv";
		IOHandler.createDirectories(outProjectLog);
		
		//Set up mutation models
		List<String> modelPaths = IOHandler.listDirectoriesPaths(mutationModelRoot);

		//Reading Spoon model info
		ModelConfig modelInfo = new ModelConfig();	
		modelInfo.init(modelBuildingInfo);

		String[] revs = IOHandler.readRevsCSV(revisionFile);

		PrintStream console = System.out;

		for(String rev : revs) {

			console.println(projectName + " - " + rev);
			
			//Extract Model Building Info
			int confID = Integer.parseInt(rev);
			String srcDir = modelInfo.getSrcPath(confID);
			int complianceLvl = modelInfo.getComplianceLevel(confID);
			String srcPath = projPath + "/" + rev + BUGGY_DIR + srcDir;
			String outPath = outProject + rev + BUGGY_DIR;
			String logFile = outProjectLog + "/" + rev + ".log";
			IOHandler.setOutputStream(logFile);

			//Read input methods
			String inputMethodPath = revisionsRoot + projectName + "/" + rev + ".key";
			HashSet<String> inputMethods = IOHandler.readInputMethods(inputMethodPath);


			MutantTester.setCompileCmd(defects4j, "compile");
			MutantTester.setTestCmd(defects4j, "test");

			MethodMutator.useBeams(true);
			MethodMutator.setNumBeams(beamWidth);
			MethodMutator.setPython("python");
			MethodAbstractor.setInputMode(true);
			
			MethodExtractor.extractMethods(projPath, srcPath, libPath, complianceLvl, compiled, inputMethods);
			IOHandler.writeMethods(outPath, MethodExtractor.getRawMethodsMap(), false);

			MethodAbstractor.abstractMethods(MethodExtractor.getRawMethodsMap(), idiomPath);
			IOHandler.writeMethods(outPath, MethodAbstractor.getAbstractedMethods(), true);
			IOHandler.writeMappings(outPath, MethodAbstractor.getMappings());

			MethodMutator.mutateMethods(outPath, MethodAbstractor.getAbstractedMethods(), modelPaths);
			IOHandler.writeMutants(outPath, MethodMutator.getMutantsMap(), modelPaths, true);

			MethodTranslator.translateMethods(MethodMutator.getMutantsMap(), MethodAbstractor.getMappings(), modelPaths);
			IOHandler.writeMutants(outPath, MethodTranslator.getTranslatedMutantsMap(), modelPaths, false);

			IOHandler.createMutantFiles(outPath, MethodTranslator.getTranslatedMutantsMap(),
					MethodExtractor.getMethods(), modelPaths);

			MutantTester.testMutants(projPath, MethodTranslator.getTranslatedMutantsMap(),
					MethodExtractor.getMethods(), modelPaths);
			IOHandler.writeBaseline(outPath, MutantTester.getCompileBaseline(), "compile");
			IOHandler.writeBaseline(outPath, MutantTester.getTestBaseline(), "test");
			IOHandler.writeLogs(outPath, MutantTester.getCompileLogs(), modelPaths, "compile");
			IOHandler.writeLogs(outPath, MutantTester.getTestLogs(), modelPaths, "test");
			IOHandler.writeResults(outPath, MutantTester.getCompilable(), modelPaths, "compile");
			IOHandler.writeResults(outPath, MutantTester.getSuccessful(), modelPaths, "test");


		}
		
		
		
		System.setOut(console);

	}




}
