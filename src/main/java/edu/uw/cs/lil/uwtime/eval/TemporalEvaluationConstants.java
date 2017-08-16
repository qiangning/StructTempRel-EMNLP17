package edu.uw.cs.lil.uwtime.eval;

import java.util.HashMap;
import java.util.Map;

import edu.uw.cs.lil.tiny.ccg.categories.AbstractCategoryServices;
import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalApproximateReference;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalReference;
import edu.uw.cs.lil.uwtime.eval.entities.TemporalSequence;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalCentury;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDay;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalDecade;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalHour;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalMinute;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalMonth;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalQuarter;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalSeason;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalSecond;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalTimeOfDay;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalWeek;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalWeekend;
import edu.uw.cs.lil.uwtime.eval.entities.durations.TemporalYear;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalAddition;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalID;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalIntersect;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalModify;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalMultiply;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalNth;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalPredicate;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalQuantify;
import edu.uw.cs.lil.uwtime.eval.predicates.TemporalShift;
import edu.uw.cs.lil.uwtime.eval.predicates.iota.TemporalFollowing;
import edu.uw.cs.lil.uwtime.eval.predicates.iota.TemporalNext;
import edu.uw.cs.lil.uwtime.eval.predicates.iota.TemporalPreceding;
import edu.uw.cs.lil.uwtime.eval.predicates.iota.TemporalPrevious;
import edu.uw.cs.lil.uwtime.eval.predicates.iota.TemporalThis;

public class TemporalEvaluationConstants {
	final private Map<LogicalConstant, TemporalPredicate> predicates;
	final private Map<LogicalConstant, Object> constantObjects;
	final private AbstractCategoryServices<LogicalExpression> categoryServices;

	public TemporalEvaluationConstants(AbstractCategoryServices<LogicalExpression> categoryServices) {	
		this.categoryServices = categoryServices;

		constantObjects = new HashMap<LogicalConstant, Object>();
		addAllConstants();

		predicates = new HashMap<LogicalConstant, TemporalPredicate>();	
		addAllPredicates();
	}

	private void addAllPredicates() {
		addPredicate("this:<s,<r,r>>", new TemporalThis());
		addPredicate("next:<s,<r,r>>", new TemporalNext());
		addPredicate("previous:<s,<r,r>>", new TemporalPrevious());
		addPredicate("following:<s,<r,r>>", new TemporalFollowing());
		addPredicate("preceding:<s,<r,r>>", new TemporalPreceding());
		addPredicate("nth:<n,<s,s>>", new TemporalNth());
		addPredicate("intersect:<s,<s,s>>", new TemporalIntersect());
		addPredicate("*:<n,<d,d>>", new TemporalMultiply());
		addPredicate("*:<n,<n,n>>", new TemporalMultiply());
		addPredicate("+:<n,<n,n>>", new TemporalAddition());
		addPredicate("shift:<s,<d,s>>", new TemporalShift());
		addPredicate("every:<s,s>", new TemporalQuantify(TemporalQuantifier.EVERY));
		addPredicate("some:<d,d>", new TemporalQuantify(TemporalQuantifier.SOME));
		addPredicate("bc:<s,s>", new TemporalModify(TemporalModifier.BC));
		addPredicate("less:<d,d>", new TemporalModify(TemporalModifier.LESS));
		addPredicate("more:<d,d>", new TemporalModify(TemporalModifier.MORE));
		addPredicate("less_e:<d,d>", new TemporalModify(TemporalModifier.LESS_E));
		addPredicate("more_e:<d,d>", new TemporalModify(TemporalModifier.MORE_E));
		addPredicate("start:<d,d>", new TemporalModify(TemporalModifier.START));
		addPredicate("start:<s,s>", new TemporalModify(TemporalModifier.START));
		addPredicate("mid:<d,d>", new TemporalModify(TemporalModifier.MID));
		addPredicate("mid:<s,s>", new TemporalModify(TemporalModifier.MID));
		addPredicate("end:<d,d>", new TemporalModify(TemporalModifier.END));
		addPredicate("end:<s,s>", new TemporalModify(TemporalModifier.END));
		addPredicate("approx:<d,d>", new TemporalModify(TemporalModifier.APPROX));
		addPredicate("approx:<s,s>", new TemporalModify(TemporalModifier.APPROX));
		addPredicate("id:<s,s>", new TemporalID());
		addPredicate("id:<d,d>", new TemporalID());
		addPredicate("id:<a,a>", new TemporalID());
	}

	private void addAllConstants() {
		addConstant("second:s", new TemporalSequence(new TemporalSecond(0)));
		addConstant("minute:s", new TemporalSequence(new TemporalMinute(0)));
		addConstant("hour:s", new TemporalSequence(new TemporalHour(0)));
		addConstant("timeofday:s", new TemporalSequence(new TemporalTimeOfDay(0)));
		addConstant("day:s", new TemporalSequence(new TemporalDay(0)));
		addConstant("month:s", new TemporalSequence(new TemporalMonth(0)));
		addConstant("season:s", new TemporalSequence(new TemporalSeason(0)));
		addConstant("quarter:s", new TemporalSequence(new TemporalQuarter(0)));
		addConstant("weekend:s", new TemporalSequence(new TemporalWeekend(0)));
		addConstant("week:s", new TemporalSequence(new TemporalWeek(0)));
		addConstant("year:s", new TemporalSequence(new TemporalYear(0)));
		addConstant("decade:s", new TemporalSequence(new TemporalDecade(0)));
		addConstant("century:s", new TemporalSequence(new TemporalCentury(0)));

		addConstant("second:d", new TemporalSecond(1));
		addConstant("minute:d", new TemporalMinute(1));
		addConstant("hour:d", new TemporalHour(1));
		addConstant("timeofday:d", new TemporalTimeOfDay(1));
		addConstant("day:d", new TemporalDay(1));
		addConstant("month:d", new TemporalMonth(1));
		addConstant("season:d", new TemporalSeason(1));
		addConstant("quarter:d", new TemporalQuarter(1));
		addConstant("weekend:d", new TemporalWeekend(1));
		addConstant("week:d", new TemporalWeek(1));
		addConstant("year:d", new TemporalYear(1));
		addConstant("decade:d", new TemporalDecade(1));
		addConstant("century:d", new TemporalCentury(1));

		addConstant("present_ref:a", new TemporalApproximateReference("PRESENT_REF"));
		addConstant("future_ref:a", new TemporalApproximateReference("FUTURE_REF"));
		addConstant("past_ref:a", new TemporalApproximateReference("PAST_REF"));
		addConstant("unknown:a", new TemporalApproximateReference("PXX"));
		addConstant("ref_time:r", new TemporalReference());
	}

	private void addPredicate(String semantics, TemporalPredicate predicate) {
		LogicalExpression e = categoryServices.parseSemantics(semantics);
		while (e instanceof Lambda)
			e = ((Lambda) e).getBody();
		predicates.put((LogicalConstant)((Literal) e).getPredicate(), predicate);
	}

	private void addConstant(String semantics, Object constant) {
		constantObjects.put((LogicalConstant) categoryServices.parseSemantics(semantics), constant);
	}

	public Object getConstantObject(LogicalConstant constant) {		
		return constantObjects.get(constant);
	}

	public TemporalPredicate getPredicate(LogicalExpression expression) {
		return predicates.get(expression);
	}
}
