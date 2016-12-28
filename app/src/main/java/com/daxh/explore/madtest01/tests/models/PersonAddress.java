package com.daxh.explore.madtest01.tests.models;

public class PersonAddress {

    private PersonAddressStreet street;

    public PersonAddress(){}

    public PersonAddress(PersonAddressStreet street){
        this.street = street;
    }

    public PersonAddressStreet getStreet() {
        return street;
    }

    public void setStreet(PersonAddressStreet street) {
        this.street = street;
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (street != null) s += " " + street.toString();
        return s;
    }
}
