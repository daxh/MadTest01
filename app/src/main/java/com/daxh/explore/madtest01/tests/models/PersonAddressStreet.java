package com.daxh.explore.madtest01.tests.models;

public class PersonAddressStreet {

    private String  streetName;

    public PersonAddressStreet(){}

    public PersonAddressStreet(String streetName){
        this.streetName = streetName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

}
