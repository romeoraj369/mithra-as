package com.gamor.mithrax.device.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decodes a local encoded recording (AAC/M4A) to 16 kHz mono PCM on-device.
 */
public final class EncodedAudioDecoder {

    public interface Listener {
        /**
         * @return false to cancel
         */
        boolean onPcm(@NonNull short[] samples);
    }

    private EncodedAudioDecoder() {
    }

    public static void decodeToMono16k(@NonNull File audioFile,
                                       @NonNull AtomicBoolean cancelled,
                                       @NonNull Listener listener) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try {
            extractor.setDataSource(audioFile.getAbsolutePath());
            int track = selectAudioTrack(extractor);
            if (track < 0) {
                throw new IOException("No audio track in recording");
            }
            extractor.selectTrack(track);
            MediaFormat format = extractor.getTrackFormat(track);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) {
                throw new IOException("Unknown audio format");
            }
            int channelCount = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            int sampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : PcmResampler.TARGET_RATE;

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            while (!outputDone) {
                if (cancelled.get()) {
                    return;
                }
                if (!inputDone) {
                    int inputIndex = codec.dequeueInputBuffer(10_000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                        if (inputBuffer == null) {
                            throw new IOException("Decoder input buffer missing");
                        }
                        inputBuffer.clear();
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = codec.dequeueOutputBuffer(info, 10_000);
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER
                        || outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                        || outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    continue;
                }
                if (outputIndex >= 0) {
                    if (info.size > 0) {
                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                        if (outputBuffer == null) {
                            throw new IOException("Decoder output buffer missing");
                        }
                        MediaFormat outputFormat = codec.getOutputFormat();
                        int outChannels = outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                                ? outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : channelCount;
                        int outRate = outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                                ? outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : sampleRate;
                        short[] pcm = toShorts(outputBuffer, info.offset, info.size);
                        short[] mono16k = PcmResampler.toMono16k(pcm, pcm.length, outChannels, outRate);
                        if (mono16k.length > 0 && !listener.onPcm(mono16k)) {
                            return;
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                }
            }
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (RuntimeException ignored) {
                }
                codec.release();
            }
            extractor.release();
        }
    }

    private static int selectAudioTrack(@NonNull MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            String mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private static short[] toShorts(@NonNull ByteBuffer buffer, int offset, int size) {
        buffer.position(offset);
        buffer.limit(offset + size);
        ByteBuffer ordered = buffer.order(ByteOrder.LITTLE_ENDIAN);
        int count = size / 2;
        short[] samples = new short[count];
        for (int i = 0; i < count; i++) {
            samples[i] = ordered.getShort();
        }
        return samples;
    }
}
