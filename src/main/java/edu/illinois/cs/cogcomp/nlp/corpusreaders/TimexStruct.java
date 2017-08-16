package edu.illinois.cs.cogcomp.nlp.corpusreaders;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.uw.cs.lil.tiny.base.time.Time;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;

/**
 * Created by qning2 on 12/13/16.
 */
public class TimexStruct {
    protected String timexId;
    protected String text;
    protected IntPair charOffset;
    protected IntPair wordOffset;
    protected String startPoint;
    protected String endPoint;
    //
    protected boolean isImplicit;

    /**
     *
     */
    public TimexStruct(String timexId) {
        this.timexId = timexId;
        this.charOffset = new IntPair(-1, -1);
        this.wordOffset = new IntPair(-1, -1);
        this.isImplicit = false;
    }

    public TimexStruct(TemporalJointChunk tc){
        this.timexId = String.valueOf(tc.getTID());
        this.text = tc.getOriginalText();
        this.charOffset = new IntPair(tc.getCharStart(),tc.getCharEnd());
        this.wordOffset = new IntPair(-1, -1);
        this.isImplicit = false;
        setWordOffset(new IntPair(-1,-1));
    }
    public TimexStruct(TemporalJointChunk tc, TextAnnotation ta){
        this.timexId = String.valueOf(tc.getTID());
        this.text = tc.getOriginalText();
        this.charOffset = new IntPair(tc.getCharStart(),tc.getCharEnd());
        this.wordOffset = new IntPair(-1, -1);
        this.isImplicit = false;

        /*Get word offset*/
        IntPair tmp = new IntPair(-1, -1);
        tmp.setFirst(ta.getTokenIdFromCharacterOffset(tc.getCharStart()));
        tmp.setSecond(ta.getTokenIdFromCharacterOffset(tc.getCharEnd() - 1) + 1);
        setWordOffset(tmp);
    }

    /**
     * @return the tId
     */
    public String getTimexId() {
        return timexId;
    }

    /**
     * @param tId
     *            the tId to set
     */
    public void setTimexId(String tId) {
        this.timexId = tId;
    }

    /**
     * @param text
     *            the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param offset
     *            the offset to set
     */
    public void setCharOffset(IntPair offset) {
        this.charOffset = offset;
    }

    /**
     * @return the offset
     */
    public IntPair getCharOffset() {
        return charOffset;
    }

    /**
     * @param wordOffset
     *            the wordOffset to set
     */
    public void setWordOffset(IntPair wordOffset) {
        this.wordOffset = new IntPair(wordOffset.getFirst(),
                wordOffset.getSecond());
    }

    /**
     * @return the wordOffset
     */
    public IntPair getWordOffset() {
        return wordOffset;
    }

    /**
     * @param startPoint
     *            the startPoint to set
     */
    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    /**
     * @return the startPoint
     */
    public String getStartPoint() {
        return startPoint;
    }

    /**
     * @param endPoint
     *            the endPoint to set
     */
    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    /**
     * @return the endPoint
     */
    public String getEndPoint() {
        return endPoint;
    }

    /**
     * @param isImplicit
     *            the isImplicit to set
     */
    public void setImplicit(boolean isImplicit) {
        this.isImplicit = isImplicit;
    }

    /**
     * @return the isImplicit
     */
    public boolean isImplicit() {
        return isImplicit;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String interval = "["
                + ((startPoint.equals("null") && endPoint.equals("null")) ? "null"
                : startPoint + "," + endPoint) + "]";
        return timexId + "\t" + "(" + text + "," + interval + ")" + "\t"
                + charOffset + " " + wordOffset;
    }
}
