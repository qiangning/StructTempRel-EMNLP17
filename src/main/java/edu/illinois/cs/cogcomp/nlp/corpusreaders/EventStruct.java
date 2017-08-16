package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.uw.cs.lil.uwtime.chunking.chunks.EventChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qning2 on 11/28/16.
 */
public class EventStruct {
    protected String eventId;
    protected String eventType;
    protected List<String> argNames = null;
    protected List<String> argTexts = null;
    protected List<IntPair> argCharOffsets = null;
    protected List<IntPair> argWordOffsets = null;
    protected String primaryTriggerText;
    protected IntPair primaryTriggerCharOffset;
    protected IntPair primaryTriggerWordOffset;

    /**
     *
     */
    public EventStruct(EventChunk ec, TextAnnotation ta){
        eventId = String.valueOf(ec.getEiid());
        eventType = ec.getAspect();
        argNames = new ArrayList<String>();
        argTexts = new ArrayList<String>();
        argCharOffsets = new ArrayList<IntPair>();
        argWordOffsets = new ArrayList<IntPair>();
        primaryTriggerText = ec.getText();
        primaryTriggerCharOffset = new IntPair(ec.getCharStart(),ec.getCharEnd());
        this.primaryTriggerWordOffset = new IntPair(-1, -1);

        /*Get word offset from textannotation*/
        IntPair tmp = new IntPair(-1, -1);
        tmp.setFirst(ta.getTokenIdFromCharacterOffset(ec.getCharStart()));
        tmp.setSecond(ta.getTokenIdFromCharacterOffset(ec.getCharEnd() - 1) + 1);
        setPrimaryTriggerWordOffset(tmp);
    }
    public EventStruct(String eventId) {
        this.eventId = eventId;
        this.eventType = "";
        this.argNames = new ArrayList<String>();
        this.argTexts = new ArrayList<String>();
        this.argCharOffsets = new ArrayList<IntPair>();
        this.argWordOffsets = new ArrayList<IntPair>();
        this.primaryTriggerText = "";
        this.primaryTriggerCharOffset = new IntPair(-1, -1);
        this.primaryTriggerWordOffset = new IntPair(-1, -1);
    }

    /**
     * @param eventType
     *            the eventType to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setPrimaryTrigger(String primaryTriggerText,
                                  IntPair primaryTriggerCharOffset) {
        this.primaryTriggerText = primaryTriggerText;
        this.primaryTriggerCharOffset = new IntPair(
                primaryTriggerCharOffset.getFirst(),
                primaryTriggerCharOffset.getSecond());
    }

    public void addArgument(String name, String text, IntPair charOffset) {
        this.argNames.add(name);
        this.argTexts.add(text);
        this.argCharOffsets.add(new IntPair(charOffset.getFirst(), charOffset
                .getSecond()));
    }

    public void addArgWordOffset(IntPair wordOffset) {
        this.argWordOffsets.add(new IntPair(wordOffset.getFirst(), wordOffset
                .getSecond()));
    }

    /**
     * @return the eventId
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return the primaryTriggerText
     */
    public String getPrimaryTriggerText() {
        return primaryTriggerText;
    }

    /**
     * @return the primaryTriggerCharOffset
     */
    public IntPair getPrimaryTriggerCharOffset() {
        return primaryTriggerCharOffset;
    }

    /**
     * @param primaryTriggerWordOffset
     *            the primaryTriggerWordOffset to set
     */
    public void setPrimaryTriggerWordOffset(IntPair primaryTriggerWordOffset) {
        this.primaryTriggerWordOffset = primaryTriggerWordOffset;
    }

    /**
     * @return the primaryTriggerWordOffset
     */
    public IntPair getPrimaryTriggerWordOffset() {
        return primaryTriggerWordOffset;
    }

    /**
     * @return the argNames
     */
    public List<String> getArgNames() {
        return argNames;
    }

    /**
     * @return the argTexts
     */
    public List<String> getArgTexts() {
        return argTexts;
    }

    /**
     * @return the argCharOffsets
     */
    public List<IntPair> getArgCharOffsets() {
        return argCharOffsets;
    }

    /**
     * @param argWordOffsets
     *            the argWordOffsets to set
     */
    public void setArgWordOffsets(List<IntPair> argWordOffsets) {
        this.argWordOffsets = argWordOffsets;
    }

    /**
     * @return the argWordOffsets
     */
    public List<IntPair> getArgWordOffsets() {
        return argWordOffsets;
    }

    /**
     * @return the eventType
     */
    public String getEventType() {
        return eventType;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer("Event: ");
        buff.append(eventType);
        buff.append(" (id=" + eventId + ")\n");
        buff.append("\t");
        boolean hasPrimaryWordOffsets = (primaryTriggerWordOffset.getFirst() != -1 && primaryTriggerWordOffset
                .getSecond() != -1);
        buff.append("PrimaryTrigger " + primaryTriggerCharOffset + " "
                + (hasPrimaryWordOffsets ? primaryTriggerWordOffset : "") + " "
                + primaryTriggerText.replaceAll("\\n", " ") + "\n");
        int n = argNames.size();
        boolean hasWordOffsets = (argWordOffsets.size() == n);
        for (int i = 0; i < n; i++) {
            buff.append("\t");
            buff.append(argNames.get(i) + " " + argCharOffsets.get(i) + " "
                    + (hasWordOffsets ? argWordOffsets.get(i) : "") + " "
                    + argTexts.get(i).replaceAll("\\n", " ") + "\n");
        }
        return buff.toString();
    }

    public void setPrimaryTriggerCharOffset(IntPair charOffset) {
        this.primaryTriggerCharOffset = new IntPair(charOffset.getFirst(),
                charOffset.getSecond());
    }
}
