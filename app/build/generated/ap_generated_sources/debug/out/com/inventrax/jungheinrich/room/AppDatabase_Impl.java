package com.inventrax.jungheinrich.room;

import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public final class AppDatabase_Impl extends AppDatabase {
  private volatile StockTakeDAO _stockTakeDAO;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `StockTakeTable` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bin` TEXT, `carton` TEXT, `sku` TEXT, `qty` TEXT)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, \"077deeab52e21730d67e4b1d87da62f4\")");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `StockTakeTable`");
      }

      @Override
      protected void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      protected void validateMigration(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsStockTakeTable = new HashMap<String, TableInfo.Column>(5);
        _columnsStockTakeTable.put("id", new TableInfo.Column("id", "INTEGER", true, 1));
        _columnsStockTakeTable.put("bin", new TableInfo.Column("bin", "TEXT", false, 0));
        _columnsStockTakeTable.put("carton", new TableInfo.Column("carton", "TEXT", false, 0));
        _columnsStockTakeTable.put("sku", new TableInfo.Column("sku", "TEXT", false, 0));
        _columnsStockTakeTable.put("qty", new TableInfo.Column("qty", "TEXT", false, 0));
        final HashSet<TableInfo.ForeignKey> _foreignKeysStockTakeTable = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesStockTakeTable = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoStockTakeTable = new TableInfo("StockTakeTable", _columnsStockTakeTable, _foreignKeysStockTakeTable, _indicesStockTakeTable);
        final TableInfo _existingStockTakeTable = TableInfo.read(_db, "StockTakeTable");
        if (! _infoStockTakeTable.equals(_existingStockTakeTable)) {
          throw new IllegalStateException("Migration didn't properly handle StockTakeTable(com.inventrax.jungheinrich.room.StockTakeTable).\n"
                  + " Expected:\n" + _infoStockTakeTable + "\n"
                  + " Found:\n" + _existingStockTakeTable);
        }
      }
    }, "077deeab52e21730d67e4b1d87da62f4", "f5857d7f95690b3cdd161081b4b3d849");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    return new InvalidationTracker(this, "StockTakeTable");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `StockTakeTable`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  public StockTakeDAO getStockTakeDAO() {
    if (_stockTakeDAO != null) {
      return _stockTakeDAO;
    } else {
      synchronized(this) {
        if(_stockTakeDAO == null) {
          _stockTakeDAO = new StockTakeDAO_Impl(this);
        }
        return _stockTakeDAO;
      }
    }
  }
}
