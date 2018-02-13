package edu.wm.cs.mutation.extractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtility {
	public static List<File> listJARFiles(String dirPath) {
		//Path Matcher for java files (recursive) 
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.jar");
		
		List<File> files = null;
		try {
			files = Files.walk(Paths.get(dirPath))
			.filter(Files::isRegularFile)
			.filter(p -> matcher.matches(p))
			.map(Path::toFile)
			.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return files;
	}
}
