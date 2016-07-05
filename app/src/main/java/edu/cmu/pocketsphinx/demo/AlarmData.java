package edu.cmu.pocketsphinx.demo;

import java.util.ArrayList;

/**
 * Created by abirshukla on 7/2/16.
 */
public class AlarmData {
    public static ArrayList<Integer> times = new ArrayList<>();
    public static int ignore = 0;
    public static int lookFor = 0;
    public static int goog = -1;

    public static void addTime(int hour, int min) {
        hour = hour * 100;
        int alarmTime = hour+min;
        times.add(alarmTime);
        deleteDups();
    }
    public static void addGoog(int min,int sec) {
        min = min * 100;
        int alarmTime = min+sec;
        goog = alarmTime;
    }
    public static void deleteDups(){
        for (int i = 0; i < times.size();i++) {
            for (int k = i+1; k < times.size();k++) {
                if (times.get(i).equals(times.get(k))) {
                    times.remove(k);
                }
            }
        }
    }
    public static ArrayList<Integer> getTimes() {
        deleteDups();
        return times;
    }
}
