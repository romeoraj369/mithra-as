package com.gamor.mithrax.device.audio;

import androidx.annotation.NonNull;

/**
 * Converts PCM to 16 kHz mono 16-bit, which Vosk expects.
 */
public final class PcmResampler {

    public static final int TARGET_RATE = 16000;

    private PcmResampler() {
    }

    @NonNull
    public static short[] toMono16k(@NonNull short[] input, int sampleCount, int channelCount, int sampleRate) {
        short[] mono = toMono(input, sampleCount, channelCount);
        if (sampleRate == TARGET_RATE) {
            return mono;
        }
        return resample(mono, sampleRate, TARGET_RATE);
    }

    @NonNull
    static short[] toMono(@NonNull short[] input, int sampleCount, int channelCount) {
        if (channelCount <= 1) {
            if (sampleCount == input.length) {
                return input;
            }
            short[] copy = new short[sampleCount];
            System.arraycopy(input, 0, copy, 0, sampleCount);
            return copy;
        }
        int frames = sampleCount / channelCount;
        short[] mono = new short[frames];
        for (int i = 0; i < frames; i++) {
            int sum = 0;
            int base = i * channelCount;
            for (int c = 0; c < channelCount; c++) {
                sum += input[base + c];
            }
            mono[i] = (short) (sum / channelCount);
        }
        return mono;
    }

    @NonNull
    static short[] resample(@NonNull short[] input, int inRate, int outRate) {
        if (inRate <= 0 || outRate <= 0 || input.length == 0) {
            return new short[0];
        }
        int outLength = (int) ((long) input.length * outRate / inRate);
        if (outLength <= 0) {
            return new short[0];
        }
        short[] output = new short[outLength];
        for (int i = 0; i < outLength; i++) {
            double src = (double) i * inRate / outRate;
            int index = (int) src;
            double frac = src - index;
            short a = input[Math.min(index, input.length - 1)];
            short b = input[Math.min(index + 1, input.length - 1)];
            output[i] = (short) Math.round(a + (b - a) * frac);
        }
        return output;
    }
}
