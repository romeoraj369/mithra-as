package com.gamor.mithrax.device.audio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PcmResamplerTest {

    @Test
    public void mixesStereoToMono() {
        short[] stereo = new short[]{100, 300, 200, 400};
        short[] mono = PcmResampler.toMono(stereo, stereo.length, 2);
        assertEquals(2, mono.length);
        assertEquals(200, mono[0]);
        assertEquals(300, mono[1]);
    }

    @Test
    public void resamplesLength() {
        short[] input = new short[16000];
        short[] out = PcmResampler.resample(input, 16000, 8000);
        assertEquals(8000, out.length);
    }
}
