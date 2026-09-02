package com.gamor.mithrax.data.local.conversation;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;

import com.gamor.mithrax.data.local.AppDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConversationMigrationTest {

    @Test
    public void migratesRecordingsRowsIntoConversations() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File dbFile = context.getDatabasePath("mithrax-migration.db");
        if (dbFile.exists()) {
            assertTrue(dbFile.delete());
        }

        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name("mithrax-migration.db")
                        .callback(new SupportSQLiteOpenHelper.Callback(2) {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                db.execSQL(
                                        "CREATE TABLE recordings ("
                                                + "id TEXT NOT NULL, "
                                                + "title TEXT NOT NULL, "
                                                + "createdAtMillis INTEGER NOT NULL, "
                                                + "durationMillis INTEGER NOT NULL, "
                                                + "localPath TEXT NOT NULL, "
                                                + "processingStatus TEXT NOT NULL, "
                                                + "transcript TEXT NOT NULL, "
                                                + "PRIMARY KEY(id))"
                                );
                                db.execSQL(
                                        "INSERT INTO recordings "
                                                + "(id, title, createdAtMillis, durationMillis, localPath, "
                                                + "processingStatus, transcript) VALUES "
                                                + "('rec-1', 'Weekly sync', 1234, 56000, '/files/rec-1.m4a', "
                                                + "'COMPLETED', 'hello transcript')"
                                );
                            }

                            @Override
                            public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            }
                        })
                        .build();
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(configuration);
        SupportSQLiteDatabase sqlite = helper.getWritableDatabase();
        AppDatabase.MIGRATION_2_3.migrate(sqlite);

        Cursor conversations = sqlite.query("SELECT id, title, createdAtMillis, updatedAtMillis, "
                + "durationMillis, audioPath, transcript, processingStatus, summary, keyPoints, "
                + "actionItems, importantFacts, metadata FROM conversations");
        assertTrue(conversations.moveToFirst());
        assertEquals("rec-1", conversations.getString(0));
        assertEquals("Weekly sync", conversations.getString(1));
        assertEquals(1234L, conversations.getLong(2));
        assertEquals(1234L, conversations.getLong(3));
        assertEquals(56000L, conversations.getLong(4));
        assertEquals("/files/rec-1.m4a", conversations.getString(5));
        assertEquals("hello transcript", conversations.getString(6));
        assertEquals("COMPLETED", conversations.getString(7));
        assertEquals("", conversations.getString(8));
        assertEquals("[]", conversations.getString(9));
        assertEquals("[]", conversations.getString(10));
        assertEquals("[]", conversations.getString(11));
        assertEquals("{}", conversations.getString(12));
        conversations.close();

        Cursor leftover = sqlite.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='recordings'");
        assertFalse(leftover.moveToFirst());
        leftover.close();
        sqlite.close();
        helper.close();
    }
}
