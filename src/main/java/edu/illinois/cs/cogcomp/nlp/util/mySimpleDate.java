package edu.illinois.cs.cogcomp.nlp.util;

import de.unihd.dbs.heideltime.standalone.DocumentType;
import de.unihd.dbs.heideltime.standalone.HeidelTimeStandalone;
import de.unihd.dbs.heideltime.standalone.OutputType;
import de.unihd.dbs.heideltime.standalone.POSTagger;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.TLINK.TlinkType;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by qning2 on 1/6/17.
 */
public class mySimpleDate {
    private int year;
    private int month;
    private int day;
    private int granularity;//1-->day,2-->month,3-->year
    private static HeidelTimeStandalone heidelTime = new HeidelTimeStandalone(
            Language.ENGLISH,
            DocumentType.NEWS,
            OutputType.TIMEML,
            "config/heideltime_config.props",
            POSTagger.NO,
            false
    );
    public mySimpleDate(int year, int month, int day, int granularity) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.granularity = granularity;
        //sanity check
        switch(granularity){
            case 1:
                if(day<=0||day>31) {
                    System.out.println("Invalid day.");
                    System.exit(-1);
                }
            case 2:
                if(month<=0||month>12) {
                    System.out.println("Invalid month.");
                    System.exit(-1);
                }
            case 3:
                if(year<0) {
                    System.out.println("Invalid year.");
                    System.exit(-1);
                }
                break;
            default:
                System.exit(-1);
        }
    }
    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getGranularity() {
        return granularity;
    }

    public void setGranularity(int granularity) {
        this.granularity = granularity;
    }

    public TlinkType compareDate(mySimpleDate other){
        if(other==null)
            return TlinkType.UNDEF;
        int year1 = year;
        int month1 = month;
        int day1 = day;
        int granularity1 = granularity;
        int year2 = other.getYear();
        int month2 = other.getMonth();
        int day2 = other.getDay();
        int granularity2 = other.getGranularity();
        if(year1>year2)
            return TlinkType.AFTER;
        else if(year1<year2)
            return TlinkType.BEFORE;
        else{//year1==year2
            if(granularity1==3){
                if(granularity2<3)
                    return TlinkType.INCLUDES;
                else{//granularity2==3
                    return TlinkType.EQUAL;
                }
            }
            else{//granularity1<3
                if(granularity2==3){
                    return TlinkType.IS_INCLUDED;
                }
                else{//granularity2<3
                    if(month1>month2)
                        return TlinkType.AFTER;
                    else if(month1<month2)
                        return TlinkType.BEFORE;
                    else{//month1==month2
                        if(granularity1==2){
                            if(granularity2<2)
                                return TlinkType.INCLUDES;
                            else {//granularity2==2
                                return TlinkType.EQUAL;
                            }
                        }
                        else{//granularity1==1
                            if(granularity2==2){
                                return TlinkType.IS_INCLUDED;
                            }
                            else{//granularity2==1
                                if(day1>day2)
                                    return TlinkType.AFTER;
                                else if(day1<day2)
                                    return TlinkType.BEFORE;
                                else // day1==day2
                                    return TlinkType.EQUAL;
                            }
                        }
                    }
                }
            }
        }
    }
    /*String2Date() only takes str formatted as 2017-01-01, 2017-01, or 2017*/
    public static mySimpleDate String2Date(String timex){
        Pattern p = Pattern.compile("[0-9]+(-[0-9]+(-[0-9]+)?)?");
        if(!p.matcher(timex).matches())
            return null;
        int year=-1, month=-1, day=-1, granularity=-1;
        String[] date = timex.split("-");
        granularity = 4-date.length;
        year = Integer.parseInt(date[0]);
        if(granularity<=2)
            month = Integer.parseInt(date[1]);
        if(granularity<=1)
            day = Integer.parseInt(date[2]);
        return new mySimpleDate(year,month,day,granularity);
    }
    public static TlinkType compareString(String timex1, String timex2){
        mySimpleDate date1 = mySimpleDate.String2Date(timex1);
        mySimpleDate date2 = mySimpleDate.String2Date(timex2);
        return date1==null?TlinkType.UNDEF:date1.compareDate(date2);
    }
    public static mySimpleDate callHeidelTime(String text, mySimpleDate ref) throws Exception{
        SimpleDateFormat f;
        if(ref==null)
            return null;
        switch(ref.getGranularity()){
            case 1:
                f = new SimpleDateFormat("yyyy-MM-dd");
                break;
            case 2:
                f = new SimpleDateFormat("yyyy-MM");
                break;
            case 3:
                f = new SimpleDateFormat("yyyy");
                break;
            default:
                f = new SimpleDateFormat("yyyy");
                System.exit(-1);
        }
        Date dct = f.parse(ref.toString());
        String res = heidelTime.process(text,dct);
        res = res.substring(res.indexOf("value=\"")+7,
                res.indexOf("\"",res.indexOf("value=\"")+7));
        if(res.equals("PRESENT_REF"))
            return ref;
        else
            return mySimpleDate.String2Date(res);
    }

    public String toString(){
        String s = Integer.toString(year);
        if(granularity<=2)
            s+="-"+Integer.toString(month);
        if(granularity<=1)
            s+="-"+Integer.toString(day);
        return s;
    }
    public static void main(String[] args){
        System.out.println(mySimpleDate.compareString("2017-01","2017").toStringfull());
    }
}
