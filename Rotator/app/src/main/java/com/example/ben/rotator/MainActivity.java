package com.example.ben.rotator;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;


public class MainActivity extends ActionBarActivity {
    private Button recordButton = null;
    private Button playButton = null;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    public static final String LOG_TAG = "AudioRecordTest";

    private AudioRecord audioRecorder = null;
    private MediaPlayer player = null;

    private final int frequency = 44100;
    private final int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
    private final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private String recordingFile = null;

    public MainActivity() {
        recordingFile = Environment.getExternalStorageDirectory().getAbsolutePath();
        recordingFile += "/recording.3gp";
        //int minBufferSize = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);

        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,frequency, channelConfiguration,audioEncoding,(int)Math.pow(Math.pow(2,10),2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordButton = (Button) findViewById(R.id.record_button);
        playButton = (Button) findViewById(R.id.play_button);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void recordButtonAction(View v) {
        if(!isRecording) {
            RecordAudio audio = new RecordAudio();
            audio.execute();
            recordButton.setText("Stop Recording");
        } else {
            recordButton.setText("Record");
        }
        isRecording=!isRecording;
    }

    public void playButtonAction(View v) {
        if (!isPlaying) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        PlayAudio playAudio = new PlayAudio();
        playAudio.execute();
        playButton.setText("Stop");
        isPlaying = true;
    }

    private void stopPlaying() {
        playButton.setText("Play");
        isPlaying=false;
    }


    int numBytes = 0;

    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isPlaying = true;

            int bufferSize = (int)Math.pow(Math.pow(2,10),2);
            short[] audioData = new short[bufferSize / 4];
            Stack<short[]> audioStack = new Stack<>();

            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, frequency,
                        channelConfiguration, audioEncoding, numBytes,
                        AudioTrack.MODE_STREAM);
                while (isPlaying && dis.available() > 0) {
                    int i = audioData.length - 1;
                    /*
                    while (dis.available() > 0 && i < audioData.length) {
                        audioData[i] = dis.readShort();
                        i++;
                    }
                    */

                    while (dis.available() > 0 && i >= 0) {
                        audioData[i] = dis.readShort();
                        i--;
                    }

                    audioStack.push(audioData);

                    //audioTrack.write(audioData, 0, audioData.length);
                }
                dis.close();

                audioTrack.play();
                while (!audioStack.empty()) {
                audioTrack.write(audioStack.peek(), 0, audioStack.pop().length);
                }

                audioTrack.stop();
                dis=null;
                audioTrack=null;
                //isPlaying = false;
                //playButton.setText("Play");
            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioTrack", "Playback Failed");
            }
            return null;
        }
    }

    //http://www.java2s.com/Code/Android/Media/UsingAudioRecord.htm
    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isRecording = true;
            try {
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(
                                recordingFile)));
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[bufferSize];
                audioRecord.startRecording();
                int r = 0;
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            bufferSize);
                    for (int i = 0; i < bufferReadResult; i++) {
                        dos.writeShort(buffer[i]);
                    }
                    publishProgress(new Integer(r));
                    r++;
                    numBytes += bufferSize;
                }
                audioRecord.stop();
                dos.close();
                audioRecord=null;
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            //statusText.setText(progress[0].toString());
        }


        /*
        protected void onPostExecute(Void result) {
            startRecordingButton.setEnabled(true);
            stopRecordingButton.setEnabled(false);
            startPlaybackButton.setEnabled(true);
        }
        */
    }

}
