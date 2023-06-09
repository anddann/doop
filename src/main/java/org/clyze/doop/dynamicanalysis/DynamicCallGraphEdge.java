package org.clyze.doop.dynamicanalysis;

import com.sun.tools.hat.internal.model.StackFrame;
import com.sun.tools.hat.internal.model.StackTrace;
import org.clyze.doop.common.Database;
import org.clyze.doop.common.PredicateFile;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by neville on 22/02/2017.
 */
public class DynamicCallGraphEdge implements DynamicFact {

    private final String contextFrom;
    private final String contextTo;
    private final String methodFrom;
    private final String lineNumberFrom;

    private final String methodTo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicCallGraphEdge that = (DynamicCallGraphEdge) o;

        if (!getMethodFrom().equals(that.getMethodFrom())) return false;
        if (!getLineNumberFrom().equals(that.getLineNumberFrom())) return false;
        return getMethodTo().equals(that.getMethodTo());
    }

    @Override
    public int hashCode() {
        int result = getMethodFrom().hashCode();
        result = 31 * result + getLineNumberFrom().hashCode();
        result = 31 * result + getMethodTo().hashCode();
        return result;
    }

    public DynamicCallGraphEdge(String methodFrom, String lineNumberFrom, String methodTo, String contextFrom, String contextTo) {
        this.contextFrom = contextFrom;
        this.contextTo = contextTo;

        this.methodFrom = methodFrom;
        this.lineNumberFrom = DumpParsingUtil.parseLineNumber(lineNumberFrom);
        this.methodTo = methodTo;
    }

    public static Collection<DynamicCallGraphEdge> fromStackTrace(StackTrace trace) {
        if (trace == null || trace.getFrames() == null)
            return new ArrayList<>();
        StackFrame[] frames = trace.getFrames();
        ArrayList<DynamicCallGraphEdge> edges = new ArrayList<>(frames.length - 1);
        for (int i = 1 ; i < frames.length; i ++) {
            if (frames[i].getClassName().startsWith("Instrumentation") ||
                    frames[i].getClassName().startsWith("javassist"))
                return edges;
        }
        for (int i = 1 ; i < frames.length; i ++) {
            edges.add(new DynamicCallGraphEdge(
                    DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame(frames[i]),
                    frames[i].getLineNumber(),
                    DumpParsingUtil.fullyQualifiedMethodSignatureFromFrame(frames[i-1]),
                    ContextInsensitive.get().getRepresentation(), ContextInsensitive.get().getRepresentation())
            );
        }

        return edges;
    }

    @Override
    public String toString() {
        return "DynamicCallGraphEdge{" +
                "methodFrom='" + methodFrom + '\'' +
                ", lineNumberFrom='" + lineNumberFrom + '\'' +
                ", methodTo='" + methodTo + '\'' +
                '}';
    }

    public String getMethodFrom() {

        return methodFrom;
    }

    public String getLineNumberFrom() {
        return lineNumberFrom;
    }

    public String getMethodTo() {
        return methodTo;
    }


    @Override
    public void write_fact(Database db) {
        db.add(PredicateFile.DYNAMIC_CALL_GRAPH_EDGE, methodFrom, lineNumberFrom, methodTo, contextFrom, contextTo);
    }
}
