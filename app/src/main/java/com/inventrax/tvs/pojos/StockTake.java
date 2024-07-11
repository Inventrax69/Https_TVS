package com.inventrax.tvs.pojos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

/**
 * Created by karthik.m on 06/12/2018.
 */


public class StockTake {


    @SerializedName("StockTake")
    private  List<StockTakeDetails> stockTakeDetails ;

    public StockTake(Set<? extends Map.Entry<?, ?>> entries)
    {

        for(Map.Entry<?, ?> entry : entries)
        {

            switch(entry.getKey().toString())
            {


                case "StockTake":
                    if(entry.getValue()!=null) {
                        List<LinkedTreeMap<?,?>> treeMaps=(List<LinkedTreeMap<?,?>>)entry.getValue();
                        List<StockTakeDetails> lstDetials=new ArrayList<StockTakeDetails>();
                        for(int i=0;i<treeMaps.size();i++)
                        {
                            StockTakeDetails dto=new StockTakeDetails(treeMaps.get(i).entrySet());
                            lstDetials.add(dto);
                            //Log.d("Message", core.getEntityObject().toString());

                        }

                        this.setStockTakeDetails(lstDetials);
                    }

                    break;

            }

        }

    }

    public StockTake() {

    }

    public List<StockTakeDetails> getStockTakeDetails() {
        return stockTakeDetails;
    }

    public void setStockTakeDetails(List<StockTakeDetails> stockTakeDetails) {
        this.stockTakeDetails = stockTakeDetails;
    }
}
