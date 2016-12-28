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

    @Override
    public String toString() {
        String s = super.toString();
        if (streetName != null) s += " " + streetName;
        return s;
    }
}
