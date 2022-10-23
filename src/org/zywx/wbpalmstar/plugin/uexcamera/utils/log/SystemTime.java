package org.zywx.wbpalmstar.plugin.uexcamera.utils.log;

import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemTime {
    public static String getNowTime() {
        Time time = new Time();
        time.setToNow();
        int year = time.year;
        int month = time.month + 1;
        int day = time.monthDay;
        int minute = time.minute;
        int hour = time.hour;
        int sec = time.second;
        return year + "-" + month + "-" + day + " " + hour + ":" + minute + ":"
                + sec;
    }

    public static String getCurYearAndMonth() {
        Time time = new Time();
        time.setToNow();
        int year = time.year;
        int month = time.month + 1;
        return year + "_" + month;
    }

    public static String getSpecialFormatTime(String format) {
        SimpleDateFormat tm = new SimpleDateFormat(format);
        return tm.format(new Date());
    }

}