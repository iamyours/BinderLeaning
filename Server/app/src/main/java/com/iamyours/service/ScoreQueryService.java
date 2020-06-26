package com.iamyours.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.iamyours.bean.Student;

import java.util.HashMap;
import java.util.Map;

public class ScoreQueryService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ScoreStub();
    }

    private static class ScoreStub extends Binder {
        private static final int TRANSACTION_query = 1;
        private Map<String, Integer> scoreMap = new HashMap<>();//模拟数据查询

        public ScoreStub() {
            scoreMap.put("张三", 100);
            scoreMap.put("李四", 89);
            scoreMap.put("王五", 60);
        }

        @Override
        protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
            if (code == TRANSACTION_query) {
                data.enforceInterface("ScoreQuery");
                Student student = data.readParcelable(Student.class.getClassLoader());
                int score = query(student);
                Log.e("Server","query:"+student+",result:"+score);
                reply.writeInt(score);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        private int query(Student s) {
            Integer score = scoreMap.get(s.getName());
            return score != null ? score : -1;
        }
    }
}
