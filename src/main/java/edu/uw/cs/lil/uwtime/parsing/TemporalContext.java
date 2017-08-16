package edu.uw.cs.lil.uwtime.parsing;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.uw.cs.lil.uwtime.chunking.chunks.TemporalJointChunk;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.utils.composites.Pair;

public class TemporalContext implements Serializable{
	private static final long serialVersionUID = -6490191095086318792L;

	public static enum TemporalDirection { PRECEDING, FOLLOWING, LAST, NEXT, THIS, NONE };
	public static enum ShiftGranularity { ANCHOR, DELTA, NONE };
	final private TemporalDirection direction;
	final private TemporalSequence reference;
	final private TemporalJointChunk referenceSource;
	final private ShiftGranularity granularity;

	public TemporalContext() {
		this(TemporalDirection.NONE, null, null, ShiftGranularity.NONE);
	}

	public TemporalContext(TemporalDirection direction, TemporalSequence reference, TemporalJointChunk referenceSource, ShiftGranularity granularity) {
		this.direction = direction;
		this.reference = reference;
		this.referenceSource = referenceSource;
		this.granularity = granularity;
	}

	private static void addContexts(Pair<TemporalSequence, TemporalJointChunk> r, List<TemporalContext> contexts, boolean hasShift, boolean isSequence, boolean usesReference, ShiftGranularity sg) {
		contexts.add(new TemporalContext(TemporalDirection.NONE, r.first(), r.second(), sg));
		if (isSequence && r.first() != null) {
			contexts.add(new TemporalContext(TemporalDirection.PRECEDING, r.first(), r.second(), sg));
			contexts.add(new TemporalContext(TemporalDirection.FOLLOWING, r.first(), r.second(), sg));
		}
	}

	private static void addContexts(Pair<TemporalSequence, TemporalJointChunk> r, List<TemporalContext> contexts, boolean hasShift, boolean isSequence, boolean usesReference) {
		if (hasShift) {
			addContexts(r, contexts, hasShift, isSequence, usesReference, ShiftGranularity.ANCHOR);
			addContexts(r, contexts, hasShift, isSequence, usesReference, ShiftGranularity.DELTA);
		}
		else {
			addContexts(r, contexts, hasShift, isSequence, usesReference, ShiftGranularity.NONE);
		}
	}

	public static List<TemporalContext> getPossibleContexts(LogicalExpression l, List<Pair<TemporalSequence, TemporalJointChunk>> references) {
		List<TemporalContext> contexts = new LinkedList<TemporalContext> ();
		boolean hasShift = l.toString().contains("shift:<");
		boolean isSequence = l.getType().isExtending(TemporalSequence.LOGICAL_TYPE);
		boolean usesReference = l.toString().contains("ref_time:r");

		for (Pair<TemporalSequence, TemporalJointChunk> r : references)
			if (!(usesReference && r.first() == null))
				addContexts(r, contexts, hasShift, isSequence, usesReference);
		return contexts;
	}


	public TemporalSequence getReference() {
		return reference;
	}

	public TemporalJointChunk getReferenceSource() {
		return referenceSource;
	}

	public String getReferenceType() {
		if (referenceSource != null)
			return "LAST_RANGE";
		else if (reference != null)
			return "DCT";
		else
			return "NONE";
	}

	public TemporalDirection getDirection() {
		return direction;
	}

	public ShiftGranularity getGranularity() {
		return granularity;
	}

	public LogicalExpression applyTemporalDirection(LogicalExpression l, LogicalExpressionCategoryServices categoryServices) {            
		String semantics = null;
		switch (direction) {
		case PRECEDING:
			semantics = "(lambda $0:s (preceding:<s,<r,r>> $0 ref_time:r))";
			break;
		case FOLLOWING:
			semantics = "(lambda $0:s (following:<s,<r,r>> $0 ref_time:r))";
			break;
		case LAST:
			semantics = "(lambda $0:s (last:<s,<r,r>> $0 ref_time:r))";
			break;
		case NEXT:
			semantics = "(lambda $0:s (next:<s,<r,r>> $0 ref_time:r))";
			break;
		case THIS:
			semantics = "(lambda $0:s (this:<s,<r,r>> $0 ref_time:r))";
			break;
		case NONE:
			return l;
		}
		return categoryServices.apply(categoryServices.parseSemantics(semantics), l);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (reference != null)
			sb.append("Reference: " + reference.getValue() + "(" + (referenceSource == null ? "DCT" : referenceSource.getOriginalText()) + ") ");
		if (direction != TemporalDirection.NONE)
			sb.append("Direction: " + direction + " ");
		if (granularity != ShiftGranularity.NONE)
			sb.append("Shift granularity: " + granularity + " ");
		return sb.toString();
	}
}
