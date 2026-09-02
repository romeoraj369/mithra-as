package com.gamor.mithrax.device.stt;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class VoskModelManagerTest {

    @Test
    public void bundledAssetsIncludeAcousticAndConf() {
        VoskModelManager manager = new VoskModelManager();
        assertTrue(
                "assets/model must include non-empty am/ and conf/ for on-device STT",
                manager.isModelAvailable(ApplicationProvider.getApplicationContext()));
    }
}
