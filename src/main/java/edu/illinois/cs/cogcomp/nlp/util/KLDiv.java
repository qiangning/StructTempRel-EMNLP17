package edu.illinois.cs.cogcomp.nlp.util;

import java.util.Arrays;

/**
 * Created by chuchu on 1/26/17.
 */
public class KLDiv {
    public static double kldivergence(double[] p, double[] q){
        assert p.length==q.length;
        int n = p.length;
        double kl = 0;
        for(int i=0;i<n;i++){
            if(p[i]<1e-6)
                continue;
            kl += p[i]*Math.log(p[i]/q[i]);
        }
        return kl;
    }

    public static double kldivergence(double[] p){
        int n = p.length;
        double[] q = new double[n];
        Arrays.fill(q,1.0/n);
        return kldivergence(p,q);
    }
}
