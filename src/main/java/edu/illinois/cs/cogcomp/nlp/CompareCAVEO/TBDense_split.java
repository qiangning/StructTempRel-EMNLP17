package edu.illinois.cs.cogcomp.nlp.CompareCAVEO;

import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by qning2 on 4/3/17.
 */
public class TBDense_split {
    public static final String[] devDocs = {
            "APW19980227.0487",
            "CNN19980223.1130.0960",
            "NYT19980212.0019",
            "PRI19980216.2000.0170",
            "ed980111.1130.0089"
    };

    public static final String[] testDocs = {
            "APW19980227.0489",
            "APW19980227.0494",
            "APW19980308.0201",
            "APW19980418.0210",
            "CNN19980126.1600.1104",
            "CNN19980213.2130.0155",
            "NYT19980402.0453",
            "PRI19980115.2000.0186",
            "PRI19980306.2000.1675"
    };
    public static final String[] trainDocs = {
            "APW19980227.0476",
            "ea980120.1830.0456",
            "AP900815-0044",
            "ABC19980114.1830.0611",
            "NYT19980206.0460",
            "APW19980219.0476",
            "APW19980213.1320",
            "PRI19980121.2000.2591",
            "APW19980213.1310",
            "CNN19980227.2130.0067",
            "CNN19980222.1130.0084",
            "PRI19980205.2000.1998",
            "ABC19980120.1830.0957",
            "AP900816-0139",
            "PRI19980205.2000.1890",
            "APW19980213.1380",
            "ea980120.1830.0071",
            "NYT19980206.0466",
            "PRI19980213.2000.0313",
            "APW19980227.0468",
            "ABC19980108.1830.0711",
            "ABC19980304.1830.1636"
    };
    public static int findDoc(String f){
        // 0: not found
        // 1: train
        // 2: dev
        // 3: test
        for(String str:trainDocs){
            if(f.equals(str))
                return 1;
        }
        for(String str:devDocs){
            if(f.equals(str))
                return 2;
        }
        for(String str:testDocs){
            if(f.equals(str))
                return 3;
        }
        return 0;
    }
    public static void main(String[] args)throws Exception{
        String dir = "data/TempEval3/";
        String fname = "TimebankDense.full.txt";
        Scanner in = new Scanner(new FileReader(dir+ File.separator+fname));
        String prev_file = "";

        int[] cnt = new int[3];
        while (in.hasNextLine()) {
            String line = in.nextLine();
            String[] tmp = line.split(" |\t");
            String file = tmp[0];
            //if(findDoc(file)==1){
            if((tmp[1].startsWith("e")&&tmp[2].startsWith("t")||tmp[1].startsWith("t")&&tmp[2].startsWith("e"))
                    &&!tmp[3].equals("v"))
                cnt[findDoc(file)-1]++;
            //}

            if(file.equals(prev_file))
                continue;
            System.out.println("\""+file+"\",");
            prev_file = file;
        }
        System.out.println(cnt[0]);
        System.out.println(cnt[1]);
        System.out.println(cnt[2]);
    }
}
