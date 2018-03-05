package edu.wm.cs.mutation.mutator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import edu.wm.cs.mutation.extractor.ModelConfig;
import edu.wm.cs.mutation.extractor.SpoonConfig;

import spoon.SpoonAPI;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;

public class MutantGenerator {

	private static final String PRED_DIR = "pred/";
	private static final String KEY_INPUT = "methods.key";
	private static final String MUTANT_INPUT = "methods.source";
	private static final String MUTANT_OUTPUT = "mutants/";
	private static final String PATH_OUTPUT = "mutant.path";

	public static void generateMutants(String srcRootPath, String outRootPath, String revID,
			String modelBuildingInfoPath, String libDir, boolean compiled) {

		System.out.println("Generate mutants... ");

		ModelConfig modelConfig = new ModelConfig();
		modelConfig.init(modelBuildingInfoPath);
		int confID = Integer.parseInt(revID);
		String srcDir = modelConfig.getSrcDir(confID);
		int complianceLvl = modelConfig.getComplianceLevel(confID);
		String srcPath = srcRootPath + srcDir;
		String predPath = outRootPath + PRED_DIR;

		// Build Spoon model
		System.out.println("  Building spoon model... ");
		if (compiled) {
			libDir = srcRootPath;
		}
		SpoonAPI spoon = SpoonConfig.buildModel(srcPath, complianceLvl, libDir, compiled);

		// Read signature and mutants from all model directories
		System.out.println("  Reading mutants... ");
		File[] models = new File(predPath).listFiles(File::isDirectory);
		for (File model : models) {
			System.out.println("    Processing " + model.toString());
			List<String> signatures = null;
			List<String> mutants = null;
			String modelPath = model.getPath() + File.separator;
			List<String> mutantPath = new ArrayList<>();
			String mutantOut = modelPath + MUTANT_OUTPUT;
			if (!Files.exists(Paths.get(mutantOut)))
				try {
					Files.createDirectories(Paths.get(mutantOut));
				} catch (IOException e1) {
					System.out.println("Error in creating mutant directory: " + e1.getMessage());
				}

			try {
				signatures = Files.readAllLines(Paths.get(modelPath + KEY_INPUT));
				mutants = Files.readAllLines(Paths.get(modelPath + MUTANT_INPUT));
			} catch (IOException e) {
				e.printStackTrace();
			}
			int counter = 1; // counter for mutated file
			int index = 0;
			Map<String, String> mutantMap = new HashMap<>();
			for (String sign : signatures) {
				mutantMap.put(sign, mutants.get(index++));
			}

			// replace raw methods with mutant
			List<CtMethod> methods = spoon.getFactory().Package().getRootPackage()
					.getElements(new TypeFilter<>(CtMethod.class));

			for (CtMethod method : methods) {
				String signature = method.getParent(CtType.class).getQualifiedName() + "#" + method.getSignature();
				if (!mutantMap.containsKey(signature))
					continue;

				SourcePosition sp = method.getPosition();
				String original = sp.getCompilationUnit().getOriginalSourceCode();

				// get path_to_src_path_to_file, file name and method name from srcPath and
				// signature
				String pathToFile = signature.split("#")[0];
				String methodName = signature.split("#")[1];
				String[] subPath = pathToFile.split("\\.");
				String fileName = subPath[subPath.length - 1] + ".java_" + counter++;
				String absPath = srcPath + pathToFile.replace(".", "/") + ".java" + "#" + methodName + "#"+ fileName;

				// construct and format mutated class
				StringBuilder sb = new StringBuilder();
				sb.append(original.substring(0, sp.getSourceStart()));
				sb.append(mutantMap.get(signature));
				sb.append(original.substring(sp.getSourceEnd() + 1));

				String formattedSrc = null;
				try {
					formattedSrc = new Formatter().formatSource(sb.toString());
					// write mutated class
					System.out.println("Writeing file: "+fileName);

					Files.write(Paths.get(mutantOut + fileName), formattedSrc.getBytes());
					mutantPath.add(absPath);

				} catch (FormatterException e) {
					System.out.println("Error in formating code: " + e.getMessage());
				} catch (IOException e) {
					System.out.println("Error in writing mutated class: " + e.getMessage());
				}
			}
			// write path file (original Path# mutated method# mutated file name_counter)
			try {
				Files.write(Paths.get(modelPath + PATH_OUTPUT), mutantPath);
			} catch (IOException e) {
				System.out.println("Error in writing path file: " + e.getMessage());
			}
		}
		System.out.println("done.");
	}

}
