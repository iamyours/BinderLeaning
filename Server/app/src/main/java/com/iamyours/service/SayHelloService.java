package com.iamyours.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.iamyours.aidl.ISayHello;

public class SayHelloService extends Service {
    private static final String TAG = "Server";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }

    private ISayHello.Stub stub = new ISayHello.Stub() {
        @Override
        public void sayHello() throws RemoteException {
            Log.e(TAG, "sayHello");
        }

        @Override
        public int sayHelloTo(String name) throws RemoteException {
            Log.e(TAG, "sayHello to " + name);
            return name.length();
        }
    };
}
