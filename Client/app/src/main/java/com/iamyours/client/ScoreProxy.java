package com.iamyours.client;

import android.os.IBinder;
import android.os.Parcel;

import com.iamyours.bean.Student;

public class ScoreProxy {
    private IBinder mRemote;
    private static final int TRANSACTION_query = 1;

    public ScoreProxy(IBinder mRemote) {//通过IBinder对象想服务端发送数据
        this.mRemote = mRemote;
    }

    public int query(Student student) {
        Parcel _data = android.os.Parcel.obtain();
        Parcel _reply = android.os.Parcel.obtain();
        int result = -1;
        try {
            _data.writeInterfaceToken("ScoreQuery");
            _data.writeParcelable(student, 0);
            mRemote.transact(TRANSACTION_query, _data, _reply, 0);
            result = _reply.readInt();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            _reply.recycle();
            _data.recycle();
        }
        return result;
    }
}
