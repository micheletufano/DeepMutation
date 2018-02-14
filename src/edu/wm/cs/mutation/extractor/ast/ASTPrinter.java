package edu.wm.cs.mutation.extractor.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

public class ASTPrinter {

	public static String printAST(List<ASTNode> leaves){
		String corpora = "";
		
		for(ASTNode node : leaves){
			corpora += extractNodeLexeme(node)+" ";
		}
		
		corpora = corpora.trim().replaceAll(" +", " ");
		
		return corpora;
	}
	
	public static String extractNodeLexeme(ASTNode node){
		String lexeme = "";
		
		//Check special case of literals
		if(node instanceof StringLiteral){
			lexeme = "<STRING> ";
		} else if(node instanceof CharacterLiteral){
			lexeme =  "<CHAR> ";
		} else if(node instanceof NumberLiteral){
			if(node.toString().contains(".")){
				lexeme =  "<FLOAT> ";
			} else {
				lexeme =  "<INT> ";
			}
		} else if(node instanceof MarkerAnnotation || getASTNodeType(node).toLowerCase().contains("annotation")){
			lexeme = "@"+extractLexeme(node)+" ";
		} else { //General case
			lexeme = extractLexeme(node)+" ";
		}
		
		return lexeme;
	}

	public static String extractLexeme(ASTNode node){
		String lexeme = node.toString();

		lexeme = lexeme.replace("{", "");
		lexeme = lexeme.replace("}", "");
		lexeme = lexeme.replace(";", "");
		lexeme = lexeme.replace(":", "");
		lexeme = lexeme.replace(",", "");
		lexeme = lexeme.replace("(", "");
		lexeme = lexeme.replace(")", "");
		lexeme = lexeme.replace("[", "");
		lexeme = lexeme.replace("]", "");
		lexeme = lexeme.replace(" ", "");
		lexeme = lexeme.replace("\n", "");
		
		return lexeme;
		
	}

	public static String getASTNodeType(ASTNode node){
		String fullType = ""+ ASTNode.nodeClassForType(node.getNodeType());

		String[] info = fullType.split("\\.");

		return info[info.length-1];
	}
}
