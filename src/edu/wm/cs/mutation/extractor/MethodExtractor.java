package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public class MethodExtractor {

    private static final String BUGGY_DIR = "/b/";
    private static final String KEY_OUTPUT = "methods.key";
    private static final String SRC_OUTPUT = "methods.src";
    private static Map<String, Map<String, String>> rawMethodsMap;
    
    public static void extractMethods(String srcRootPath, String outRootPath, String modelBuildingInfoPath,
                                      String libDir, boolean compiled) {
        
        System.out.println("Extracting methods... ");

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.init(modelBuildingInfoPath);
        File[] revisions = new File(srcRootPath).listFiles(File::isDirectory);
        sortRevisionDirectories(revisions);
        
        rawMethodsMap = new HashMap<>();
        int i=0;
        for (File rev : revisions) {
            System.out.println("  Processing " + rev.toString() + " (" + ++i + "/" + revisions.length + ")... ");
            
            Map<String, String> revMethodsMap = new HashMap<>();
            //Extract Model Building Info
            int confID = Integer.parseInt(rev.getName());
            String srcDir = modelConfig.getSrcDir(confID);
            int complianceLvl = modelConfig.getComplianceLevel(confID);
            String sourcePath = rev.getAbsolutePath() + BUGGY_DIR + srcDir;
            
            // Build Spoon model
            System.out.println("    Building spoon model... ");
            if(compiled) {
                libDir = rev.getAbsolutePath() + BUGGY_DIR;
            }

            SpoonAPI spoon = SpoonConfig.buildModel(sourcePath, complianceLvl, libDir, compiled);

            //Create out dir
            String outDir = createOutDir(outRootPath, rev);
          
            // Generate methods
            System.out.println("    Generating methods... ");
            
            List<CtMethod> methods = spoon.getFactory()
                    .Package()
                    .getRootPackage()
                    .getElements(new TypeFilter<>(CtMethod.class));

            // Write methods
            System.out.println("    Writing methods... ");

            List<String> signatures = new ArrayList<>();
            List<String> bodies = new ArrayList<>();         

            for(CtMethod method : methods) {
                String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
                SourcePosition sp = method.getPosition();
                String body = sp.getCompilationUnit()
                                .getOriginalSourceCode()
                                .substring(sp.getSourceStart(), sp.getSourceEnd() + 1);

                signatures.add(signature);
                bodies.add(body);
                revMethodsMap.put(signature, body);
            }
            rawMethodsMap.put(sourcePath, new HashMap<String, String>(revMethodsMap));

            try {
                Files.write(Paths.get(outDir + KEY_OUTPUT), signatures);
                Files.write(Paths.get(outDir + SRC_OUTPUT), bodies);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("  done.");
        }

        System.out.println("done.");
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
    public static Map<String, Map<String, String>> getRawMethods() {
    	    return rawMethodsMap;
    }
    private static void sortRevisionDirectories(File[] revisions){
		Arrays.sort(revisions, new Comparator<File>() {
			public int compare(File f1, File f2) {
				try {
					int i1 = Integer.parseInt(f1.getName());
					int i2 = Integer.parseInt(f2.getName());
					return i1 - i2;
				} catch(NumberFormatException e) {
					throw new AssertionError(e);
				}
			}
		});
	}
}
