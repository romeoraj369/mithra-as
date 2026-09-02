package com.gamor.mithrax.device.memory;

import android.util.Log;

import androidx.annotation.NonNull;

import com.gamor.mithrax.device.ai.QwenLocalLanguageModel;
import com.gamor.mithrax.device.llama.LocalLlamaRuntime;
import com.gamor.mithrax.device.llama.QwenPrompts;
import com.gamor.mithrax.device.llama.QwenResponseParser;
import com.gamor.mithrax.domain.memory.ExtractedMemory;
import com.gamor.mithrax.domain.memory.HeuristicMemoryExtractor;
import com.gamor.mithrax.domain.memory.MemoryExtractionRequest;
import com.gamor.mithrax.domain.memory.MemoryExtractor;

import java.util.List;

/**
 * Uses the on-device Qwen model for memory extraction with heuristic fallback.
 */
public final class LlmMemoryExtractor implements MemoryExtractor {

  private static final String TAG = "LlmMemoryExtractor";

  private final QwenLocalLanguageModel languageModel;
  private final HeuristicMemoryExtractor fallback = new HeuristicMemoryExtractor();

  public LlmMemoryExtractor(@NonNull QwenLocalLanguageModel languageModel) {
    this.languageModel = languageModel;
  }

  @NonNull
  @Override
  public List<ExtractedMemory> extract(@NonNull MemoryExtractionRequest request) {
    if (!languageModel.isAvailable()) {
      return fallback.extract(request);
    }
    try {
      String raw = languageModel.generateRawForMemory(
              QwenPrompts.memorySystemPrompt(),
              QwenPrompts.memoryUserPrompt(request));
      return QwenResponseParser.parseMemories(raw);
    } catch (QwenResponseParser.ParseException e) {
      Log.w(TAG, "Falling back to heuristic memory extraction: " + e.getMessage());
      return fallback.extract(request);
    } catch (LocalLlamaRuntime.LlamaRuntimeException e) {
      Log.w(TAG, "Falling back to heuristic memory extraction", e);
      return fallback.extract(request);
    }
  }
}
