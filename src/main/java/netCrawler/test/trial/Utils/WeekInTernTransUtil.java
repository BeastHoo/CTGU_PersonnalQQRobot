package netCrawler.test.trial.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WeekInTernTransUtil {
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    public static long transfer(){
        String CurDate = format.format(new Date());
        long weekCnt = 0;
        try {
            long openDay = format.parse("2022-02-27").getTime();
            long curDateInMills = format.parse(CurDate).getTime();
            long remainder = (curDateInMills - openDay)%(1000*60*60*24*7);
            weekCnt = (curDateInMills - openDay)/(1000*60*60*24*7);
            if (remainder != 0)
            {
                weekCnt += 1;
            }
            return weekCnt;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return weekCnt;
    }
}
