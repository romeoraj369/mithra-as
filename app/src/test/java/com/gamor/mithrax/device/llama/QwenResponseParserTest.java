package com.gamor.mithrax.device.llama;

import com.gamor.mithrax.domain.conversation.ActionItem;
import com.gamor.mithrax.domain.memory.ExtractedMemory;
import com.gamor.mithrax.domain.memory.MemoryType;
import com.gamor.mithrax.domain.understanding.ConversationInsights;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class QwenResponseParserTest {

  @Test
  public void parsesStructuredSummarizationJson() throws Exception {
    String raw = "{"
            + "\"summary\":\"Client wants API by Friday.\","
            + "\"keyPoints\":[\"API deadline is Friday\"],"
            + "\"actionItems\":[{\"text\":\"Prepare API\",\"completed\":false}],"
            + "\"importantFacts\":[\"Client deadline Friday\"]"
            + "}";
    ConversationInsights insights = QwenResponseParser.parseInsights(raw);
    assertEquals("Client wants API by Friday.", insights.summary);
    assertEquals(1, insights.keyPoints.size());
    assertEquals(1, insights.actionItems.size());
    assertEquals("Prepare API", insights.actionItems.get(0).text);
    assertFalse(insights.actionItems.get(0).completed);
  }

  @Test
  public void stripsThinkingWrapperBeforeParsingAnswer() throws Exception {
    String raw = "Some analysis" + "</" + "think>\nFriday.";
    String answer = QwenResponseParser.parseAnswer(raw);
    assertEquals("Friday.", answer);
  }

  @Test
  public void detectsNotFoundAnswers() {
    assertTrue(QwenResponseParser.isNotFoundAnswer("I could not find that in your memories."));
    assertFalse(QwenResponseParser.isNotFoundAnswer("The migration is next Wednesday."));
  }

  @Test
  public void parsesMemoryExtractionPayload() throws Exception {
    String raw = "{\"memories\":[{\"type\":\"ACTION\",\"content\":\"Raj will implement payment API by Friday.\","
            + "\"confidence\":0.9,\"metadata\":{\"owner\":\"Raj\",\"task\":\"implement payment API\","
            + "\"deadline\":\"Friday\"}}]}";
    List<ExtractedMemory> memories = QwenResponseParser.parseMemories(raw);
    assertEquals(1, memories.size());
    ExtractedMemory memory = memories.get(0);
    assertEquals(MemoryType.ACTION, memory.type);
    assertEquals("Raj", memory.metadata.get("owner"));
    assertEquals("Friday", memory.metadata.get("deadline"));
  }
}
