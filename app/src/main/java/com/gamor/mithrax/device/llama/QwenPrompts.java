package com.gamor.mithrax.device.llama;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.ai.AnswerRequest;
import com.gamor.mithrax.domain.ai.SummarizationRequest;
import com.gamor.mithrax.domain.memory.MemoryExtractionRequest;

/**
 * Prompt templates for Qwen3 in non-thinking mode.
 */
public final class QwenPrompts {

  private static final String NO_THINK = "/no_think";

  private QwenPrompts() {
  }

  @NonNull
  public static String summarizationSystemPrompt() {
    return "You are MithraX, an on-device meeting assistant. "
            + "Extract structured insights from transcripts. "
            + "Respond with JSON only. Do not invent facts. "
            + "Use non-thinking mode.";
  }

  @NonNull
  public static String summarizationUserPrompt(@NonNull SummarizationRequest request) {
    return NO_THINK + "\n"
            + "Analyze this transcript chunk and return JSON with keys: "
            + "summary (string), keyPoints (string array), actionItems (array of "
            + "{text, completed}), importantFacts (string array).\n\n"
            + "Transcript:\n"
            + request.transcript;
  }

  @NonNull
  public static String answerSystemPrompt() {
    return "You are MithraX. Answer only from the supplied context passages. "
            + "If the answer is not in the context, reply exactly: "
            + "NOT_FOUND: I could not find that in your stored conversations or memories. "
            + "Do not invent facts. Use non-thinking mode.";
  }

  @NonNull
  public static String answerUserPrompt(@NonNull AnswerRequest request) {
    return NO_THINK + "\n"
            + "Question:\n" + request.question + "\n\n"
            + "Context:\n" + request.context.packedForModel() + "\n\n"
            + "Answer using only the context above.";
  }

  @NonNull
  public static String memorySystemPrompt() {
    return "You are MithraX. Extract durable memories from meeting transcripts. "
            + "Respond with JSON only: {\"memories\":[{\"type\":\"ACTION|DEADLINE|FACT|PERSON|DECISION|PREFERENCE|CONTEXT\","
            + "\"content\":\"...\",\"confidence\":0.0,\"metadata\":{\"owner\":\"\",\"task\":\"\",\"deadline\":\"\"}}]}. "
            + "Do not invent facts.";
  }

  @NonNull
  public static String memoryUserPrompt(@NonNull MemoryExtractionRequest request) {
    return NO_THINK + "\n"
            + "Transcript:\n" + request.transcript + "\n\n"
            + "Existing summary:\n" + request.insights.summary;
  }
}
