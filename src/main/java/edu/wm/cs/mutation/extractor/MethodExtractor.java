package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.util.*;

import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public class MethodExtractor {

    public static final String BUGGY_DIR = "/b/";
    private static LinkedHashMap<String,String> rawMethodsMap;
    private static List<CtMethod> methods;

    public static void extractMethods(String projPath, String srcPath, String libPath,
                                      int complianceLvl, boolean compiled) {
        System.out.println("\nExtracting methods from " + projPath + "... ");

        File project = new File(projPath);
        rawMethodsMap = new LinkedHashMap<>();

        // Build Spoon model
        if (compiled) {
            libPath = project.getAbsolutePath() + "/";
        }
        SpoonAPI spoon = SpoonConfig.buildModel(srcPath, complianceLvl, libPath, compiled);

        // Generate methods
        methods = spoon.getFactory()
                .Package()
                .getRootPackage()
                .getElements(new TypeFilter<>(CtMethod.class));

        for (CtMethod method : methods) {
            String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();

            // filter out getters/setters
            String methodName = method.getSignature().split(" ")[1];
            if (methodName.startsWith("set") || methodName.startsWith("get")) {
                continue;
            }

            SourcePosition sp = method.getPosition();
            String body = sp.getCompilationUnit()
                    .getOriginalSourceCode()
                    .substring(sp.getSourceStart(), sp.getSourceEnd() + 1);

            rawMethodsMap.put(signature, body);
        }
        System.out.println("done.");
    }

    public static void extractMethods(Defects4JInput input, String libPath, boolean compiled) {
        extractMethods(input.getProjPath(), input.getSrcPath(), libPath, input.getComplianceLvl(), compiled);
    }

    public static List<Defects4JInput> generateDefect4JInputs(String projBasePath, String outBasePath, String modelConfigPath) {

        ModelConfig modelConfig = new ModelConfig();
        modelConfig.init(modelConfigPath);
        File[] revisions = new File(projBasePath).listFiles(File::isDirectory);
        sortRevisionDirectories(revisions);

        List<Defects4JInput> inputs = new ArrayList<>();
        for (File rev : revisions) {
            int confID = Integer.parseInt(rev.getName());

            String projPath = projBasePath + rev.getName() + MethodExtractor.BUGGY_DIR;
            String srcPath = projPath + modelConfig.getSrcPath(confID);
            String outPath = outBasePath + rev.getName() + MethodExtractor.BUGGY_DIR;
            int complianceLvl = modelConfig.getComplianceLevel(confID);

            inputs.add(new Defects4JInput(projPath, srcPath, outPath, complianceLvl));
        }

        return inputs;
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

    public static LinkedHashMap<String, String> getRawMethodsMap() {
        return rawMethodsMap;
    }

    public static List<CtMethod> getMethods() {
        return methods;
    }
}
