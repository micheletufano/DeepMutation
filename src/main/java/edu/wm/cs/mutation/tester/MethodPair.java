package edu.wm.cs.mutation.tester;

import spoon.reflect.declaration.CtMethod;

public class MethodPair {

    private CtMethod methodBefore;
    private CtMethod methodAfter;


    public MethodPair(CtMethod methodBefore, CtMethod methodAfter) {
        this.methodBefore = methodBefore;
        this.methodAfter = methodAfter;
    }

    public CtMethod getMethodBefore() {
        return methodBefore;
    }

    public void setMethodBefore(CtMethod methodBefore) {
        this.methodBefore = methodBefore;
    }

    public CtMethod getMethodAfter() {
        return methodAfter;
    }

    public void setMethodAfter(CtMethod methodAfter) {
        this.methodAfter = methodAfter;
    }


    @Override
    public int hashCode() {
        String signature = methodBefore.getSignature() + methodAfter.getSignature();

        return signature.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        MethodPair m = (MethodPair) obj;

        if (m.getMethodBefore().equals(methodBefore) && m.getMethodAfter().equals(methodAfter)) {
            return true;
        }

        return false;
    }

}
