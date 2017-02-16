package com.example.wyj806.myapplication;

import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.media.MediaCodecInfo.*;

public class MainActivity extends AppCompatActivity {

    String LOG_TAG="test_ex";
    boolean sawInputEOS = false;
    final long TIMEOUT_US = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//geo:126.938052,37.578706?q=우리집
        //geo:37.445178,127.132287(Treasure)
//geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA
//geo:0,0?q=34.99,-106.61(Treasure)
//        Uri gmmIntentUri = Uri.parse("geo:0,0?q=34.99,-106.61(Treasure)");
//        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(mapIntent);

        try {

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            InputStream isr = getResources().openRawResource(R.raw.sound2);
            int size = isr.available();


            AssetFileDescriptor sampleFD = getResources().openRawResourceFd(R.raw.sound2);
            AssetFileDescriptor sampleFD_tmp = getResources().openRawResourceFd(R.raw.sound2);

            MediaExtractor extractor;
            MediaCodec codec;
            ByteBuffer[] codecInputBuffers;
            ByteBuffer[] codecOutputBuffers;

            extractor = new MediaExtractor();
            extractor.setDataSource(sampleFD.getFileDescriptor(), sampleFD.getStartOffset(), sampleFD.getLength());

            extractor.selectTrack(0); // <= You must select a track. You will read samples from the media

            Log.d(LOG_TAG, String.format("TRACKS #: %d", extractor.getTrackCount()));
            MediaFormat format = extractor.getTrackFormat(0);

 //           int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            String mime = format.getString(MediaFormat.KEY_MIME);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();

            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();
            MediaFormat format22 = codec.getOutputFormat();
            //  codecInputBuffers = codec.getInputBuffer();
            //  codecOutputBuffers = codec.getOutputBuffers();
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            int sampleRate = format22.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelConfig = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
            int buffsize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            // create an audiotrack object

            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffsize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
        //    audioTrack.setVolume(1.0f);
            boolean test = true;
            while (test) {
                int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize = extractor.readSampleData(dstBuf, 0);
                    Log.d(LOG_TAG, "bufIndex = " + inputBufIndex + "sampleSize = " + sampleSize);
                    long presentationTimeUs = 0;
                    if (sampleSize < 0) {
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();

                    }

                    codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
//                    info.offset = 0; //항상 0이 겠지만 Buffer에 채워넣은 데이터의 시작점
//                    info.size = sampleSize; //Buffer에 채워 넣은 데이터 사이즈 정보
//                    info.presentationTimeUs = presentationTimeUs; //디코딩의 경우 Play할 데이터 시간(micro sec)
//                    info.flags =0; //BUFFER_FLAG_CODEC_CONFIG: 읽은 버퍼의 정보가 설정값인지, BUFFER_FLAG_END_OF_STREAM: 마지막데이터인지
                        extractor.advance();
                    }
                }
                final int res = codec.dequeueOutputBuffer(info, TIMEOUT_US); //디코딩된 데이터 가져오기
                if (res >= 0) {
                    int outputBufIndex = res;
                    ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk); // Read the buffer all at once
                    buf.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

                    if (chunk.length > 0) {
                         audioTrack.write(chunk, 0, chunk.length);
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false /* render */);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        //sawOutputEOS = true;
                        test = false;
                    }
                    //test = false;
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.getOutputBuffers();
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    final MediaFormat oformat = codec.getOutputFormat();
//                   audioTrack.setPlaybackRate(oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                }

            }
            }catch(IOException e){
                e.printStackTrace();
            }
    }

    private MediaFormat makeAACCodecSpecificData(MediaFormat origin, int audioProfile, int sampleRate, int channelConfig) {
        MediaFormat format = origin;
      //  format.setString(MediaFormat.KEY_MIME, "audio/vorbis");
    //    format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
     //   format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig);

        int samplingFreq[] = {
                96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                16000, 12000, 11025, 8000
        };

        // Search the Sampling Frequencies
        int sampleIndex = -1;
        for (int i = 0; i < samplingFreq.length; ++i) {
            if (samplingFreq[i] == sampleRate) {
                Log.d("TAG", "kSamplingFreq " + samplingFreq[i] + " i : " + i);
                sampleIndex = i;
            }
        }

        if (sampleIndex == -1) {
            return null;
        }

        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put((byte) ((audioProfile << 3) | (sampleIndex >> 1)));

        csd.position(1);
        csd.put((byte) ((byte) ((sampleIndex << 7) & 0x80) | (channelConfig << 3)));
        csd.flip();
        format.setByteBuffer("csd-0", csd); // add csd-0

        for (int k = 0; k < csd.capacity(); ++k) {
            Log.e("TAG", "csd : " + csd.array()[k]);
        }

        return format;
    }


}
