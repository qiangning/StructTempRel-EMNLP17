package edu.illinois.cs.cogcomp.nlp.util;

/**
 * Created by qning2 on 12/16/16.
 */
public class MemoryDog {
    private Runtime runtime;
    private int chunk;
    public MemoryDog() {
        chunk = 1024*1024;
        runtime = Runtime.getRuntime();
    }
    public MemoryDog(int chunk){
        this.chunk = chunk;
        runtime = Runtime.getRuntime();
    }

    public void print(int verbose){
        if(verbose>=1) {
            System.out.println("##### Heap utilization statistics [MB] #####");
            //Print used memory
            System.out.println("Used Memory:"
                    + (runtime.totalMemory() - runtime.freeMemory()) / chunk);
        }
        if(verbose>=2)
            //Print free memory
            System.out.println("Free Memory:"
                    + runtime.freeMemory() / chunk);
        if(verbose>=3) {
            //Print total available memory
            System.out.println("Total Memory:" + runtime.totalMemory() / chunk);

            //Print Maximum available memory
            System.out.println("Max Memory:" + runtime.maxMemory() / chunk);
        }
    }
}
