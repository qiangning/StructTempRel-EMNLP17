package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalSeason extends TemporalDuration{
	final private static String[] SEASON_LABELS = {"WI","SP","SU","FA"};  
	public TemporalSeason(int n) {
		super(n);
	}
	
	public TemporalSeason(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalSeason(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "season";
	}

	@Override
	public String getLocalSequenceValue() {
		return "-" + (isFixed() ? SEASON_LABELS[getN() - 1] : "X");
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalSeason(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}
	
	@Override
	public int getGranularity() {
		return 2;
	}

	@Override
	public int getMaximumN() {
		return 4;
	}

	@Override
	public String getDurationValue() {
		// Never actually used in dataset
		return "P" + (isFixed() ? SEASON_LABELS[getN() - 1] : "X");
	}
	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalYear(0, this);
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		jodaDate = jodaDate.withMonthOfYear((getN() - 1) * 3 + 1);
		return child == null ? jodaDate :child.getStartJodaTime(jodaDate);
	}

	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withMonths(3);
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return (jodaTime.getMonthOfYear() - 1) / 3 + 1;
	}
}
