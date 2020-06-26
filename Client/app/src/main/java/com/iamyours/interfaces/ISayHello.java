package com.iamyours.interfaces;

import com.iamyours.annotations.AIDL;
import com.iamyours.bean.Student;

@AIDL
public interface ISayHello {
    void sayHello();
    int sayHelloTo(String name);
    int query(Student s);
}
