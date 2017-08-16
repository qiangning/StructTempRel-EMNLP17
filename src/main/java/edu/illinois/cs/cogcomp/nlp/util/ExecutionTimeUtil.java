package edu.illinois.cs.cogcomp.nlp.util;

import edu.illinois.cs.cogcomp.core.utilities.ExecutionTimer;

/**
 * Created by qning2 on 11/28/16.
 */
public class ExecutionTimeUtil extends ExecutionTimer {

    public void print(){
        System.out.println("Total Time: "+getTimeSeconds());
    }
}
