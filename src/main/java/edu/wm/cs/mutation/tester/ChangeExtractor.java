package edu.wm.cs.mutation.tester;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.ITree;

import gumtree.spoon.AstComparator;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.DiffImpl;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.export.ChangeExporter;
import gumtree.spoon.pair.MethodPair;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

public class ChangeExtractor {

    private DiffImpl diff;
    private CtMethod mutatedMethod;
    private MappingStore mapping;
    private Map<MethodPair, List<Operation>> changedMethods;
    private boolean debug = false;

    public Map<MethodPair, List<Operation>> extractChanges(String srcBefore, String srcAfter, CtMethod curMethod) {

        // Compare two code snippts
        AstComparator comp = new AstComparator();
        try {
            mutatedMethod = curMethod;
            diff = (DiffImpl) comp.compare(srcBefore, srcAfter);
        } catch (Exception e) {
            System.err.println("ERROR while computing the DIFF" + e.getMessage());
            return null;
        } catch (StackOverflowError t) {
            System.err.println("ERROR while computing the DIFF (StackOverflow)");
            return null;
        }

        setMappings();
        changedMethods = extractChangedMethods();

        return changedMethods;
    }

    public Map<MethodPair, List<Operation>> extractChangedMethods() {

        Map<MethodPair, List<Operation>> changedMethods = new HashMap<>();
        List<Operation> operations = diff.getAllOperations();

        for (Operation op : operations) {

            if (problematicOpration(op)) {
                continue;
            }

            MethodPair methodPair = extractMethodPair(op);

            if (methodPair == null) {
                if (debug) {
                    System.out.println("Null MethodPair. Skipping operation.");
                }
                continue;
            }

            List<Operation> methodPairOperations = new ArrayList<>();
            if (changedMethods.containsKey(methodPair)) {
                methodPairOperations = changedMethods.get(methodPair);
            }
            methodPairOperations.add(op);

            changedMethods.put(methodPair, methodPairOperations);
        }

        return changedMethods;

    }

    private boolean problematicOpration(Operation op) {
        CtElement element = (CtElement) op.getAction().getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

        if (element == null) {
            return true;
        }

        return false;
    }

    private MethodPair extractMethodPair(Operation operation) {

        CtMethod methodBefore = extractMethodBefore(operation);
        CtMethod methodAfter = extractMethodAfter(operation);

        if (methodBefore == null || methodAfter == null) {
            if (debug) {
                System.out.println("Null Method: " + (methodBefore == null) + "-" + (methodAfter == null));
            }
            return null;
        }
        String curSignature = mutatedMethod.getSignature();
        if (!methodBefore.getSignature().equals(curSignature) && !methodAfter.getSignature().equals(curSignature)) {
            if (debug) {
                System.out.println("Irrelevant Change");
            }
            return null;
        }

        MethodPair methodPair = new MethodPair(methodBefore, methodAfter);

        return methodPair;
    }

    private CtMethod extractMethodBefore(Operation operation) {

        Action action = operation.getAction();
        CtElement element = null;
        if (debug) {
            System.out.println(ChangeExporter.toStringAction(action));
        }

        if (action instanceof Insert) {
            element = getFirstMappedElement(action, true);
        } else if (action instanceof Delete) {
            element = operation.getNode();
        } else if (action instanceof Update) {
            element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        } else if (action instanceof Move) {
            element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
        }

        return retrieveMethod(element);
    }

    private CtMethod extractMethodAfter(Operation operation) {

        Action action = operation.getAction();
        CtElement element = null;

        if (action instanceof Insert) {
            element = operation.getNode();
        } else if (action instanceof Delete) {
            element = getFirstMappedElement(action, false);
        } else if (action instanceof Update) {
            element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
        } else if (action instanceof Move) {
            element = (CtElement) action.getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
        }

        return retrieveMethod(element);
    }

    private CtElement getFirstMappedElement(Action action, boolean src) {

        for (ITree parent : action.getNode().getParents()) {
            if (parent != null) {
                ITree mapped = null;
                if (src) {
                    mapped = mapping.getSrc(parent);
                } else {
                    mapped = mapping.getDst(parent);
                }

                if (mapped != null) {
                    CtElement el = (CtElement) mapped.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                    return el;
                }
            }
        }

        return null;
    }

    private void setMappings() {
        Field f = null;
        try {
            f = diff.getClass().getDeclaredField("_mappingsComp");
            f.setAccessible(true);
            mapping = (MappingStore) f.get(diff);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static CtMethod retrieveMethod(CtElement element) {

        if (element == null) {
            return null;
        }

        CtMethod method = null;
        if (element instanceof CtMethod) {
            method = (CtMethod) element;
        } else {
            method = element.getParent(CtMethod.class);
        }

        return method;
    }

}
