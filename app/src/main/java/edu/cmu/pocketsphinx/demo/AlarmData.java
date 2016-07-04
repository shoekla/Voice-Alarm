package edu.cmu.pocketsphinx.demo;

import java.util.ArrayList;

/**
 * Created by abirshukla on 7/2/16.
 */
public class AlarmData {
    public static ArrayList<Integer> times = new ArrayList<>();
    public static int ignore = 0;
    public static int lookFor = 0;

    public static void addTime(int hour, int min) {
        hour = hour * 100;
        int alarmTime = hour+min;
        times.add(alarmTime);
    }
    public static ArrayList<Integer> getTimes() {
        return times;
    }
}
