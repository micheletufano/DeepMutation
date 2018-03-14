package edu.wm.cs.mutation.tester;

import edu.wm.cs.mutation.io.IOHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MutantTester {

    private static final String MUTANT_DIR_SUFFIX = "Mutant";
    private static final String COPY_SUFFIX = ".bak";

    public static void testMutants(String outPath, String projPath, String srcPath,
                                   Map<String, LinkedHashMap<String,String>> mutantsMap, List<String> modelPaths) {

        System.out.println("Testing " + projPath + "... ");

        System.out.println("  Copying project... ");
        String projDir = null, mutantProjDir = null;
        File srcDir = null, destDir = null;
        try {
            srcDir = new File(projPath);
            destDir = new File(new File(projPath).getParent() + "/" + srcDir.getName() + MUTANT_DIR_SUFFIX);
            FileUtils.copyDirectory(srcDir, destDir);

            projDir = srcDir.getName();
            mutantProjDir = destDir.getName();
        } catch (IOException e) {
            System.err.println("    ERROR: could not copy project");
            return;
        }
        System.out.println("  done.");

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            // Get mutant signatures
            List<String> signatures = new ArrayList<>(mutantsMap.get(modelName).keySet());

            // Get mutant files
            String mutantsPath = outPath + modelName + "/" + IOHandler.MUTANT_DIR;
            File[] mutantFiles = new File(mutantsPath).listFiles();
            if (mutantFiles == null) {
                System.err.println("  WARNING: could not find any mutants");
                continue;
            }
            Arrays.sort(mutantFiles);

            // Test each mutant
            for (File mutantFile : mutantFiles) {
                int mutantID = Integer.parseInt(mutantFile.getName().split("_")[0]);
                File origFile = new File(mutantFile.getName()
                        .split("_")[1]
                        .replace("-","/")
                        .replaceFirst(projDir, mutantProjDir));
                File copyFile = new File(origFile.getPath() + COPY_SUFFIX);

                System.out.println("    Testing mutant " + mutantID + ": " + signatures.get(mutantID) + "... ");

                // Copy original file
                try {
                    FileUtils.copyFile(origFile, copyFile);
                } catch (IOException e) {
                    System.err.println("    WARNING: could not copy original file");
                    e.printStackTrace();
                    continue;
                }

                // Replace original file with mutant file
                try {
                    FileUtils.copyFile(mutantFile, origFile);
                } catch (IOException e) {
                    System.err.println("    WARNING: could not copy mutant file");
                    continue;
                }

                // Run test

                // Replace mutant file with original file
                try {
                    FileUtils.copyFile(copyFile, origFile);
                    FileUtils.forceDelete(copyFile);
                } catch (IOException e) {
                    System.err.println("    WARNING: could not restore original file");
                }
            }

            System.out.println("  done.");
        }

        try {
            FileUtils.deleteDirectory(destDir);
        } catch (IOException e) {
            System.err.println("  WARNING: could not clean up directory");
        }

        System.out.println("done.");
    }
}
