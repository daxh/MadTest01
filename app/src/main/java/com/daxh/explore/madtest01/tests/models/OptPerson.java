package com.daxh.explore.madtest01.tests.models;

import com.annimon.stream.Optional;

public class OptPerson {

    private Optional<String> firstName = Optional.empty();

    private Optional<String> secondName = Optional.empty();

    private Optional<Integer> age = Optional.empty();

    private Optional<OptPersonAddress> address = Optional.empty();

    public Optional<String> getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = Optional.ofNullable(firstName);
    }

    public Optional<String> getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = Optional.ofNullable(secondName);
    }

    public Optional<Integer> getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = Optional.ofNullable(age);
    }

    public Optional<OptPersonAddress> getAddress() {
        return address;
    }

    public void setAddress(OptPersonAddress address) {
        this.address = Optional.ofNullable(address);
    }

}
