package com.iamyours.server.binder;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import com.iamyours.bean.Student;
import java.lang.Override;
import java.lang.String;

public abstract class ISayHelloStub extends Binder implements ISayHello {
  private static final String DESCRIPTOR = "com.iamyours.interfaces.ISayHello";

  private static final int TRANSACTION_sayHello = android.os.IBinder.FIRST_CALL_TRANSACTION + 0;

  private static final int TRANSACTION_sayHelloTo = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;

  private static final int TRANSACTION_query = android.os.IBinder.FIRST_CALL_TRANSACTION + 2;

  @Override
  public abstract void sayHello();

  @Override
  public abstract int sayHelloTo(String var0);

  @Override
  public abstract int query(Student var0);

  @Override
  protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws
      RemoteException {
    String descriptor = DESCRIPTOR;
    switch(code){
    case TRANSACTION_sayHello:{
    data.enforceInterface(descriptor);
    this.sayHello();
    reply.writeNoException();
    return true;
    }
    case TRANSACTION_sayHelloTo:{
    data.enforceInterface(descriptor);
    String _arg0  = data.readString();
    int _result = this.sayHelloTo(_arg0);
    reply.writeNoException();
    reply.writeInt(_result);
    return true;
    }
    case TRANSACTION_query:{
    data.enforceInterface(descriptor);
    com.iamyours.bean.Student _arg0 = data.readParcelable(com.iamyours.bean.Student.class.getClassLoader());
    int _result = this.query(_arg0);
    reply.writeNoException();
    reply.writeInt(_result);
    return true;
    }
    default: {
    return super.onTransact(code, data, reply, flags);
    }
    }
  }

  private static class Proxy implements ISayHello {
    private IBinder mRemote;

    Proxy(IBinder mRemote) {
      this.mRemote = mRemote;
    }

    @Override
    public void sayHello() {
      android.os.Parcel _data = android.os.Parcel.obtain();
      android.os.Parcel _reply = android.os.Parcel.obtain();
      try{
      _data.writeInterfaceToken(DESCRIPTOR);
      mRemote.transact(TRANSACTION_sayHello, _data, _reply, 0);
      _reply.readException();
      }catch(Exception e){e.printStackTrace();}finally{
      _reply.recycle();
      _data.recycle();
      }
    }

    @Override
    public int sayHelloTo(String var0) {
      int _result = 0;
      android.os.Parcel _data = android.os.Parcel.obtain();
      android.os.Parcel _reply = android.os.Parcel.obtain();
      try{
      _data.writeInterfaceToken(DESCRIPTOR);
      _data.writeString(var0);
      mRemote.transact(TRANSACTION_sayHelloTo, _data, _reply, 0);
      _reply.readException();
      _result = _reply.readInt();}catch(Exception e){e.printStackTrace();}finally{
      _reply.recycle();
      _data.recycle();
      }
      return _result;
    }

    @Override
    public int query(Student var0) {
      int _result = 0;
      android.os.Parcel _data = android.os.Parcel.obtain();
      android.os.Parcel _reply = android.os.Parcel.obtain();
      try{
      _data.writeInterfaceToken(DESCRIPTOR);
      _data.writeParcelable(var0,0);
      mRemote.transact(TRANSACTION_query, _data, _reply, 0);
      _reply.readException();
      _result = _reply.readInt();}catch(Exception e){e.printStackTrace();}finally{
      _reply.recycle();
      _data.recycle();
      }
      return _result;
    }
  }
}
