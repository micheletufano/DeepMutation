package edu.wm.cs.mutation.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;

import gumtree.spoon.pair.MethodPair;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

public class ChangeExporter {

	private static final String METHOD_BEFORE = "original.java";
	private static final String METHOD_AFTER = "changed.java";
	private static final String OPERATIONS = "operations.txt";

	private Map<MethodPair, List<Operation>> changedMethods;

	public ChangeExporter(Map<MethodPair, List<Operation>> changedMethods) {
		this.changedMethods = changedMethods;
	}

	public void exportChanges(String outDir) {

		int id = 0;
		for (Entry<MethodPair, List<Operation>> e : changedMethods.entrySet()) {
			// Create directory
			String out = outDir + "_change_" + id + File.separator;
			createDir(out);
			id++;

			exportMethodPair(e.getKey(), out);
			exportOperations(e.getValue(), out);
		}

	}

	private void createDir(String out) {
		try {
			Files.createDirectories(Paths.get(out));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private static void exportMethodPair(MethodPair methodPair, String out) {
		exportMethod(methodPair.getMethodBefore(), out + METHOD_BEFORE);
		exportMethod(methodPair.getMethodAfter(), out + METHOD_AFTER);
	}

	private static void exportMethod(CtMethod method, String out) {
		try {
			Files.write(Paths.get(out), method.toString().getBytes());
		} catch (Exception e) {
			System.err.println("ERROR! Cennot print method: " + method.getSignature());
		}
	}

	private static void exportOperations(List<Operation> operations, String out) {

		out = out + OPERATIONS;
		List<String> printedOperations = new ArrayList<>();

		for (Operation op : operations) {
			printedOperations.add(toStringAction(op.getAction()));
		}

		try {
			Files.write(Paths.get(out), printedOperations);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String toStringAction(Action action) {
		StringBuilder stringBuilder = new StringBuilder();

		// Modified element
		CtElement element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

		// Action performed
		stringBuilder.append(action.getClass().getSimpleName());

		// Element node type
		String nodeType = element.getClass().getSimpleName();
		nodeType = nodeType.substring(2, nodeType.length() - 4);
		stringBuilder.append(" ").append(nodeType);

		// Action position
		String parentType = element.getParent().getClass().getSimpleName();
		parentType = parentType.substring(2, parentType.length() - 4);
		String position = "";

		if (action instanceof Move) {
			CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			position = " from " + parentType;
			position += " to " + elementDest.getClass().getSimpleName();
		} else {
			position = " at " + parentType;
		}
		stringBuilder.append(position);

		return stringBuilder.toString();
	}

	public static String toStringActionOriginal(Action action) {
		String newline = System.getProperty("line.separator");
		StringBuilder stringBuilder = new StringBuilder();

		CtElement element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
		// action name
		stringBuilder.append(action.getClass().getSimpleName());

		// node type
		String nodeType = element.getClass().getSimpleName();
		nodeType = nodeType.substring(2, nodeType.length() - 4);
		stringBuilder.append(" ").append(nodeType);

		// action position
		CtElement parent = element;
		while (parent.getParent() != null && !(parent.getParent() instanceof CtPackage)) {
			parent = parent.getParent();
		}
		String position = " at ";
		if (parent instanceof CtType) {
			position += ((CtType) parent).getQualifiedName();
		}
		if (element.getPosition() != null) {
			position += ":" + element.getPosition().getLine();
		}
		if (action instanceof Move) {
			CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			position = " from " + element.getParent(CtClass.class).getQualifiedName() + ":"
					+ element.getPosition().getLine();
			position += " to " + elementDest.getParent(CtClass.class).getQualifiedName() + ":"
					+ elementDest.getPosition().getLine();
		}
		stringBuilder.append(position).append(newline);

		// code change
		String label = element.toString();
		if (action instanceof Update) {
			CtElement elementDest = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
			label += " to " + elementDest.toString();
		}
		String[] split = label.split(newline);
		for (String s : split) {
			stringBuilder.append("\t").append(s).append(newline);
		}
		return stringBuilder.toString();
	}

}
