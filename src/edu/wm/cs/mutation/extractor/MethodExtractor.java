package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

public class MethodExtractor {
    private static final String BUGGY_DIR = "/b/";

    public static void extractMethods(String srcRootPath, String outRootPath, String modelBuildingInfoPath,
                                      String libDir, boolean compiled) {

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.init(modelBuildingInfoPath);
        File[] revisions = new File(srcRootPath).listFiles(File::isDirectory);
        for(File rev : revisions) {
            //Create out dir
            String outDir = createOutDir(outRootPath, rev);

            //Extract Model Building Info
            int confID = Integer.parseInt(rev.getName());
            String srcDir = modelConfig.getSrcDir(confID);
            int complianceLvl = modelConfig.getComplianceLevel(confID);
            String sourcePath = rev.getAbsolutePath() + BUGGY_DIR + srcDir;

            if(compiled) {
                libDir = rev.getAbsolutePath() + BUGGY_DIR;
            }

            // Build Spoon model
            SpoonAPI spoon = SpoonConfig.buildModel(sourcePath, complianceLvl, libDir, compiled);

            // Generate methods
            List<CtMethod> methods = spoon.getFactory()
                    .Package()
                    .getRootPackage()
                    .getElements(new TypeFilter<>(CtMethod.class));


            for(CtMethod method : methods){
                String methodName = method.getSignature();
                System.out.println("Method Name:");
                System.out.println(methodName);

                SourcePosition sp = method.getPosition();
                String srcCode = sp.getCompilationUnit().getOriginalSourceCode().substring(sp.getSourceStart(), sp.getSourceEnd()+1);
                System.out.println("Method Source Code:");
                System.out.println(srcCode);

                //Write methods to file
                String methodPath = outDir + methodName;
                try {
                    Files.write(Paths.get(methodPath), srcCode.getBytes());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private static String createOutDir(String outRootPath, File rev){
        String outDir = outRootPath + rev.getName() + BUGGY_DIR;

        //Create dir
        try {
            Files.createDirectories(Paths.get(outDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outDir;
    }
}
