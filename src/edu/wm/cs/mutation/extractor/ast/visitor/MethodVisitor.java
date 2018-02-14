package edu.wm.cs.mutation.extractor.ast.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class MethodVisitor extends ASTVisitor {

	private List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	private boolean filterBySize = false;
	private int minLen;
	private int maxLen;

	@Override 
	public boolean visit(MethodDeclaration node) {
		
		//Filter by lines of code
		if(filterBySize){
			String code = node.toString();
			int loc = code.split(System.getProperty("line.separator")).length;

			if(loc > minLen && loc < maxLen){
				methods.add(node); 
			}
		} else{
			//Filtering disabled
			methods.add(node); 
		}
		return super.visit(node);
	}

	public List<MethodDeclaration> getMethods(){
		return methods;
	}

	public void setFilterBySize(boolean filterBySize) {
		this.filterBySize = filterBySize;
	}

	public void setMinLen(int minLen) {
		this.minLen = minLen;
	}

	public void setMaxLen(int maxLen) {
		this.maxLen = maxLen;
	}

}

