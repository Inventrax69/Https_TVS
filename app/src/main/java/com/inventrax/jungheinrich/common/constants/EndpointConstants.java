package com.inventrax.jungheinrich.common.constants;

/**
 * Created by Prasanna.ch on 05/31/2018.
 */

public enum  EndpointConstants {
    None, LoginUserDTO, ProfileDTO,Inbound,PutAwayDTO,Inventory,Exception,CycleCount,Outbound, DenestingDTO,HouseKeepingDTO,ScanDTO,StockTakeDTO,VNA;
    public enum ScanType { Unloading, Putaway, Picking, Loading, DeNesting, Assortment };
}