package com.example.aaron.practice8.Model;

public class User {

    private  String email,password,name,phone,uid,lat,lng;
    public User(){

    }


    public User(String email, String password, String name, String phone, String uid, String lat, String lng){
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.uid = uid;
        this.lat = lat;
        this.lng = lng;
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

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }
}
