package edu.uw.cs.lil.uwtime.learn.temporal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDuration;
import edu.uw.cs.utils.composites.Pair;

public class TemporalExecutionHistory implements Serializable{
	private static final long serialVersionUID = 4345548231624414895L;
	
	private List<Pair<TemporalSequence, TemporalDuration>> shiftArguments;
	private List<Pair<TemporalSequence, TemporalSequence>> intersectionArguments;

	public TemporalExecutionHistory() {
		shiftArguments = new LinkedList<Pair<TemporalSequence, TemporalDuration>>();
		intersectionArguments = new LinkedList<Pair<TemporalSequence, TemporalSequence>>();
	}
	public void addShiftArguments(TemporalSequence sequence, TemporalDuration duration) {
		shiftArguments.add(Pair.of(sequence, duration));
	}
	public List<Pair<TemporalSequence, TemporalDuration>> getShiftArguments() {
		return shiftArguments;
	}
	public List<Pair<TemporalSequence, TemporalSequence>> getIntersectionArguments() {
		return intersectionArguments;
	}
	public void addIntersectionArguments(TemporalSequence s1, TemporalSequence s2) {
		intersectionArguments.add(Pair.of(s1, s2));
	}
}
