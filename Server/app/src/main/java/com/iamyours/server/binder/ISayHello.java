package com.iamyours.server.binder;

import com.iamyours.bean.Student;

public interface ISayHello {
    void sayHello();
    int sayHelloTo(String name);
    int query(Student s);
}