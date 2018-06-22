/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work.impl;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import androidx.work.impl.utils.Preferences;

/**
 * Migration helpers for {@link androidx.work.impl.WorkDatabase}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkDatabaseMigrations {

    private WorkDatabaseMigrations() {
        // does nothing
    }

    // Known WorkDatabase versions
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION_3 = 3;

    private static final String CREATE_SYSTEM_ID_INFO =
            "CREATE TABLE IF NOT EXISTS `SystemIdInfo` (`work_spec_id` TEXT NOT NULL, `system_id`"
                    + " INTEGER NOT NULL, PRIMARY KEY(`work_spec_id`), FOREIGN KEY(`work_spec_id`)"
                    + " REFERENCES `WorkSpec`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )";

    private static final String MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO =
            "INSERT INTO SystemIdInfo(work_spec_id, system_id) "
                    + "SELECT work_spec_id, alarm_id AS system_id FROM alarmInfo";

    private static final String REMOVE_ALARM_INFO = "DROP TABLE IF EXISTS alarmInfo";

    /**
     * Removes the {@code alarmInfo} table and substitutes it for a more general
     * {@code SystemIdInfo} table.
     * Adds implicit work tags for all work (a tag with the worker class name).
     */
    public static Migration MIGRATION_1_2 = new Migration(VERSION_1, VERSION_2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_SYSTEM_ID_INFO);
            database.execSQL(MIGRATE_ALARM_INFO_TO_SYSTEM_ID_INFO);
            database.execSQL(REMOVE_ALARM_INFO);
            database.execSQL("INSERT OR IGNORE INTO worktag(tag, work_spec_id) "
                    + "SELECT worker_class_name AS tag, id AS work_spec_id FROM workspec");
        }
    };

    /**
     * Migrates {@link WorkDatabase} version 2 to 3.
     */
    public static class Migration2To3 extends Migration {
        final Context mContext;

        public Migration2To3(@NonNull Context context) {
            super(VERSION_2, VERSION_3);
            mContext = context;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Preferences preferences = new Preferences(mContext);
            preferences.setNeedsReschedule(true);
        }
    }
}
