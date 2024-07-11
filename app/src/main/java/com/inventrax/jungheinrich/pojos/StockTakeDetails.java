package com.inventrax.jungheinrich.pojos;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Set;

public class StockTakeDetails {

    @SerializedName("CartonCode")
    public String CartonCode ;
    @SerializedName("LocationCode")
    public String LocationCode ;
    @SerializedName("MaterialCode")
    public String MaterialCode ;
    @SerializedName("Quantity")
    public String Quantity ;

    public StockTakeDetails(){

    }

    public StockTakeDetails(Set<? extends Map.Entry<?, ?>> entries) {
        for (Map.Entry<?, ?> entry : entries) {

            switch (entry.getKey().toString()) {

                case "CartonCode":
                    if (entry.getValue() != null) {
                        this.setCartonCode(entry.getValue().toString());
                    }
                    break;
                case "LocationCode":
                    if (entry.getValue() != null) {
                        this.setLocationCode(entry.getValue().toString());
                    }
                    break;
                case "MaterialCode":
                    if (entry.getValue() != null) {
                        this.setMaterialCode(entry.getValue().toString());
                    }
                    break;
                case "Quantity":
                    if (entry.getValue() != null) {
                        this.setQuantity(entry.getValue().toString());
                    }
                    break;

            }

        }
    }


    public String getCartonCode() {
        return CartonCode;
    }

    public void setCartonCode(String cartonCode) {
        CartonCode = cartonCode;
    }

    public String getLocationCode() {
        return LocationCode;
    }

    public void setLocationCode(String locationCode) {
        LocationCode = locationCode;
    }

    public String getMaterialCode() {
        return MaterialCode;
    }

    public void setMaterialCode(String materialCode) {
        MaterialCode = materialCode;
    }

    public String getQuantity() {
        return Quantity;
    }

    public void setQuantity(String quantity) {
        Quantity = quantity;
    }
}
