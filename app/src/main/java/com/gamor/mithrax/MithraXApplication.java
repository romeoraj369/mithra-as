package com.gamor.mithrax;

import android.app.Application;

import com.gamor.mithrax.data.local.AppDatabase;
import com.gamor.mithrax.data.local.conversation.ConversationRepository;
import com.gamor.mithrax.data.local.memory.MemoryRepository;
import com.gamor.mithrax.data.local.ask.AskRepository;
import com.gamor.mithrax.data.local.ask.LocalAskService;
import com.gamor.mithrax.data.local.ask.RoomPassageLoader;
import com.gamor.mithrax.data.local.privacy.LocalPrivacyStore;
import com.gamor.mithrax.data.local.privacy.PrivacyRepository;
import com.gamor.mithrax.data.local.search.FtsSearchService;
import com.gamor.mithrax.data.local.search.LocalSearchIndex;
import com.gamor.mithrax.data.local.search.SearchRepository;
import com.gamor.mithrax.device.ai.QwenLocalLanguageModel;
import com.gamor.mithrax.domain.ai.LocalLanguageModel;
import com.gamor.mithrax.device.audio.MicrophoneLease;
import com.gamor.mithrax.device.memory.LlmMemoryExtractor;
import com.gamor.mithrax.device.memory.MemoryExtractionController;
import com.gamor.mithrax.device.recording.RecordingSession;
import com.gamor.mithrax.data.local.AppPreferences;
import com.gamor.mithrax.device.stt.TranscriptionController;
import com.gamor.mithrax.device.stt.VoskModelManager;
import com.gamor.mithrax.device.stt.VoskSpeechToTextEngine;
import com.gamor.mithrax.device.tts.AndroidTextToSpeechEngine;
import com.gamor.mithrax.device.tts.SpeechPlaybackController;
import com.gamor.mithrax.device.understanding.SummarizationController;
import com.gamor.mithrax.domain.tts.TextToSpeechEngine;
import com.gamor.mithrax.domain.search.SearchService;

/**
 * Process-wide state. All processing is intended to stay on-device.
 */
public class MithraXApplication extends Application {

    private VoskModelManager voskModelManager;
    private ConversationRepository conversationRepository;
    private MemoryRepository memoryRepository;
    private SearchRepository searchRepository;
    private AskRepository askRepository;
    private PrivacyRepository privacyRepository;
    private RecordingSession recordingSession;
    private MicrophoneLease microphoneLease;
    private TranscriptionController transcriptionController;
    private SummarizationController summarizationController;
    private MemoryExtractionController memoryExtractionController;
    private SpeechPlaybackController speechPlaybackController;
    private QwenLocalLanguageModel languageModel;

    @Override
    public void onCreate() {
        super.onCreate();
        voskModelManager = new VoskModelManager();
        AppDatabase database = AppDatabase.getInstance(this);
        LocalSearchIndex searchIndex = new LocalSearchIndex(database.searchDao());
        conversationRepository = new ConversationRepository(database.conversationDao(), searchIndex);
        memoryRepository = new MemoryRepository(database.memoryDao(), searchIndex);
        SearchService searchService = new FtsSearchService(
                database.searchDao(),
                database.conversationDao(),
                database.memoryDao()
        );
        searchRepository = new SearchRepository(searchService);
        recordingSession = new RecordingSession();
        microphoneLease = new MicrophoneLease();
        languageModel = new QwenLocalLanguageModel(this);
        memoryExtractionController = new MemoryExtractionController(
                conversationRepository,
                memoryRepository,
                new LlmMemoryExtractor(languageModel)
        );
        summarizationController = new SummarizationController(
                conversationRepository,
                languageModel,
                memoryExtractionController
        );
        askRepository = new AskRepository(new LocalAskService(
                searchService,
                new RoomPassageLoader(conversationRepository, memoryRepository),
                languageModel
        ));
        privacyRepository = new PrivacyRepository(new LocalPrivacyStore(
                this,
                conversationRepository,
                memoryRepository,
                voskModelManager,
                languageModel
        ));
        transcriptionController = new TranscriptionController(
                conversationRepository,
                new VoskSpeechToTextEngine(this, voskModelManager),
                summarizationController
        );
        transcriptionController.resumeInterrupted();
        summarizationController.resumeInterrupted();
        TextToSpeechEngine ttsEngine = new AndroidTextToSpeechEngine(this);
        speechPlaybackController = new SpeechPlaybackController(
                ttsEngine,
                () -> AppPreferences.spokenRepliesEnabled(this)
        );
    }

    public VoskModelManager getVoskModelManager() {
        return voskModelManager;
    }

    public ConversationRepository getConversationRepository() {
        return conversationRepository;
    }

    public MemoryRepository getMemoryRepository() {
        return memoryRepository;
    }

    public SearchRepository getSearchRepository() {
        return searchRepository;
    }

    public AskRepository getAskRepository() {
        return askRepository;
    }

    public PrivacyRepository getPrivacyRepository() {
        return privacyRepository;
    }

    public RecordingSession getRecordingSession() {
        return recordingSession;
    }

    public MicrophoneLease getMicrophoneLease() {
        return microphoneLease;
    }

    public TranscriptionController getTranscriptionController() {
        return transcriptionController;
    }

    public SummarizationController getSummarizationController() {
        return summarizationController;
    }

    public MemoryExtractionController getMemoryExtractionController() {
        return memoryExtractionController;
    }

    public SpeechPlaybackController getSpeechPlaybackController() {
        return speechPlaybackController;
    }

    @Override
    public void onTerminate() {
        if (speechPlaybackController != null) {
            speechPlaybackController.shutdown();
        }
        if (languageModel != null) {
            languageModel.shutdown();
        }
        super.onTerminate();
    }
}
