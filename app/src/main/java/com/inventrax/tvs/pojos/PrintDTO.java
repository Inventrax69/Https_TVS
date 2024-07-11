package com.inventrax.tvs.pojos;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class PrintDTO implements Serializable {


    @SerializedName("PrintDesc")
    private String PrintDesc;

    @SerializedName("ZPLScript")
    private String ZPLScript;

    @SerializedName("TenantID")
    private String TenantID;

    @SerializedName("Labletype")
    private String Labletype;

    @SerializedName("WareHouseID")
    private String WareHouseID;

    public PrintDTO() {
    }
    public PrintDTO(Set<? extends Map.Entry<?, ?>> entries) {
        for (Map.Entry<?, ?> entry : entries) {

            switch (entry.getKey().toString()) {

                case "PrintDesc":
                    if (entry.getValue() != null) {
                        this.setPrintDesc(entry.getValue().toString());
                    }
                    break;
                case "ZPLScript":
                    if (entry.getValue() != null) {
                        this.setZPLScript(entry.getValue().toString());
                    }
                    break;
                case "TenantID":
                    if (entry.getValue() != null) {
                        this.setTenantID(entry.getValue().toString());
                    }
                    break;
                case "Labletype":
                    if (entry.getValue() != null) {
                        this.setLabletype(entry.getValue().toString());
                    }
                    break;
                case "WareHouseID":
                    if (entry.getValue() != null) {
                        this.setWareHouseID(entry.getValue().toString());
                    }
                    break;


            }
        }
    }


    public String getPrintDesc() {
        return PrintDesc;
    }

    public void setPrintDesc(String printDesc) {
        PrintDesc = printDesc;
    }

    public String getZPLScript() {
        return ZPLScript;
    }

    public void setZPLScript(String ZPLScript) {
        this.ZPLScript = ZPLScript;
    }

    public String getTenantID() {
        return TenantID;
    }

    public void setTenantID(String tenantID) {
        TenantID = tenantID;
    }

    public String getLabletype() {
        return Labletype;
    }

    public void setLabletype(String labletype) {
        Labletype = labletype;
    }

    public String getWareHouseID() {
        return WareHouseID;
    }

    public void setWareHouseID(String wareHouseID) {
        WareHouseID = wareHouseID;
    }
}
