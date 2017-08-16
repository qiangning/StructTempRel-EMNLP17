package edu.uw.cs.lil.uwtime.corrections;

public class MentionCorrection {
    final private String value, type, mod;
    final private int start, end;

    public MentionCorrection (String value, String type, String mod, int start, int end) {
        this.value = value;
        this.type = type;
        this.mod = mod.equals("NONE") ? null : mod;
        this.start = start;
        this.end = end;
    }

    public String toString() {
        return String.format("(%s,%s,%s)[%d->%d]", value, type, mod, start, end);
    }
    
    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getMod() {
        return mod;
    }
    
    public boolean useSpan() {
        return start >= 0 && end >= 0;
    }

    public int getStartChar() {
        return start;
    }
    
    public int getEndChar() {
        return end;
    }
}
