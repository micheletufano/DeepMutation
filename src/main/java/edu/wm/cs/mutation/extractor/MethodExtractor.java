package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import edu.wm.cs.mutation.Consts;
import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeInformation;
import spoon.reflect.visitor.filter.TypeFilter;

public class MethodExtractor {

    public static final String BUGGY_DIR = "/b/";
    private static LinkedHashMap<String,String> rawMethodsMap = new LinkedHashMap();
    private static List<CtMethod> methods;

    public static void extractMethods(String projPath, String srcPath, String libPath,
                                      int complianceLvl, boolean compiled, String inputMethodsPath) {
        System.out.println("Extracting methods from " + projPath + "... ");

        // Read user-specified methods
        Set<String> inputMethods = null;
        if (inputMethodsPath != null) {
            System.out.println("  Reading specified methods from input file... ");
            inputMethods = readInputMethods(inputMethodsPath);
            System.out.println("  done.");
        }

        File project = new File(projPath);
        rawMethodsMap.clear();
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

        int userMethods = 0;
        int interfaceMethods = 0;
        for (CtMethod method : methods) {
            String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
            
            // keep methods matching the specified signatures   
            if (inputMethods != null && !inputMethods.contains(signature)) {
            	    continue;
            }
            if (inputMethods != null && inputMethods.contains(signature)) {
                userMethods++;
            }
            
            // filter out getters/setters when methods are not specified
         	if (inputMethods == null) {
         		String methodName = method.getSignature().split(" ")[1];
         		if (methodName.startsWith("set") || methodName.startsWith("get")) {
         			continue;
         		}
         	}
            
            // filter out methods in java interfaces
            if (((CtTypeInformation) method.getParent()).isInterface()) {
                interfaceMethods++;
            	    continue;
            }

            SourcePosition sp = method.getPosition();
            String body = sp.getCompilationUnit()
                    .getOriginalSourceCode()
                    .substring(sp.getSourceStart(), sp.getSourceEnd() + 1);

            rawMethodsMap.put(signature, body);
        }
        if (rawMethodsMap.size() == 0) {
            System.err.println("  ERROR: Could not extract any methods.");
        } else {
            if (inputMethods != null) {
                System.out.println("  Found " + userMethods + "/" + inputMethods.size() + " user-specified methods.");
                System.out.println("  Ignored " + interfaceMethods + " interface methods.");
            }
            System.out.println("  Extracted " + rawMethodsMap.size() + " methods.");
        }
        System.out.println("done.");
    }

    public static void extractMethods(Defects4JInput input, String libPath, boolean compiled, String inputMethodsPath) {
        extractMethods(input.getProjPath(), input.getSrcPath(), libPath, input.getComplianceLvl(), compiled, inputMethodsPath);
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

    public static void buildModel(String projPath, String srcPath, String libPath,
                                  int complianceLvl, boolean compiled) {
        System.out.println("Building spoon model... ");

        File project = new File(projPath);
        if (compiled) {
            libPath = project.getAbsolutePath() + "/";
        }
        SpoonAPI spoon = SpoonConfig.buildModel(srcPath, complianceLvl, libPath, compiled);

        methods = spoon.getFactory()
                .Package()
                .getRootPackage()
                .getElements(new TypeFilter<>(CtMethod.class));

        System.out.println("done.");
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

    public static void writeMethods(String outPath) {
        System.out.println("Writing extracted methods... ");

        if (rawMethodsMap == null) {
            System.err.println("  ERROR: cannot write null map");
            return;
        }

        List<String> signatures = new ArrayList<>(rawMethodsMap.keySet());
        List<String> bodies = new ArrayList<>(rawMethodsMap.values());

        try {
            Files.createDirectories(Paths.get(outPath));
            Files.write(Paths.get(outPath + Consts.METHODS + Consts.KEY_SUFFIX), signatures);
            Files.write(Paths.get(outPath + Consts.METHODS + Consts.SRC_SUFFIX), bodies);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done.");
    }

    private static HashSet<String> readInputMethods(String methodPath) {
        List<String> methods = null;
        try {
            methods = Files.readAllLines(Paths.get(methodPath));
        } catch (IOException e) {
            System.err.println("    ERROR: could not load specified methods from files: " + e.getMessage());
        }

        if (methods == null) {
            System.err.println("    ERROR: could not load specified methods from files");
            return null;
        }
        return new HashSet<>(methods);
    }

    public static LinkedHashMap<String, String> getRawMethodsMap() {
        return rawMethodsMap;
    }

    public static List<CtMethod> getMethods() {
        return methods;
    }
    
    public static void setMethods(List<CtMethod> methods) {
    	MethodExtractor.methods = methods;
    }

    public static void setRawMethodsMap(LinkedHashMap<String, String> rawMethodsMap) {
        MethodExtractor.rawMethodsMap = rawMethodsMap;
    }

}
