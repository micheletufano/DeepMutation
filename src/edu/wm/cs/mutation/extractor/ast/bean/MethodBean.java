package edu.wm.cs.mutation.extractor.ast.bean;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class MethodBean {
	
	private MethodDeclaration methodDeclaration;
	private AbstractTypeDeclaration type;
	
	
	public MethodBean(MethodDeclaration methodDeclaration, AbstractTypeDeclaration type){
		this.methodDeclaration = methodDeclaration;
		this.type = type;
	}
	
	
	public MethodDeclaration getMethodDeclaration() {
		return methodDeclaration;
	}
	
	public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
		this.methodDeclaration = methodDeclaration;
	}
	
	public AbstractTypeDeclaration getType() {
		return type;
	}
	
	public void setType(AbstractTypeDeclaration type) {
		this.type = type;
	}
	
}
