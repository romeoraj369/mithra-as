package com.gamor.mithrax.data.local.memory;

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
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MemoryMigrationTest {

    @Test
    public void migratesV3DatabaseByAddingMemoriesTable() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File dbFile = context.getDatabasePath("mithrax-memory-migration.db");
        if (dbFile.exists()) {
            assertTrue(dbFile.delete());
        }

        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name("mithrax-memory-migration.db")
                        .callback(new SupportSQLiteOpenHelper.Callback(3) {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                db.execSQL(
                                        "CREATE TABLE conversations ("
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
                                                + "PRIMARY KEY(id))"
                                );
                                db.execSQL(
                                        "INSERT INTO conversations VALUES ("
                                                + "'c1', 'Sync', 1, 1, 0, '', 'hello', 'COMPLETED', "
                                                + "'', '[]', '[]', '[]', '{}')"
                                );
                            }

                            @Override
                            public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                            }
                        })
                        .build();
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(configuration);
        SupportSQLiteDatabase sqlite = helper.getWritableDatabase();
        AppDatabase.MIGRATION_3_4.migrate(sqlite);

        Cursor tables = sqlite.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='memories'");
        assertTrue(tables.moveToFirst());
        tables.close();

        sqlite.execSQL(
                "INSERT INTO memories (id, type, content, sourceConversationId, createdAtMillis, "
                        + "confidence, metadata, deleted) VALUES "
                        + "('m1', 'FACT', 'Client requested API completion by Friday.', 'c1', 2, "
                        + "0.8, '{}', 0)"
        );
        Cursor row = sqlite.query("SELECT content, sourceConversationId FROM memories WHERE id='m1'");
        assertTrue(row.moveToFirst());
        assertEquals("Client requested API completion by Friday.", row.getString(0));
        assertEquals("c1", row.getString(1));
        row.close();
        sqlite.close();
        helper.close();
    }
}
