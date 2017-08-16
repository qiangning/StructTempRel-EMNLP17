package edu.illinois.cs.cogcomp.nlp.timeline.test;

import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;

/**
 * Created by qning2 on 12/19/16.
 */
public class testGurobi {
    /*
    maximize 5x+10y
    s.t. (1) y<=-x+1 (x+y<=3) (2) y<=x+1 (-x+y<=1)
    =>x=1,y=2
    */
    public static void main(String[] args) throws Exception{
        int[] vars = new int[2];
        double[] coef1 = new double[]{1,1};
        double[] coef2 = new double[]{-1,1};
        GurobiHook solver = new GurobiHook();
        vars[0]=solver.addRealVariable(5);
        vars[1]=solver.addRealVariable(10);
        solver.addLessThanConstraint(vars,coef1,3);
        solver.addLessThanConstraint(vars,coef2,1);
        solver.setMaximize(true);
        solver.solve();
        System.out.println("Solution");
        solver.printSolution();
    }
}
