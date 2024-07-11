package com.inventrax.tvs.room;

import android.database.Cursor;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public final class StockTakeDAO_Impl implements StockTakeDAO {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfStockTakeTable;

  private final EntityDeletionOrUpdateAdapter __deletionAdapterOfStockTakeTable;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfStockTakeTable;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfUpdate;

  public StockTakeDAO_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfStockTakeTable = new EntityInsertionAdapter<StockTakeTable>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `StockTakeTable`(`id`,`bin`,`carton`,`sku`,`qty`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, StockTakeTable value) {
        stmt.bindLong(1, value.id);
        if (value.bin == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.bin);
        }
        if (value.carton == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.carton);
        }
        if (value.sku == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.sku);
        }
        if (value.qty == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.qty);
        }
      }
    };
    this.__deletionAdapterOfStockTakeTable = new EntityDeletionOrUpdateAdapter<StockTakeTable>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `StockTakeTable` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, StockTakeTable value) {
        stmt.bindLong(1, value.id);
      }
    };
    this.__updateAdapterOfStockTakeTable = new EntityDeletionOrUpdateAdapter<StockTakeTable>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `StockTakeTable` SET `id` = ?,`bin` = ?,`carton` = ?,`sku` = ?,`qty` = ? WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, StockTakeTable value) {
        stmt.bindLong(1, value.id);
        if (value.bin == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.bin);
        }
        if (value.carton == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.carton);
        }
        if (value.sku == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.sku);
        }
        if (value.qty == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.qty);
        }
        stmt.bindLong(6, value.id);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM StockTakeTable";
        return _query;
      }
    };
    this.__preparedStmtOfUpdate = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE StockTakeTable SET qty =? WHERE bin=? and carton=? and sku=?";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(List<StockTakeTable> stockTakeTables) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfStockTakeTable.insert(stockTakeTables);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(StockTakeTable stockTakeTable) {
    __db.beginTransaction();
    try {
      __insertionAdapterOfStockTakeTable.insert(stockTakeTable);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(StockTakeTable stockTakeTable) {
    __db.beginTransaction();
    try {
      __deletionAdapterOfStockTakeTable.handle(stockTakeTable);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(StockTakeTable stockTakeTable) {
    __db.beginTransaction();
    try {
      __updateAdapterOfStockTakeTable.handle(stockTakeTable);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public void update(String bin, String carton, String sku, String qty) {
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdate.acquire();
    __db.beginTransaction();
    try {
      int _argIndex = 1;
      if (qty == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, qty);
      }
      _argIndex = 2;
      if (bin == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, bin);
      }
      _argIndex = 3;
      if (carton == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, carton);
      }
      _argIndex = 4;
      if (sku == null) {
        _stmt.bindNull(_argIndex);
      } else {
        _stmt.bindString(_argIndex, sku);
      }
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfUpdate.release(_stmt);
    }
  }

  @Override
  public List<StockTakeTable> getAll() {
    final String _sql = "SELECT * FROM StockTakeTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfBin = _cursor.getColumnIndexOrThrow("bin");
      final int _cursorIndexOfCarton = _cursor.getColumnIndexOrThrow("carton");
      final int _cursorIndexOfSku = _cursor.getColumnIndexOrThrow("sku");
      final int _cursorIndexOfQty = _cursor.getColumnIndexOrThrow("qty");
      final List<StockTakeTable> _result = new ArrayList<StockTakeTable>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final StockTakeTable _item;
        final String _tmpBin;
        _tmpBin = _cursor.getString(_cursorIndexOfBin);
        final String _tmpCarton;
        _tmpCarton = _cursor.getString(_cursorIndexOfCarton);
        final String _tmpSku;
        _tmpSku = _cursor.getString(_cursorIndexOfSku);
        final String _tmpQty;
        _tmpQty = _cursor.getString(_cursorIndexOfQty);
        _item = new StockTakeTable(_tmpBin,_tmpCarton,_tmpSku,_tmpQty);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<String> getLocations() {
    final String _sql = "SELECT bin FROM StockTakeTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final List<String> _result = new ArrayList<String>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final String _item;
        _item = _cursor.getString(0);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public int getAllItemsAllCount() {
    final String _sql = "SELECT COUNT(*) FROM StockTakeTable";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _result;
      if(_cursor.moveToFirst()) {
        _result = _cursor.getInt(0);
      } else {
        _result = 0;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public StockTakeTable getPreviousRecord(String bin, String carton, String sku) {
    final String _sql = "SELECT * FROM StockTakeTable WHERE bin=? and carton=? and sku=?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    if (bin == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, bin);
    }
    _argIndex = 2;
    if (carton == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, carton);
    }
    _argIndex = 3;
    if (sku == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, sku);
    }
    final Cursor _cursor = __db.query(_statement);
    try {
      final int _cursorIndexOfId = _cursor.getColumnIndexOrThrow("id");
      final int _cursorIndexOfBin = _cursor.getColumnIndexOrThrow("bin");
      final int _cursorIndexOfCarton = _cursor.getColumnIndexOrThrow("carton");
      final int _cursorIndexOfSku = _cursor.getColumnIndexOrThrow("sku");
      final int _cursorIndexOfQty = _cursor.getColumnIndexOrThrow("qty");
      final StockTakeTable _result;
      if(_cursor.moveToFirst()) {
        final String _tmpBin;
        _tmpBin = _cursor.getString(_cursorIndexOfBin);
        final String _tmpCarton;
        _tmpCarton = _cursor.getString(_cursorIndexOfCarton);
        final String _tmpSku;
        _tmpSku = _cursor.getString(_cursorIndexOfSku);
        final String _tmpQty;
        _tmpQty = _cursor.getString(_cursorIndexOfQty);
        _result = new StockTakeTable(_tmpBin,_tmpCarton,_tmpSku,_tmpQty);
        _result.id = _cursor.getInt(_cursorIndexOfId);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
