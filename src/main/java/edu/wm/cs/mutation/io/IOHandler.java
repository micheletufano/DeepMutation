package edu.wm.cs.mutation.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

public class IOHandler {

    private static final String FAILED_OUT = "failed.out";

    public static void writeFailedMutants(String outPath, Map<String, List<String>> modelsMap, List<String> modelPaths) {
        System.out.println("Writing failed mutants... ");
        if (modelsMap == null) {
            System.err.println("ERROR: cannot write null input map");
            return;
        }

        for (String modelPath : modelPaths) {
            File modelFile = new File(modelPath);
            String modelName = modelFile.getName();
            System.out.println("  Processing model " + modelName + "... ");

            List<String> failedMutants = modelsMap.get(modelName);
            if (failedMutants == null) {
                System.err.println("    WARNING: cannot write null list for model " + modelName);
                continue;
            }

            String path = outPath + modelName + File.separator;
            try {
                Files.write(Paths.get(path + FAILED_OUT), failedMutants);
            } catch (IOException e) {
                System.err.println("    ERROR: could not write failed mutants list for model " + modelName);
                e.printStackTrace();
            }
        }
    }

    public static void exportMutants(String projPath, String outPath, String mutantsPath) {
        System.out.println("Exporting mutants... ");

        File origProj = new File(projPath);
        File dir = new File(mutantsPath);
        File[] mutants = dir.listFiles();

        if (mutants == null) {
            System.err.println("  ERROR: " + mutantsPath + " is not a directory");
            return;
        }
        if (mutants.length == 0) {
            System.out.println("  ERROR: could not find any mutants");
            return;
        }

        for (File mutant : mutants) {
            if (!mutant.isFile() || !mutant.getName().endsWith(".java")) {
                continue;
            }
            String mutantID = mutant.getName().split("_")[0];
            File mutantProj = new File(outPath + origProj.getName() + mutantID);
            String mutantPath = mutant.getName().split("_")[1].replace("-", File.separator).replaceFirst(projPath,
                    mutantProj.getPath() + File.separator);

            try {
                FileUtils.copyDirectory(origProj, mutantProj);
                System.out.println("  Created " + mutantProj.getPath() + ".");
            } catch (IOException e) {
                System.err.println("  ERROR: could not copy project for mutant " + mutantID);
                e.printStackTrace();
                return;
            }

            try {
                Files.copy(mutant.toPath(), Paths.get(mutantPath), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("  ERROR: could not export mutant " + mutantID);
                try {
                    FileUtils.deleteDirectory(mutantProj);
                } catch (IOException ee) {
                    System.err.println("  ERROR: could not clean up failed project " + mutantProj.getPath());
                    ee.printStackTrace();
                }
                e.printStackTrace();
            }
        }
        System.out.println("done.");
    }

    public static String[] readRevsCSV(String filePath) {
    	
    	List<String> lines = readLines(filePath);

    	return lines.get(0).split(",");
    }
    
    public static List<String> listDirectoriesPaths(String dirPath){
    	
    	File[] directories = new File(dirPath).listFiles(File::isDirectory);	
    	List<String> paths = new ArrayList<>();
    	
    	for(File dir : directories) {
    		paths.add(dir.getAbsolutePath()+File.separator);
    	}
    	
    	return paths;
    }

    public static List<String> readLines(String filePath){
    	List<String> lines = null;
    	
    	try {
			lines = Files.readAllLines(Paths.get(filePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	return lines;
    }
    
    
    public static void setOutputStream(String logFilePath) {
		try {
			FileOutputStream log = new FileOutputStream(logFilePath);
			PrintStream out = new PrintStream(log);
			PrintStream err = new PrintStream(log);
			System.setOut(out);
			System.setErr(err);  
		}catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    public static void createDirectories(String dirPath) {
    	try {
			Files.createDirectories(Paths.get(dirPath));
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

}
