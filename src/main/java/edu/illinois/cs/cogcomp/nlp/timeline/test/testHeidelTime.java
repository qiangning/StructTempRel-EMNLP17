package edu.illinois.cs.cogcomp.nlp.timeline.test;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import edu.illinois.cs.cogcomp.nlp.util.mySimpleDate;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by qning2 on 1/6/17.
 */
public class testHeidelTime {
    public static void main(String[] args) throws Exception {
        /*HeidelTimeStandalone heidelTime = new HeidelTimeStandalone(
                Language.ENGLISH,
                DocumentType.NEWS,
                OutputType.TIMEML,
                "config/heideltime_config.props",
                POSTagger.NO,
                false
        );
        SimpleDateFormat f= new SimpleDateFormat("yyyy-MM-dd");
        Date dct = f.parse("2017-01-01");
        heidelTime.process("October", dct);
        heidelTime.process("Oct. 1, 2000", dct);
        heidelTime.process("three days ago", dct);
        heidelTime.process("October", dct);
        heidelTime.process("Oct. 1, 2000", dct);
        heidelTime.process("three days ago", dct);
        heidelTime.process("October", dct);
        heidelTime.process("Oct. 1, 2000", dct);
        heidelTime.process("three days ago", dct);
        heidelTime.process("October", dct);
        heidelTime.process("Oct. 1, 2000", dct);
        heidelTime.process("three days ago", dct);*/
        mySimpleDate ref = new mySimpleDate(1988,8,-1,2);
        System.out.println(mySimpleDate.callHeidelTime("a year ago",ref));
        System.out.println(mySimpleDate.callHeidelTime("one year ago",ref));
        System.out.println(mySimpleDate.callHeidelTime("Oct. 1, 2000",ref));
        System.out.println(mySimpleDate.callHeidelTime("three days ago",ref));
        System.out.println(mySimpleDate.callHeidelTime("next month",ref));
        System.out.println(mySimpleDate.callHeidelTime("previous month",ref));
        System.out.println(mySimpleDate.callHeidelTime("last year",ref));
        System.out.println(mySimpleDate.callHeidelTime("last friday",ref));
        System.out.println(mySimpleDate.callHeidelTime("next sunday",ref));
        System.out.println(mySimpleDate.callHeidelTime("7 days ago",ref));
        System.out.println(mySimpleDate.callHeidelTime("tomorrow",ref));
        System.out.println(mySimpleDate.callHeidelTime("the day after tomorrow",ref));
        System.out.println(mySimpleDate.callHeidelTime("3 days after today",ref));//3days->duration, today-->ref

    }
}
