package edu.wm.cs.mutation.abstractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;


public class MethodAbstractor {
	 private static final String KEY_OUTPUT = "methods.key1";
	 private static final String SRC_OUTPUT = "methods.abstract";
	 
	 public static void generateAbsCode(Map<String, LinkedHashMap<String, String>> rawMethods) {
		 
		 System.out.println("Abstracting methods... ");

		 for (Map.Entry<String, LinkedHashMap<String, String>> entry : rawMethods.entrySet()) {
			 Map<String, String> revMethodMap = entry.getValue();
			 
			 String outPath = entry.getKey(); // output path for each revision
	         List<String> signatures = new ArrayList<>();
	         List<String> absMethods= new ArrayList<>();   
	         
	         System.out.println("Processing: "+outPath);
			 for (Entry<String, String> entry1 : revMethodMap.entrySet()) {
				 String signatrue = entry1.getKey();
				 String srcCode = entry1.getValue();
				 signatures.add(signatrue);
				 methodProcessor(signatrue, srcCode, absMethods);					 
			 }
			 
			 System.out.println("Writing to files: ");
			 try {
	                Files.write(Paths.get(outPath + KEY_OUTPUT), signatures);
	                Files.write(Paths.get(outPath + SRC_OUTPUT), absMethods);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
			 System.out.println("  done.");
		 }
		 System.out.println("done.");
	 }
	 private static void methodProcessor( String signatrue, String srcCode, List<String> absMethods) {
			//Parser
			MethodParser parser = new MethodParser();

			try {
				parser.parse(srcCode);
			} catch (Exception e) {
				System.err.println("Exception during parsing!");
			} catch(StackOverflowError e){
				System.err.println("StackOverflow during parsing!");
			}
			
			//Tokenizer
			MethodLexer tokenizer = new MethodLexer();

//			System.out.println("Types: "+parser.getTypes());
//			System.out.println("Methods: "+parser.getMethods());

			tokenizer.setTypes(parser.getTypes());
			tokenizer.setMethods(parser.getMethods());

			String afterTokenized = tokenizer.tokenize(srcCode);
//			System.out.println("Signiture: "+signatrue);
//			System.out.println("AfterTokenized: "+afterTokenized);
            
			// Add to result list for each revision
			absMethods.add(afterTokenized);
	 }
}
