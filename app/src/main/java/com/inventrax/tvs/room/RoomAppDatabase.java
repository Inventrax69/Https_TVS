package com.inventrax.tvs.room;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;


public class RoomAppDatabase {

    public Context context;

    public RoomAppDatabase(Context context){

        this.context=context;
    }

    public AppDatabase getAppDatabase(){
        AppDatabase db = Room.databaseBuilder(context,
                AppDatabase.class, "stockTake")
                .allowMainThreadQueries()
                //.fallbackToDestructiveMigration()
                //.addMigrations(MIGRATION_1_3)
                .build();
        return db;
    }

    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
           // database.execSQL("ALTER TABLE VariantTable ADD COLUMN stackSize INTEGER NOT NULL DEFAULT 0");
            //database.execSQL("ALTER TABLE VariantTable ADD COLUMN packSize INTEGER NOT NULL DEFAULT 0");
        }
    };

}
