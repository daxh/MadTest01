package com.daxh.explore.madtest01.tests.models;

import com.annimon.stream.Optional;

public class OptPersonAddressStreet {

    private Optional<String>  streetName;

    public Optional<String> getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = Optional.ofNullable(streetName);
    }

}
