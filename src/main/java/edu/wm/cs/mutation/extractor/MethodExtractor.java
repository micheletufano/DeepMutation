package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.util.*;

import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public class MethodExtractor {

    private static final String BUGGY_DIR = "/b/";
    private static Map<String, LinkedHashMap<String, String>> defects4jMap;
    private static LinkedHashMap<String,String> rawMethodsMap;

    public static void extractMethods(String rootPath, String sourcePath, String libDir,
                                      int complianceLvl, boolean compiled) {
        System.out.println("Extracting methods... ");

        File project = new File(rootPath);
        rawMethodsMap = new LinkedHashMap<>();
        System.out.println("  Processing " + project.toString() + "... ");

        // Build Spoon model
        System.out.println("    Building spoon model... ");

        if (compiled) {
            libDir = project.getAbsolutePath() + "/";
        }
        SpoonAPI spoon = SpoonConfig.buildModel(sourcePath, complianceLvl, libDir, compiled);

        // Generate methods
        System.out.println("    Generating methods... ");

        List<CtMethod> methods = spoon.getFactory()
                .Package()
                .getRootPackage()
                .getElements(new TypeFilter<>(CtMethod.class));

        System.out.println("    Saving methods... ");
        for (CtMethod method : methods) {
            String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
            SourcePosition sp = method.getPosition();
            String body = sp.getCompilationUnit()
                    .getOriginalSourceCode()
                    .substring(sp.getSourceStart(), sp.getSourceEnd() + 1);

            rawMethodsMap.put(signature, body);
        }
        System.out.println("  done.");
        System.out.println("done.");
    }

    public static void extractFromDefects4J(String srcRootPath, String outRootPath, String modelBuildingInfoPath,
                                            String libDir, boolean compiled) {

        System.out.println("Extracting methods... ");

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.init(modelBuildingInfoPath);
        File[] revisions = new File(srcRootPath).listFiles(File::isDirectory);
        sortRevisionDirectories(revisions);

        defects4jMap = new HashMap<>();
        int i = 0;
        for (File rev : revisions) {
            System.out.println("  Processing " + rev.toString() + " (" + ++i + "/" + revisions.length + ")... ");

            LinkedHashMap<String, String> revMethodsMap = new LinkedHashMap<>();

            //Extract Model Building Info
            int confID = Integer.parseInt(rev.getName());
            String srcDir = modelConfig.getSrcDir(confID);
            int complianceLvl = modelConfig.getComplianceLevel(confID);
            String sourcePath = rev.getAbsolutePath() + BUGGY_DIR + srcDir;

            // Build Spoon model
            System.out.println("    Building spoon model... ");
            if (compiled) {
                libDir = rev.getAbsolutePath() + BUGGY_DIR;
            }
            SpoonAPI spoon = SpoonConfig.buildModel(sourcePath, complianceLvl, libDir, compiled);

            // Generate methods
            System.out.println("    Generating methods... ");

            List<CtMethod> methods = spoon.getFactory()
                    .Package()
                    .getRootPackage()
                    .getElements(new TypeFilter<>(CtMethod.class));

            System.out.println("    Saving methods... ");
            for (CtMethod method : methods) {
                String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
                SourcePosition sp = method.getPosition();
                String body = sp.getCompilationUnit()
                        .getOriginalSourceCode()
                        .substring(sp.getSourceStart(), sp.getSourceEnd() + 1);

                revMethodsMap.put(signature, body);
            }
            String outDir = outRootPath + rev.getName() + BUGGY_DIR;
            defects4jMap.put(outDir, revMethodsMap);
            System.out.println("  done.");
        }

        System.out.println("done.");
    }

    public static LinkedHashMap<String, String> getRawMethodsMap() {
        return rawMethodsMap;
    }

    public static Map<String, LinkedHashMap<String, String>> getDefects4jMap() {
        return defects4jMap;
    }

    private static void sortRevisionDirectories(File[] revisions) {
        Arrays.sort(revisions, new Comparator<File>() {
            public int compare(File f1, File f2) {
                try {
                    int i1 = Integer.parseInt(f1.getName());
                    int i2 = Integer.parseInt(f2.getName());
                    return i1 - i2;
                } catch (NumberFormatException e) {
                    throw new AssertionError(e);
                }
            }
        });
    }
}
