package edu.wm.cs.mutation.test.extractor;

import edu.wm.cs.mutation.extractor.MethodExtractor;

public class MethodExtractorTest {
	public static void main(String[] args) {

        String dataPath = "data/";

		//Lang
//		String srcRootPath = "/scratch/mtufano.scratch/projects/deeprepair/systems/Lang/";
//		String outRootPath = "/scratch/mtufano.scratch/tmp/spoonKeys/out/Lang/";
//		String modelBuildingInfoPath = "/scratch/mtufano.scratch/tmp/spoonKeys/modelInfo/lang.json";
//		String libDir = "/scratch/mtufano.scratch/tmp/spoonKeys/lib/Lang";
		
		//Time
//		String srcRootPath = "/scratch/mtufano.scratch/projects/deeprepair/systems/Time/";
//		String outRootPath = "/scratch/mtufano.scratch/tmp/spoonKeys/out/Time/";
//		String modelBuildingInfoPath = "/scratch/mtufano.scratch/tmp/spoonKeys/modelInfo/time.json";
//		String libDir = "/scratch/mtufano.scratch/tmp/spoonKeys/lib/Time";
		
		//Chart
		String srcRootPath = dataPath + "Chart/";
		String outRootPath = dataPath + "out/Chart/";
		String modelBuildingInfoPath = dataPath + "spoonModel/model/Chart.json";
		String libDir = dataPath + "spoonModel/lib/Chart";
		boolean compiled = true;

		//Math
//		String srcRootPath = "/scratch/mtufano.scratch/projects/deeprepair/systems/Math/";
//		String outRootPath = "/scratch/mtufano.scratch/tmp/spoonKeys/out/Math/";
//		String modelBuildingInfoPath = "/scratch/mtufano.scratch/tmp/spoonKeys/modelInfo/math.json";
//		String libDir = "/scratch/mtufano.scratch/tmp/spoonKeys/lib/Math";
		
		//Closure
//		String srcRootPath = "/scratch/mtufano.scratch/projects/deeprepair/builtSystems/Closure/";
//		String outRootPath = "/scratch/mtufano.scratch/tmp/ClosureTest/out/";
//		String modelBuildingInfoPath = "/scratch/mtufano.scratch/projects/deeprepair/spoonModel/model/Closure.json";
//		String libDir = "";
//		boolean compiled = true;
		
		//Mockito
//		String srcRootPath = "/scratch/mtufano.scratch/projects/deeprepair/systems/testedSystems/Mockito/";
//		String outRootPath = "/scratch/mtufano.scratch/projects/deeprepair/corpora/out/Mockito/";
//		String modelBuildingInfoPath = "/scratch/mtufano.scratch/projects/deeprepair/spoonModel/model/Mockito.json";
//		String libDir = "/scratch/mtufano.scratch/projects/deeprepair/spoonModel/lib/Mockito";
//		boolean compiled = true;

		MethodExtractor.extractMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
	}

}
