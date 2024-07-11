package com.inventrax.jungheinrich.room;



import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StockTakeDAO {

    @Query("SELECT * FROM StockTakeTable")
    List<StockTakeTable> getAll();

   /* @Query("SELECT * FROM StockTakeTable WHERE divisionID IN (SELECT divisionID FROM CustomerTable WHERE customerId=:customerId)")
    List<StockTakeTable> getAllByCustomer(String customerId);*/


    @Query("SELECT bin FROM StockTakeTable")
    List<String> getLocations();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StockTakeTable> stockTakeTables);

    @Delete
    void delete(StockTakeTable stockTakeTable);

    @Query("DELETE FROM StockTakeTable")
    void deleteAll();

    @Update
    void update(StockTakeTable stockTakeTable);

    @Query("UPDATE StockTakeTable SET qty =:qty WHERE bin=:bin and carton=:carton and sku=:sku")
    void update(String bin, String carton, String sku, String qty);


    @Query("SELECT COUNT(*) FROM StockTakeTable")
    int getAllItemsAllCount();

    // Master data updates
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    void insert(StockTakeTable stockTakeTable);

    @Query("SELECT * FROM StockTakeTable WHERE bin=:bin and carton=:carton and sku=:sku")
    StockTakeTable getPreviousRecord(String bin, String carton, String sku);

}
