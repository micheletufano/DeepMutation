package edu.wm.cs.mutation.abstractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.wm.cs.mutation.abstractor.lexer.MethodLexer;
import edu.wm.cs.mutation.abstractor.parser.MethodParser;


public class MethodAbstractor {
	 private static Map <String, String>  abstractMethods;
	 public static void generateAbsCode(Map<String, Map<String, String>> rawMethodsMap) {
		 abstractMethods = new HashMap<>();
		 for (Map.Entry<String, Map<String, String>> entry : rawMethodsMap.entrySet()) {
			 Map<String, String> revMethodMap = entry.getValue();
			 for (Entry<String, String> entry1 : revMethodMap.entrySet()) {
				 String signatrue = entry1.getKey();
				 String srcCode = entry1.getValue();
				 methodProcessor(signatrue, srcCode);			 
			 }
		 }
	 }
	 private static void methodProcessor( String signatrue, String srcCode) {
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

			System.out.println("Types: "+parser.getTypes());
			System.out.println("Methods: "+parser.getMethods());

			tokenizer.setTypes(parser.getTypes());
			tokenizer.setMethods(parser.getMethods());

			String afterTokenized = tokenizer.tokenize(srcCode);
			System.out.println("Signiture: "+signatrue);
			System.out.println("AfterTokenized: "+afterTokenized);
            
			//Write to the result map
			abstractMethods.put(signatrue, afterTokenized);
	 }
}
