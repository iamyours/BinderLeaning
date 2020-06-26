package com.iamyours.compiler;

import com.google.auto.service.AutoService;
import com.iamyours.annotations.AIDL;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.sun.tools.javac.code.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class AIDLProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<Element, List<AidlMethod>> sources = new HashMap<>();
        for (Element e : roundEnvironment.getElementsAnnotatedWith(AIDL.class)) {
            List<AidlMethod> methods = new ArrayList<>();
            sources.put(e, methods);
            List<? extends Element> list = elementUtils.getAllMembers((TypeElement) e);
            for (Element ee : list) {
                boolean isAbstract = ee.getModifiers().contains(Modifier.ABSTRACT);
                if (isAbstract) {
                    methods.add(createAidlMethod(ee));
                }
            }
        }
        generateAIDL(sources);
        return true;
    }

    private AidlMethod createAidlMethod(Element e) {
        AidlMethod aMethod = new AidlMethod();
        Type.MethodType mt = (Type.MethodType) e.asType();
        Type retType = mt.getReturnType();
        aMethod.name = e.getSimpleName() + "";
        if (retType instanceof Type.JCPrimitiveType) {
            aMethod.retCls = getPrimitiveType(retType);
        } else {
            if (!"void".equals(retType + "")) {
                aMethod.retClsName = ClassName.bestGuess(retType + "");
            }
        }

        List<Type> types = mt.getParameterTypes();
        List<ParamData> params = new ArrayList<>();
        for (Type t : types) {
            ParamData p = new ParamData();
            if (t instanceof Type.JCPrimitiveType) {
                p.cls = getPrimitiveType(t);
            } else if (t instanceof Type.ClassType) {
                Type.ClassType ct = (Type.ClassType) t;
                String cname = ct + "";
                if ("java.lang.String".equals(cname) || isParcelable(ct)) {
                    p.clsName = ClassName.bestGuess(cname);
                } else {
                    throw new RuntimeException("--unSupport param:" + t + ",in method:" + mt + " source:" + e);
                }
            } else {
                throw new RuntimeException("unSupport param:" + t + ",in method:" + mt + " source:" + e);
            }
            params.add(p);
        }
        aMethod.params = params;
        System.out.println(aMethod);
        return aMethod;
    }

    private boolean isParcelable(Type.ClassType ct) {
        for (Type t : ct.interfaces_field) {
            if ("android.os.Parcelable".equals(t.toString())) return true;
        }
        return false;
    }

    private Class getPrimitiveType(Type t) {
        Class cls = null;
        switch (t.getKind()) {
            case INT:
                cls = int.class;
                break;
            case BYTE:
                cls = byte.class;
                break;
            case LONG:
                cls = long.class;
                break;
            case DOUBLE:
                cls = double.class;
                break;
            case FLOAT:
                cls = float.class;
                break;
            case CHAR:
                cls = char.class;
                break;
        }
        if (cls == null) {
            throw new RuntimeException("unSupport type:" + t.getKind());
        }
        return cls;
    }

    TypeName iBinderType = ClassName.bestGuess("android.os.IBinder");
    TypeName stringType = ClassName.bestGuess("java.lang.String");
    TypeName binderType = ClassName.bestGuess("android.os.Binder");
    TypeName remoteExceptionType = ClassName.bestGuess("android.os.RemoteException");
    TypeName parcelType = ClassName.bestGuess("android.os.Parcel");

    private void generateAIDL(Map<Element, List<AidlMethod>> sources) {

        String descName = "DESCRIPTOR";
        for (Element e : sources.keySet()) {
            //generate stub class
            List<AidlMethod> methods = sources.get(e);
            TypeSpec.Builder builder = TypeSpec.classBuilder(e.getSimpleName() + "Stub")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
            builder.superclass(binderType);
            String pkg = String.valueOf(elementUtils.getPackageOf(e));
            String interfaceName = pkg + "." + e.getSimpleName();
            TypeName interfaceType = ClassName.bestGuess(interfaceName);
            builder.addSuperinterface(interfaceType);

            FieldSpec descriptorField = FieldSpec.builder(stringType, descName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("\"$N\"", interfaceName)
                    .build();
            builder.addField(descriptorField);
            MethodSpec asInterfaceMethod = MethodSpec.methodBuilder("asInterface")
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .addParameter(iBinderType, "iBinder")
                    .addCode("return new Proxy(iBinder);\n")
                    .returns(interfaceType)
                    .build();
            builder.addMethod(asInterfaceMethod);
            int index = 0;
            //add abstract methods for interface
            //override onTransact method and process data
            MethodSpec.Builder onTransactBuilder = MethodSpec.methodBuilder("onTransact")
                    .addAnnotation(Override.class)
                    .addParameter(int.class, "code")
                    .addParameter(parcelType, "data")
                    .addParameter(parcelType, "reply")
                    .addParameter(int.class, "flags")
                    .returns(boolean.class)
                    .addException(remoteExceptionType)
                    .addModifiers(Modifier.PROTECTED);
            StringBuilder onTransactCode = new StringBuilder();
            onTransactCode.append("String descriptor = " + descName + ";\n");
            onTransactCode.append("switch(code){\n");
            for (AidlMethod am : methods) {
                FieldSpec field = FieldSpec.builder(int.class, "TRANSACTION_" + am.name, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("android.os.IBinder.FIRST_CALL_TRANSACTION + " + (index++))
                        .build();
                builder.addField(field);
                MethodSpec.Builder mBuilder = MethodSpec.methodBuilder(am.name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.ABSTRACT);
                if (am.retClsName != null) {
                    mBuilder.returns(am.retClsName);
                } else if (am.retCls != null) {
                    mBuilder.returns(am.retCls);
                }
                int count = 0;
                onTransactCode.append("case TRANSACTION_" + am.name + ":{\n");
                onTransactCode.append("data.enforceInterface(descriptor);\n");
                StringBuilder params = new StringBuilder();
                for (ParamData p : am.params) {
                    addReadCode(onTransactCode, p, count);
                    params.append("_arg").append(count).append(",");
                    if (p.clsName != null)
                        mBuilder.addParameter(p.clsName, "var" + count);
                    else
                        mBuilder.addParameter(p.cls, "var" + count);
                    count++;
                }
                if (params.length() > 1) params.deleteCharAt(params.length() - 1);
                if (am.hasReturn()) {//返回值
                    onTransactCode.append(am.retType())
                            .append(" _result = this." + am.name)
                            .append("(")
                            .append(params).append(");\n");
                    onTransactCode.append("reply.writeNoException();\n");
                    onTransactCode.append("reply.write" + am.getRetCapitalizeType() + "(_result);\n");
                } else {
                    onTransactCode.append("this." + am.name)
                            .append("(")
                            .append(params).append(");\n");
                    onTransactCode.append("reply.writeNoException();\n");
                }
                onTransactCode.append("return true;\n").append("}\n");

                builder.addMethod(mBuilder.build());
            }
            onTransactCode.append("default: {\n")
                    .append("return super.onTransact(code, data, reply, flags);\n")
                    .append("}\n}\n");

            onTransactBuilder.addCode(onTransactCode.toString());
            builder.addMethod(onTransactBuilder.build());
            //generate proxy class
            TypeSpec.Builder proxyBuilder = TypeSpec.classBuilder("Proxy")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

            FieldSpec mRemoteField = FieldSpec.builder(
                    iBinderType,
                    "mRemote",
                    Modifier.PRIVATE
            ).build();
            proxyBuilder.addField(mRemoteField);
            // add constructor
            MethodSpec constructor = MethodSpec.constructorBuilder()
                    .addParameter(iBinderType, "mRemote")
                    .addStatement("this.$N = $N", "mRemote", "mRemote")
                    .build();
            proxyBuilder.addMethod(constructor);
            //add interface
            proxyBuilder.addSuperinterface(interfaceType);
            //override methods
            overrideProxyMethods(proxyBuilder, methods);

            builder.addType(proxyBuilder.build());

            JavaFile javaFile = JavaFile.builder(pkg, builder.build())
                    .build();

            try {
                javaFile.writeTo(filer);
            } catch (IOException iex) {
                iex.printStackTrace();
            }
        }
    }

    private void addReadCode(StringBuilder onTransactCode, ParamData p, int index) {
        String argName = " _arg" + index;
        if (p.cls != null) {//基本类型
            onTransactCode.append(p.cls + argName + " = data.read" + p.getCapitalizeType() + "();\n");
        }
        if (p.clsName != null) {
            if ("java.lang.String".equals(p.clsName.toString())) {//String
                onTransactCode.append("String" + argName + "  = data.readString();\n");
            } else {//Parcelable
                onTransactCode.append(p.clsName + argName + " = data.readParcelable(" + p.clsName + ".class.getClassLoader());\n");
            }
        }
    }

    /**
     * 生成proxy数据transact代码
     *
     * @param proxyBuilder
     * @param methods
     */
    private void overrideProxyMethods(TypeSpec.Builder proxyBuilder, List<AidlMethod> methods) {
        for (AidlMethod m : methods) {
            MethodSpec.Builder mBuilder = MethodSpec.methodBuilder(m.name)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC);
            int count = 0;
            boolean hasReturn = m.retCls != null || m.retClsName != null;

            if (m.retCls != null) mBuilder.returns(m.retCls);
            if (m.retClsName != null) mBuilder.returns(m.retClsName);
            StringBuilder codeBuilder = new StringBuilder();
            if (hasReturn) {
                String typeName = m.retCls != null ? m.retCls + "" : m.retClsName + "";
                String initValue = "0";
                if (m.retCls == boolean.class) initValue = "false";
                if (m.retClsName != null) initValue = "null";
                codeBuilder.append(typeName + " _result = " + initValue + ";\n");
            }
            codeBuilder.append("android.os.Parcel _data = android.os.Parcel.obtain();\n");
            codeBuilder.append("android.os.Parcel _reply = android.os.Parcel.obtain();\n");
            codeBuilder.append("try{\n");
            codeBuilder.append("_data.writeInterfaceToken(DESCRIPTOR);\n");
            for (ParamData p : m.params) {
                String argName = "var" + count;

                if (p.clsName != null) {
                    String cName = p.clsName.toString();
                    mBuilder.addParameter(p.clsName, argName);
                    if ("java.lang.String".equals(cName)) {
                        codeBuilder.append("_data.writeString(" + argName + ");\n");
                    } else {
                        codeBuilder.append("_data.writeParcelable(" + argName + ",0);\n");
                    }
                } else if (p.cls != null) {
                    mBuilder.addParameter(p.cls, argName);
                    addWriteStatement(codeBuilder, p.cls, argName);
                }
            }
            String code = "TRANSACTION_" + m.name;
            codeBuilder.append("mRemote.transact(" + code + ", _data, _reply, 0);\n");
            codeBuilder.append("_reply.readException();\n");
            if (hasReturn) {//返回值
                if (m.retCls != null) {
                    String type = m.retCls + "";
                    String finalType = type.substring(0, 1).toUpperCase() + type.substring(1);
                    codeBuilder.append("_result = _reply.read" + finalType + "();");
                }
                if (m.retClsName != null) {
                    String cName = m.retClsName.toString();
                    if ("java.lang.String".equals(cName)) {
                        codeBuilder.append("_result = _data.readString();");
                    } else if ("android.os.Parcelable".equals(cName)) {
                        codeBuilder.append("_result = _data.readParcelable();\n");
                    }
                }
            }

            codeBuilder.append("}catch(Exception e){e.printStackTrace();}finally{\n");
            codeBuilder.append("_reply.recycle();\n");
            codeBuilder.append("_data.recycle();\n");
            codeBuilder.append("}\n");
            if (hasReturn) codeBuilder.append("return _result;\n");
            mBuilder.addCode(codeBuilder.toString());
            proxyBuilder.addMethod(mBuilder.build());
        }
    }

    private void addWriteStatement(StringBuilder mBuilder, Class cls, String name) {
        if (cls == int.class) {
            mBuilder.append("_data.writeInt(" + name + ");\n");
        }
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(AIDL.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
