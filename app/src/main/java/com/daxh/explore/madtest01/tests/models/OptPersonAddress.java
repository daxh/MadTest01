package com.daxh.explore.madtest01.tests.models;

import com.annimon.stream.Optional;

public class OptPersonAddress {

    private Optional<OptPersonAddressStreet> street;

    public Optional<OptPersonAddressStreet> getStreet() {
        return street;
    }

    public void setStreet(OptPersonAddressStreet street) {
        this.street = Optional.ofNullable(street);
    }

}
