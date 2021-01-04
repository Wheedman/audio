package com.example.user.audio

import android.Manifest
import android.media.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private var recording = false //states if it is recording right now
    private var recFile: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonRecord.setOnClickListener(startRecOnClickListener)
        buttonStop.setOnClickListener(stopRecOnClickListener)
        buttonPlay.setOnClickListener(playBackOnClickListener)

        buttonStop.isEnabled = false
    }

    private val startRecOnClickListener = View.OnClickListener {

        val recordThread = Thread(Runnable {
            recording = true
            requestAudioPermissions()
        })

        recordThread.start()
        buttonRecord.isEnabled = false
        buttonStop.isEnabled = true
    }

   private val stopRecOnClickListener = View.OnClickListener {
        recording = false
        buttonRecord.isEnabled = true
        buttonStop.isEnabled = false
    }

    private val playBackOnClickListener = View.OnClickListener {
        val inputStream = recFile!!.inputStream()

        val myRunnable = Runnable {
            playRecord(inputStream)
        }

        Thread(myRunnable).start()
    }

    private fun startRecord() {

        val recFileName = "test.raw"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        try {
            recFile = File(storageDir?.toString() + "/" + recFileName)
        } catch (ex: IOException) {
            // ErroroccurredwhilecreatingtheFile
        }
        try {
            val outputStream = FileOutputStream(recFile)
            val bufferedOutputStream = BufferedOutputStream(outputStream)
            val dataOutputStream = DataOutputStream(bufferedOutputStream)
            val minBufferSize= AudioRecord.getMinBufferSize(44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT)
            val aFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            val recorder = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(aFormat)
                    .setBufferSizeInBytes(minBufferSize)
                    .build()
            val audioData = ByteArray(minBufferSize)
            recorder.startRecording()

            while(recording) {
                val numofBytes= recorder.read(audioData, 0, minBufferSize)
                if(numofBytes>0) {
                    dataOutputStream.write(audioData)
                }
            }
            recorder.stop()
            dataOutputStream.close()
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    private fun playRecord(inputStream : InputStream){
            val minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT)
            val aBuilder = AudioTrack.Builder()
            val aAttr: AudioAttributes= AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            val aFormat: AudioFormat= AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            val track = aBuilder.setAudioAttributes(aAttr)
                    .setAudioFormat(aFormat)
                    .setBufferSizeInBytes(minBufferSize)
                    .build()
            track.setVolume(0.8f)
            track.play()

            val buffer = ByteArray(minBufferSize)
            try{
                var i = inputStream.read(buffer, 0, minBufferSize)
                while(i != -1) {
                    track.write(buffer, 0, i)
                    i = inputStream.read(buffer, 0, minBufferSize)
                }
            } catch(e: IOException) {
                e.printStackTrace()
            }
            try{
                inputStream.close()
            } catch(e: IOException) {
                e.printStackTrace()
            }
            track.stop()
            track.release()
    }


    private val MY_PERMISSIONS_RECORD_AUDIO = 1

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show()

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        MY_PERMISSIONS_RECORD_AUDIO)

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_RECORD_AUDIO)
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            //Go ahead with recording audio now
            startRecord()
        }
    }

    //Handling callback
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_RECORD_AUDIO -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    startRecord()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }
}
