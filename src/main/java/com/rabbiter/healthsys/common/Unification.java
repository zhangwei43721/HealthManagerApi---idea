package com.rabbiter.healthsys.common;

//主要用于统一返回结果格式。




public class Unification<T> {
    private Integer code;
    private String message;
    private T data;


    public static <T> Unification<T> success(){
        return new Unification<>(20000,"success",null);
    }

    public static <T> Unification<T> success(T data){
        return new Unification<>(20000,"success",data);
    }

    public static <T> Unification<T> success(T data,String message){
        return new Unification<>(20000,message,data);
    }

    public static <T> Unification<T> success(String message){
        return new Unification<>(20000,message,null);
    }

    public static<T>  Unification<T> fail(){
        return new Unification<>(20001,"fail",null);
    }

    public static<T>  Unification<T> fail(Integer code){
        return new Unification<>(code,"fail",null);
    }

    public static<T>  Unification<T> fail(Integer code, String message){
        return new Unification<>(code,message,null);
    }

    public static<T>  Unification<T> fail( String message){
        return new Unification<>(20001,message,null);
    }

    public Unification() {
    }

    public Unification(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Unification{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
