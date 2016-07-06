/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {
    private final int REQ_CODE_SPEECH_INPUT = 100;
    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "hey voice alarm";
    boolean away = false;
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    CountDownTimer newtimer;
    MediaPlayer mediaPlayer;
    Thread playSong;
    SharedPreferences sharedPref;
    @Override
    public void onCreate(Bundle state) {
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        Calendar ams = Calendar.getInstance();
        System.out.println("Calendar: "+ams.get(Calendar.HOUR_OF_DAY));
        if (sharedPref != null) {
            String alarmSave = sharedPref.getString("alarms", "");
            if (!alarmSave.equals("") && AlarmData.times.size()==0) {
                System.out.println("AlarmSave: "+alarmSave);
                String arrs[] = alarmSave.split(",");
                for (int i = 0; i < arrs.length; i++) {
                    AlarmData.times.add(Integer.parseInt(arrs[i]));
                }
            }
        }
        super.onCreate(state);
        mediaPlayer = MediaPlayer.create(PocketSphinxActivity.this, R.raw.play);
        mediaPlayer.setLooping(true);
        playSong = new Thread(new Runnable() {
            public void run() {
               mediaPlayer.start();
            }
        });

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(DIGITS_SEARCH, R.string.digits_caption);
        captions.put(PHONE_SEARCH, R.string.phone_caption);
        captions.put(FORECAST_SEARCH, R.string.forecast_caption);
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();
        newtimer = new CountDownTimer(1000000000, 1000) {

            public void onTick(long millisUntilFinished) {
                Calendar c = Calendar.getInstance();
                //System.out.println("Calendar Ignore: "+AlarmData.ignore);
                //textView.setText(c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));
                if (c.get(Calendar.HOUR_OF_DAY) == getHour(AlarmData.ignore) && c.get(Calendar.MINUTE) == getMin(AlarmData.ignore)) {
                        mediaPlayer.pause();
                    System.out.println("ignore");
                } else {
                    System.out.println("Not ignore");
                    for (int i = 0; i < AlarmData.getTimes().size(); i++) {
                        if (AlarmData.ignore != AlarmData.getTimes().get(i)) {
                            int h = getHour(AlarmData.getTimes().get(i));
                            int m = getMin(AlarmData.getTimes().get(i));
                            if (c.get(Calendar.HOUR_OF_DAY) == h && c.get(Calendar.MINUTE) == m) {
                                if (mediaPlayer.isPlaying() == false) {
                                    mediaPlayer.start();
                                }
                            }
                        }
                    }
                    if (AlarmData.lookFor != 0) {
                        System.out.print("Inside 1");
                            int h = getHour(AlarmData.lookFor);
                            int m = getMin(AlarmData.lookFor);
                            if (c.get(Calendar.HOUR_OF_DAY) == h && c.get(Calendar.MINUTE) == m) {
                                System.out.print("Inside 2");
                                    mediaPlayer.start();


                        }
                    }
                    if (AlarmData.goog != -1) {
                /*    int m = getHour(AlarmData.goog);
                    m = m*100;
                    int s = getMin(AlarmData.goog);
                    int g = m+s;
                    int mC = c.get(Calendar.MINUTE);
                    mC = mC*100;
                    int sC = c.get(Calendar.SECOND);
                    int gC = mC+sC;
                    Toast.makeText(getApplicationContext(), "Goog: "+gC+""+g+"", Toast.LENGTH_SHORT).show();
                    if (gC-g == 5) {
                        Toast.makeText(getApplicationContext(), "Reset!", Toast.LENGTH_SHORT).show();
                        Intent mStartActivity = new Intent(getApplicationContext(), PocketSphinxActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    }

*/

                    }
                }
            }
            public void onFinish() {

            }
        };
        newtimer.start();
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("To start Voice Command say \"Voice Alarm\"");
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }
    public void onError() {

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }
    public void goToAlarms(View view) {

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            newtimer.cancel();
            mediaPlayer.stop();
        }
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_alarm);
        TextView alarmTextView = (TextView) findViewById(R.id.textViewAlarm);
        String sar = "";
        for (int i =0; i < AlarmData.getTimes().size(); i++) {
            String messAlarm = getHour(AlarmData.getTimes().get(i)) +": "+getMin(AlarmData.getTimes().get(i));
            sar = sar+ "Alarm "+(i+1)+": "+messAlarm+"\n";
        }
        if (sar.length() <2) {
            sar = "No Alarms Yet";
        }
        alarmTextView.setText(sar);
        away = true;

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onBackPressed() {
        if (away) {
            away = false;
            Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
            startActivity(b);
        }
        else {
            onDestroy();
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        String alarmSave = "";
        for (int i = 0; i < AlarmData.times.size(); i++) {
            alarmSave = alarmSave+AlarmData.times.get(i)+",";
        }
        savedInstanceState.putString("alarms",alarmSave);
        super.onSaveInstanceState(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        SharedPreferences.Editor editor = sharedPref.edit();
        String alarmSave = "";
        for (int i = 0; i < AlarmData.getTimes().size(); i++) {
            alarmSave = alarmSave+AlarmData.getTimes().get(i)+",";
        }
        editor.putString("alarms", alarmSave);
        editor.commit();
        super.onDestroy();
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            newtimer.cancel();
            mediaPlayer.stop();
        }
    }
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String alarmSave = savedInstanceState.getString("alarms");
        if (!alarmSave.equals("") && AlarmData.times.size()==0) {
            System.out.println("AlarmSave: "+alarmSave);
            String arrs[] = alarmSave.split(",");
            for (int i = 0; i < arrs.length; i++) {
                AlarmData.times.add(Integer.parseInt(arrs[i]));
            }
        }
    }
    public void editA (View view) {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            newtimer.cancel();
            mediaPlayer.stop();
        }
        promptSpeechInput();

    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    public int getHour(int t) {
        int a = t/100;
        return a;
    }
    public int getMin(int t) {
        int h = getHour(t);
        h = h *100;
        return t-h;
    }
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Calendar c = Calendar.getInstance();
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
        //textView.setText(c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));
        if (text.equals(KEYPHRASE)) {
            int check = 0;

            //textView.setText(c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));
            for (int i = 0 ; i < AlarmData.getTimes().size();i++) {
                if (AlarmData.ignore != AlarmData.getTimes().get(i)) {
                    int h = getHour(AlarmData.getTimes().get(i));
                    int m = getMin(AlarmData.getTimes().get(i));
                    if (c.get(Calendar.HOUR_OF_DAY) == h && c.get(Calendar.MINUTE) == m) {

                            mediaPlayer.pause();
                            promptSpeechInput();
                            check = 1;

                    }
                }
            }
            if (AlarmData.lookFor != 0) {
                if (AlarmData.lookFor != AlarmData.ignore) {
                    int h = getHour(AlarmData.lookFor);
                    int m = getMin(AlarmData.lookFor);
                    if (c.get(Calendar.HOUR_OF_DAY) == h && c.get(Calendar.MINUTE) == m) {

                            mediaPlayer.pause();
                            promptSpeechInput();
                            check = 1;


                    }
                }
            }
            if (check == 0){
                Toast.makeText(this, "You can only say voice commands if, an alarm is ringing, try adding an alarm!", Toast.LENGTH_SHORT).show();
            }
        }


    }
    private void promptSpeechInput() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            newtimer.cancel();
            mediaPlayer.stop();
        }
        String speech_prompt = "Enter Voice Command";
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                speech_prompt);

        try {


            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Speech Not Supported",
                    Toast.LENGTH_SHORT).show();
            return;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
            newtimer.cancel();
            mediaPlayer.stop();
        }
        String res = "";


        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    res = result.get(0);
                }
                break;
            }


        }
        try {
            ((TextView) findViewById(R.id.result_text)).setText("Latest Command: " + res);
            Toast.makeText(this, "Latest Command: " + res, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {

        }
        res = res.toLowerCase();
        if (res.contains(":")) {
            if (res.contains("delete")) {
                String arr[] = res.split(" ");
                String ares = "";
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].contains(":")) {
                        ares = arr[i];
                    }
                }
                int hour = Integer.parseInt(ares.substring(0, ares.indexOf(":")));
                if (res.contains("p.m.")) {
                    hour = hour+12;
                    if (hour >= 24) {
                        hour = hour - 24;
                    }
                }
                int min = Integer.parseInt(ares.substring(ares.indexOf(":") + 1));
                hour = hour * 100;
                int alarmTime = hour+min;
                AlarmData.times.remove(AlarmData.times.indexOf(alarmTime));
                Toast.makeText(this, "Deleted Alarm for " + ares + "", Toast.LENGTH_SHORT).show();
                Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
                startActivity(b);

            }
            else {
                String arr[] = res.split(" ");
                String ares = "";
                String messA = "";
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].contains(":")) {
                        ares = arr[i];
                        int hour = Integer.parseInt(ares.substring(0, ares.indexOf(":")));
                        if (res.contains("p.m.")) {
                            hour = hour+12;
                            if (hour >= 24) {
                                hour = hour - 24;
                            }
                        }
                        int min = Integer.parseInt(ares.substring(ares.indexOf(":") + 1));
                        AlarmData.addTime(hour, min);
                        messA = messA + " " + ares + "";
                    }
                }
                Toast.makeText(this, "Added Alarm for " + messA + "", Toast.LENGTH_SHORT).show();
                Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
                startActivity(b);
                //mediaPlayer.start();
            }
        }else if (res.contains("minute")) {
            int m = -1;
            String arr[] = res.split(" ");
            for (int i = 0; i < arr.length; i++) {
                try {
                    m = Integer.parseInt(arr[i]);
                }
                catch (Exception e) {

                }
            }
            if (res.contains("one")) {
                m = 1;
            }
            if (m != -1) {
                Calendar c = Calendar.getInstance();
                //textView.setText(c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND));

                int hour = c.get(Calendar.HOUR_OF_DAY);
                int min = c.get(Calendar.MINUTE);
                hour = hour * 100;
                int alarmTime = hour+min;
                AlarmData.ignore = alarmTime;
                hour = c.get(Calendar.HOUR_OF_DAY);
                System.out.println("Ignore: "+AlarmData.ignore);
                m = min+m;
                if (m >= 60) {
                    m = m-60;
                    hour = hour + 1;
                }
                if (hour >= 24) {
                    hour = hour - 24;
                }
                hour = hour*100;
                alarmTime = hour+m;
                AlarmData.lookFor = alarmTime;

                System.out.println("Look For: "+AlarmData.lookFor);

                Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
                startActivity(b);
            }

        }
        else if (res.contains("hour")) {
            int m = -1;
            String arr[] = res.split(" ");
            for (int i = 0; i < arr.length; i++) {
                try {
                    m = Integer.parseInt(arr[i]);
                }
                catch (Exception e) {

                }
            }
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int min = c.get(Calendar.MINUTE);
            hour = hour * 100;
            int alarmTime = hour+min;
            AlarmData.ignore = alarmTime;
            hour = c.get(Calendar.HOUR_OF_DAY);
            hour = hour+m;
            if (hour >= 24) {
                hour= hour-24;
                hour = hour + 1;
            }
            hour = hour*100;
            alarmTime = hour+min;
            AlarmData.lookFor = alarmTime;
            System.out.println("Look For: "+AlarmData.lookFor);

            Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
            startActivity(b);

        }
        Intent b = new Intent(getApplicationContext(),PocketSphinxActivity.class);
        startActivity(b);

    }
    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        try {
            ((TextView) findViewById(R.id.result_text)).setText("");
            if (hypothesis != null) {
                String text = hypothesis.getHypstr();
                makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e) {
            if (hypothesis != null) {
                String text = hypothesis.getHypstr();
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile


                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}
