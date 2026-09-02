package com.gamor.mithrax.data.local.search;

import androidx.annotation.NonNull;

import com.gamor.mithrax.data.local.conversation.ConversationDao;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.memory.MemoryDao;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.domain.search.SearchHit;
import com.gamor.mithrax.domain.search.SearchQueryNormalizer;
import com.gamor.mithrax.domain.search.SearchResults;
import com.gamor.mithrax.domain.search.SearchService;
import com.gamor.mithrax.domain.search.SearchSnippet;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline full-text search over Room/SQLite FTS4. Replace this class later for vector retrieval.
 */
public class FtsSearchService implements SearchService {

    private final SearchDao searchDao;
    private final ConversationDao conversationDao;
    private final MemoryDao memoryDao;

    public FtsSearchService(@NonNull SearchDao searchDao,
                            @NonNull ConversationDao conversationDao,
                            @NonNull MemoryDao memoryDao) {
        this.searchDao = searchDao;
        this.conversationDao = conversationDao;
        this.memoryDao = memoryDao;
    }

    @NonNull
    @Override
    public SearchResults search(@NonNull String query) {
        SearchQueryNormalizer.ParsedQuery parsed = SearchQueryNormalizer.parse(query);
        if (parsed.isEmpty()) {
            return empty();
        }
        List<ConversationEntity> conversations = loadConversations(parsed.ftsMatch);
        List<MemoryEntity> memories = loadMemories(parsed.ftsMatch);
        return new SearchResults(
                toConversationHits(conversations, parsed.tokens),
                toMemoryHits(memories, parsed.tokens)
        );
    }

    @NonNull
    private List<ConversationEntity> loadConversations(@NonNull String matchQuery) {
        List<String> ids = searchDao.matchConversationIds(matchQuery);
        List<ConversationEntity> results = new ArrayList<>();
        if (ids == null) {
            return results;
        }
        for (String id : ids) {
            ConversationEntity entity = conversationDao.getById(id);
            if (entity != null) {
                results.add(entity);
            }
        }
        return results;
    }

    @NonNull
    private List<MemoryEntity> loadMemories(@NonNull String matchQuery) {
        List<String> ids = searchDao.matchMemoryIds(matchQuery);
        List<MemoryEntity> results = new ArrayList<>();
        if (ids == null) {
            return results;
        }
        for (String id : ids) {
            MemoryEntity entity = memoryDao.getById(id);
            if (entity != null && !entity.deleted) {
                results.add(entity);
            }
        }
        return results;
    }

    @NonNull
    private static List<SearchHit> toConversationHits(@NonNull List<ConversationEntity> conversations,
                                                      @NonNull List<String> tokens) {
        List<SearchHit> hits = new ArrayList<>();
        DateFormat dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        for (ConversationEntity conversation : conversations) {
            String snippet = firstSnippet(tokens,
                    conversation.summary,
                    conversation.transcript,
                    conversation.title);
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("kind", "conversation");
            metadata.put("status", conversation.processingStatus);
            metadata.put("createdAt", dates.format(new Date(conversation.createdAtMillis)));
            hits.add(new SearchHit(
                    SearchHit.Kind.CONVERSATION,
                    conversation.id,
                    conversation.title,
                    snippet,
                    metadata,
                    conversation.id
            ));
        }
        return hits;
    }

    @NonNull
    private static List<SearchHit> toMemoryHits(@NonNull List<MemoryEntity> memories,
                                                @NonNull List<String> tokens) {
        List<SearchHit> hits = new ArrayList<>();
        for (MemoryEntity memory : memories) {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put("kind", "memory");
            metadata.put("type", memory.type);
            metadata.put("confidence", String.valueOf(memory.confidence));
            if (memory.metadata != null) {
                metadata.putAll(memory.metadata);
            }
            String title = memory.type + " memory";
            hits.add(new SearchHit(
                    SearchHit.Kind.MEMORY,
                    memory.id,
                    title,
                    SearchSnippet.extract(memory.content, tokens, 180),
                    metadata,
                    memory.sourceConversationId
            ));
        }
        return hits;
    }

    @NonNull
    private static String firstSnippet(@NonNull List<String> tokens, String... texts) {
        for (String text : texts) {
            String snippet = SearchSnippet.extract(text, tokens, 180);
            if (!snippet.isEmpty() && containsAny(snippet, tokens)) {
                return snippet;
            }
        }
        for (String text : texts) {
            String snippet = SearchSnippet.extract(text, tokens, 180);
            if (!snippet.isEmpty()) {
                return snippet;
            }
        }
        return "";
    }

    private static boolean containsAny(@NonNull String snippet, @NonNull List<String> tokens) {
        String lower = snippet.toLowerCase();
        for (String token : tokens) {
            if (lower.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
