package com.example.aaron.practice8.Model;

public class User {

    private  String email,password,name,phone,uid;
    public User(){

    }


    public User(String email, String password, String name, String phone, String uid){
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.uid = uid;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

}
