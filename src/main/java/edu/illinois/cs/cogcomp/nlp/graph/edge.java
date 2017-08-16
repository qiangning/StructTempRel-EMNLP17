package edu.illinois.cs.cogcomp.nlp.graph;

/**
 * Created by qning2 on 11/19/16.
 */
public class edge {
    private vertex source;
    private vertex target;
    private String label;

    public vertex getSource() {
        return source;
    }

    public void setSource(vertex source) {
        this.source = source;
    }

    public vertex getTarget() {
        return target;
    }

    public void setTarget(vertex target) {
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public edge(vertex source, vertex target, String label) {

        this.source = source;
        this.target = target;
        this.label = label;
    }
}
