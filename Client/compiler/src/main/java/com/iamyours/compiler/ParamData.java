package com.iamyours.compiler;

import com.squareup.javapoet.ClassName;

public class ParamData {
    public ClassName clsName;
    public Class cls;
    public String name;


    public String getType() {//首字母大写的
        if (cls != null) return cls.toString();
        String text = clsName.toString();
        if(!"java.lang.String".equals(text)){
            text = "android.os.Parcel";
        }
        return text.substring(text.lastIndexOf("."));
    }

    public String getCapitalizeType() {
        String type = getType();
        return type.substring(0, 1).toUpperCase() + type.substring(1);
    }

    @Override
    public String toString() {
        return "ParamData{" +
                "clsName=" + clsName +
                ", cls=" + cls +
                '}';
    }
}
