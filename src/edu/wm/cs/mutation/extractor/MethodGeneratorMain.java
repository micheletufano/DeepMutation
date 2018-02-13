package edu.wm.cs.mutation.extractor;


public class MethodGeneratorMain {
	public static void main(String[] args) {
        //Chart
		String dataPath = "/Users/sw/Desktop/src2txt_data";
		String srcRootPath = dataPath+"/Chart/";
		String outRootPath = dataPath +"/out/Chart/";
		String modelBuildingInfoPath = dataPath + "/model/Chart.json";
		String libDir = dataPath + "/lib/Chart";
		boolean compiled = true;

		MethodGenerator.generateMethods(srcRootPath, outRootPath, modelBuildingInfoPath, libDir, compiled);
	}
}
