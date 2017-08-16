package edu.uw.cs.lil.uwtime.eval.entities.durations;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import edu.uw.cs.lil.uwtime.eval.TemporalModifier;
import edu.uw.cs.lil.uwtime.eval.TemporalQuantifier;


public class TemporalQuarter extends TemporalDuration {
	public TemporalQuarter(int n) {
		super(n);
	}
	
	public TemporalQuarter(int n, TemporalDuration child) {
		super(n, child);
	}

	public TemporalQuarter(int n, TemporalDuration child, TemporalQuantifier quantifier, TemporalModifier modifier) {
		super(n, child, quantifier, modifier);
	}

	@Override
	public String getName() {
		return "quarter";
	}

	@Override
	public String getLocalSequenceValue() {
		return isFixed() ? String.format("-Q%d", getN()) : "-QX";
	}

	@Override
	public int getGranularity() {
		return 1;
	}

	@Override
	public TemporalDuration getDefaultParent() {
		return new TemporalYear(0, this);
	}

	@Override
	public TemporalDuration clone() {
		return new TemporalQuarter(getN(), child == null ? null : child.clone(), quantifier, modifier);
	}

	@Override
	public int getMaximumN() {
		return 4;
	}

	@Override
	public LocalDateTime getStartJodaTime(LocalDateTime jodaDate) {
		/*
		 * Fiscal quarters
		int startingMonth;
		switch (getN()) {
		case 1:
			startingMonth = 10;
			break;
		case 2:
			startingMonth = 1;
			break;
		case 3:
			startingMonth = 4;
			break;
		case 4:
			startingMonth = 7;
			break;
		default:
			startingMonth = 0;
			break;
		}
		jodaDate = jodaDate.withMonthOfYear(startingMonth);
		*/
		jodaDate = jodaDate.withMonthOfYear((getN() - 1) * 3 + 1);
		return child == null ? jodaDate :child.getStartJodaTime(jodaDate);
	}
	@Override
	public Period getJodaUnitPeriod() {
		return new Period().withMonths(3);
	}
	@Override
	public String getDurationValue() {
		return String.format("P%sQ", isFixed() ? getN() + "": "X");
	}

	@Override
	public int atSameGranularity(LocalDateTime jodaTime) {
		return (jodaTime.getMonthOfYear() - 1) / 3 + 1;
	}
}
