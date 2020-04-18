package com.knaanstudio.coronawise.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "MacAddressTable")
public class MacAddressInstance {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "macAddress")
    String macAddress;

    public MacAddressInstance(String macAddress){
        this.macAddress=macAddress;
    }

    public String getMacAddress(){
        return this.macAddress;
    }

}
