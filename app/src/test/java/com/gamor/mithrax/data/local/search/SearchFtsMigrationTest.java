package com.gamor.mithrax.data.local.search;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SearchFtsMigrationTest {

    @Test
    public void migratesV4DatabaseToFtsWithoutPrefixOption() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File dbFile = context.getDatabasePath("mithrax-fts-migration.db");
        if (dbFile.exists()) {
            assertTrue(dbFile.delete());
        }

        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name("mithrax-fts-migration.db")
                        .callback(new SupportSQLiteOpenHelper.Callback(4) {
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
                                        "CREATE TABLE memories ("
                                                + "id TEXT NOT NULL, "
                                                + "type TEXT NOT NULL, "
                                                + "content TEXT NOT NULL, "
                                                + "sourceConversationId TEXT NOT NULL, "
                                                + "createdAtMillis INTEGER NOT NULL, "
                                                + "confidence REAL NOT NULL, "
                                                + "metadata TEXT NOT NULL, "
                                                + "deleted INTEGER NOT NULL, "
                                                + "PRIMARY KEY(id))"
                                );
                                db.execSQL(
                                        "INSERT INTO conversations VALUES ("
                                                + "'c1', 'Sync', 1, 1, 0, '', 'payment API', 'COMPLETED', "
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
        AppDatabase.MIGRATION_4_5.migrate(sqlite);

        Cursor sql = sqlite.query(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='conversation_fts'");
        assertTrue(sql.moveToFirst());
        String createSql = sql.getString(0);
        sql.close();
        assertFalse(createSql.contains("prefix"));

        Cursor hit = sqlite.query(
                "SELECT sourceId FROM conversation_fts WHERE conversation_fts MATCH 'payment'");
        assertTrue(hit.moveToFirst());
        hit.close();
        sqlite.close();
        helper.close();
    }
}
