package com.knaanstudio.coronawise.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.knaanstudio.coronawise.CoronaHandler;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static com.knaanstudio.coronawise.CoronaHandler.readContactTrace;

@Entity(tableName = "ContactTraceTable")
public class ContactTrace {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    int id;

    @ColumnInfo(name = "key")
    byte[] key;

    @ColumnInfo(name = "timeStamp")
    long timestamp;



    public ContactTrace(byte[] key){
        this.key = key;
        Date now = new java.util.Date();
        Timestamp timestamp = new Timestamp(now.getTime());
        this.timestamp= timestamp.getTime();
    }
    public ContactTrace(Key key){
        this.key = key.getEncoded();
        Date now = new java.util.Date();
        Timestamp timestamp = new Timestamp(now.getTime());
        this.timestamp= timestamp.getTime();
    }


    public int getId(){
        return this.id;
    }
    public Timestamp getTimestamp(){
        return new Timestamp(this.timestamp);
    }
    public Key getKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        return CoronaHandler.binaryToPublicKey(key);
    }



}
