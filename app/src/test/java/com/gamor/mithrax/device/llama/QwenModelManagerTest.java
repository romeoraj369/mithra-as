package com.gamor.mithrax.device.llama;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QwenModelManagerTest {

  private Context context;
  private QwenModelManager manager;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    manager = new QwenModelManager(context);
  }

  @Test
  public void reportsMissingModelWhenFileAbsent() {
    assertFalse(manager.isModelInstalled());
    assertEquals(0L, manager.getInstalledModelSizeBytes());
  }

  @Test
  public void rejectsTooSmallModelFile() throws Exception {
    manager.ensureModelsDirectory();
    File model = manager.getPreferredModelFile();
    try (FileOutputStream out = new FileOutputStream(model)) {
      out.write(new byte[]{1, 2, 3});
    }
    assertFalse(manager.isModelInstalled());
  }

  @Test
  public void detectsInstalledModelInFilesDir() throws Exception {
    manager.ensureModelsDirectory();
    File model = manager.getPreferredModelFile();
    try (RandomAccessFile raf = new RandomAccessFile(model, "rw")) {
      raf.setLength(2_100_000_000L);
    }
    assertTrue(manager.isModelInstalled());
    assertTrue(manager.getInstalledModelSizeBytes() >= 2_000_000_000L);
    assertNotNull(manager.getInstalledModelPath());
  }
}
