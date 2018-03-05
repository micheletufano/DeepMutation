package edu.wm.cs.mutation.mutator;

public class MutantGeneratorTest {

	public static void main(String[] args) {
		String dataPath = "data/";
		String BUGGY_DIR = "/b/";
		// Chart
		String srcRootPath = dataPath + "Chart/";
		String outRootPath = dataPath + "out/Chart/";
		String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
		String libDir = dataPath + "spoonModel/lib/Chart";
		boolean compiled = true;
		String revID = "2";
		MutantGenerator.generateMutants(srcRootPath + revID +BUGGY_DIR, outRootPath + revID +BUGGY_DIR , revID, modelBuildingInfoPath, libDir, compiled);
	}
}
