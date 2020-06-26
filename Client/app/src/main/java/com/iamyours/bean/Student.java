package com.iamyours.bean;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Student implements Parcelable {
    public String name;

    public Student(String name) {
        this.name = name;
    }

    protected Student(Parcel in) {
        name = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Student> CREATOR = new Creator<Student>() {
        @Override
        public Student createFromParcel(Parcel in) {
            return new Student(in);
        }

        @Override
        public Student[] newArray(int size) {
            return new Student[size];
        }
    };
}
