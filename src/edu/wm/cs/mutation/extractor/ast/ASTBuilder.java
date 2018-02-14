package edu.wm.cs.mutation.extractor.ast;

import edu.wm.cs.mutation.extractor.ast.visitor.NodeVisitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ASTBuilder {

	public static String formatCode(String srcCode, int complianceLvl) {
		//Create dummy class containing the executable
		String sourceStart = "class DUMMY {\n";
		String sourceEnd = "\n}";
		String src = sourceStart + srcCode + sourceEnd;

		//Extract Compilation Unit
		final CompilationUnit cu = ASTBuilder.extractCompilationUnitFromSrc(src, complianceLvl);

		//Extract AST nodes
		List<ASTNode> nodes = ASTBuilder.extractNodes(cu);

		//Extract AST leaf nodes
		List<ASTNode> leaves = ASTBuilder.extractLeaves(nodes);

		//Print AST
		String formatted = ASTPrinter.printAST(leaves);
		formatted = formatted.substring(5).trim();

		return formatted;
	}


	/**
	 * Extracts all the ASTNodes from the subtree rooted in the argument node
	 * @param root : the node which represents the root of the subtree
	 * @return List of ASTNodes in the subtree
	 */
	public static List<ASTNode> extractNodes(ASTNode root){
		NodeVisitor visitor = new NodeVisitor();
		root.accept(visitor);

		return visitor.getNodes();
	}


	public static List<ASTNode> extractLeaves(List<ASTNode> nodes){
		List<ASTNode> leaves = new ArrayList<ASTNode>(nodes);

		//Remove nodes which are parent of other nodes
		for(ASTNode node : nodes){
			leaves.remove(node.getParent());
		}

		//Remove trivial leaves
		leaves = removeTrivialLeaves(leaves);

		return leaves;
	}


	private static List<ASTNode> removeTrivialLeaves(List<ASTNode> leaves){
		List<ASTNode> toRemove = new ArrayList<ASTNode>();

		//Identify
		for(ASTNode node : leaves){
			if(		   node instanceof EmptyStatement
					|| node instanceof Javadoc
					|| node instanceof CompilationUnit
					|| node instanceof AnonymousClassDeclaration
					|| node instanceof ArrayInitializer
					|| node instanceof Block){

				toRemove.add(node);
			}
		}

		//Remove
		for(ASTNode node : toRemove){
			leaves.remove(node);
		}

		return leaves;
	}

	public static CompilationUnit extractCompilationUnitFromSrc(String src, int javaVersion){

		//Create Parser
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(src.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		//Select Java Version
		String javaCoreJavaVersion = getJavaVersion(javaVersion);

		//Set Java Version
		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, javaCoreJavaVersion);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, javaCoreJavaVersion);
		options.put(JavaCore.COMPILER_SOURCE, javaCoreJavaVersion);
		parser.setCompilerOptions(options);

		//Extract Compilation Unit
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

		return cu;
	}

	private static String getJavaVersion(int javaVersion) {
		String javaCoreJavaVersion = "";
		if(javaVersion == 1){
			javaCoreJavaVersion = JavaCore.VERSION_1_1;
		} else if(javaVersion == 2){
			javaCoreJavaVersion = JavaCore.VERSION_1_2;
		} else if(javaVersion == 3){
			javaCoreJavaVersion = JavaCore.VERSION_1_3;
		} else if(javaVersion == 4){
			javaCoreJavaVersion = JavaCore.VERSION_1_4;
		} else if(javaVersion == 5){
			javaCoreJavaVersion = JavaCore.VERSION_1_5;
		} else if(javaVersion == 6){
			javaCoreJavaVersion = JavaCore.VERSION_1_6;
		} else if(javaVersion == 7){
			javaCoreJavaVersion = JavaCore.VERSION_1_7;
		} else if(javaVersion == 8){
			javaCoreJavaVersion = JavaCore.VERSION_1_8;
		}
		return javaCoreJavaVersion;
	}

}
