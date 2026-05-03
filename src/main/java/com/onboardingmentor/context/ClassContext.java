package com.onboardingmentor.context;

import java.util.List;

public class ClassContext {
    private final String className;
    private final String source;
    private final List<String> callers;
    private final List<String> callees;
    private final List<String> annotations;
    private final List<String> fields;

    public ClassContext(String className, String source, List<String> callers, List<String> callees, List<String> annotations, List<String> fields) {
        this.className = className;
        this.source = source;
        this.callers = callers;
        this.callees = callees;
        this.annotations = annotations;
        this.fields = fields;
    }

    public String getClassName() { return className; }
    public String getSource() { return source; }
    public List<String> getCallers() { return callers; }
    public List<String> getCallees() { return callees; }
    public List<String> getAnnotations() { return annotations; }
    public List<String> getFields() { return fields; }

    @Override
    public String toString() {
        return "ClassContext{" +
                "className='" + className + '\'' +
                ", callers=" + callers +
                ", callees=" + callees +
                ", annotations=" + annotations +
                ", fields=" + fields +
                ", source(length)=" + (source != null ? source.length() : 0) +
                '}';
    }
}
