package edu.wm.cs.mutation.extractor.ast.visitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class NodeVisitor extends ASTVisitor {
	
	List<ASTNode> nodes = new ArrayList<ASTNode>();
	
	public void preVisit(ASTNode node){
		nodes.add(node);		
	}

	
	public List<ASTNode> getNodes(){
		return nodes;
	}
}
