package com.gamor.mithrax.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.gamor.mithrax.data.local.conversation.ConversationConverters;
import com.gamor.mithrax.data.local.conversation.ConversationDao;
import com.gamor.mithrax.data.local.conversation.ConversationEntity;
import com.gamor.mithrax.data.local.memory.MemoryDao;
import com.gamor.mithrax.data.local.memory.MemoryEntity;
import com.gamor.mithrax.data.local.search.ConversationFtsEntity;
import com.gamor.mithrax.data.local.search.MemoryFtsEntity;
import com.gamor.mithrax.data.local.search.SearchDao;

@Database(
        entities = {
                ConversationEntity.class,
                MemoryEntity.class,
                ConversationFtsEntity.class,
                MemoryFtsEntity.class
        },
        version = 5,
        exportSchema = false
)
@TypeConverters({ConversationConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE recordings ADD COLUMN transcript TEXT NOT NULL DEFAULT ''");
            database.execSQL(
                    "UPDATE recordings SET processingStatus = 'COMPLETED' WHERE processingStatus = 'RECORDED'");
        }
    };

    /**
     * Moves conversation fields off the recordings table so the record can exist
     * without a raw audio file. Existing audio paths are copied as references.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS conversations ("
                            + "id TEXT NOT NULL, "
                            + "title TEXT NOT NULL, "
                            + "createdAtMillis INTEGER NOT NULL, "
                            + "updatedAtMillis INTEGER NOT NULL, "
                            + "durationMillis INTEGER NOT NULL, "
                            + "audioPath TEXT NOT NULL, "
                            + "transcript TEXT NOT NULL, "
                            + "processingStatus TEXT NOT NULL, "
                            + "summary TEXT NOT NULL, "
                            + "keyPoints TEXT NOT NULL, "
                            + "actionItems TEXT NOT NULL, "
                            + "importantFacts TEXT NOT NULL, "
                            + "metadata TEXT NOT NULL, "
                            + "PRIMARY KEY(id)"
                            + ")"
            );
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_createdAtMillis "
                    + "ON conversations(createdAtMillis)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_processingStatus "
                    + "ON conversations(processingStatus)");
            database.execSQL(
                    "INSERT INTO conversations ("
                            + "id, title, createdAtMillis, updatedAtMillis, durationMillis, "
                            + "audioPath, transcript, processingStatus, summary, keyPoints, "
                            + "actionItems, importantFacts, metadata) "
                            + "SELECT id, title, createdAtMillis, createdAtMillis, durationMillis, "
                            + "localPath, transcript, processingStatus, '', '[]', '[]', '[]', '{}' "
                            + "FROM recordings"
            );
            database.execSQL("DROP TABLE recordings");
        }
    };

    /**
     * Adds a local memories table linked to conversations. Embeddings stay out of this table.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS memories ("
                            + "id TEXT NOT NULL, "
                            + "type TEXT NOT NULL, "
                            + "content TEXT NOT NULL, "
                            + "sourceConversationId TEXT NOT NULL, "
                            + "createdAtMillis INTEGER NOT NULL, "
                            + "confidence REAL NOT NULL, "
                            + "metadata TEXT NOT NULL, "
                            + "deleted INTEGER NOT NULL, "
                            + "PRIMARY KEY(id), "
                            + "FOREIGN KEY(sourceConversationId) REFERENCES conversations(id) "
                            + "ON DELETE CASCADE"
                            + ")"
            );
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_sourceConversationId "
                    + "ON memories(sourceConversationId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_type ON memories(type)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_deleted ON memories(deleted)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_memories_createdAtMillis "
                    + "ON memories(createdAtMillis)");
        }
    };

    /**
     * Adds local FTS4 indexes for conversation and memory text. No embeddings.
     * Prefix indexes are omitted: Android's SQLite rejects {@code prefix=2,3,4}.
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // A previous attempt may have auto-committed a broken FTS virtual table.
            database.execSQL("DROP TABLE IF EXISTS `conversation_fts`");
            database.execSQL("DROP TABLE IF EXISTS `memory_fts`");
            database.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `conversation_fts` USING FTS4("
                            + "`sourceId`, `title`, `transcript`, `summary`, `insights`, "
                            + "`metadataText`, tokenize=porter, notindexed=`sourceId`)"
            );
            database.execSQL(
                    "INSERT INTO conversation_fts("
                            + "sourceId, title, transcript, summary, insights, metadataText) "
                            + "SELECT id, title, transcript, summary, "
                            + "keyPoints || ' ' || actionItems || ' ' || importantFacts, metadata "
                            + "FROM conversations"
            );
            database.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `memory_fts` USING FTS4("
                            + "`sourceId`, `sourceConversationId`, `body`, tokenize=porter, "
                            + "notindexed=`sourceId`, notindexed=`sourceConversationId`)"
            );
            database.execSQL(
                    "INSERT INTO memory_fts(sourceId, sourceConversationId, body) "
                            + "SELECT id, sourceConversationId, type || ' ' || content || ' ' || metadata "
                            + "FROM memories WHERE deleted = 0"
            );
        }
    };

    public abstract ConversationDao conversationDao();

    public abstract MemoryDao memoryDao();

    public abstract SearchDao searchDao();

    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "mithrax.db"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build();
                }
            }
        }
        return instance;
    }
}
