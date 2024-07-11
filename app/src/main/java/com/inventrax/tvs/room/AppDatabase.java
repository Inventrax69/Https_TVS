package com.inventrax.tvs.room;


import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {StockTakeTable.class}, version = 1)
public abstract  class AppDatabase extends RoomDatabase {

    public abstract StockTakeDAO getStockTakeDAO();

}
