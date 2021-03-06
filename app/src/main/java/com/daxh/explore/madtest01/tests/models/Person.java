package com.daxh.explore.madtest01.tests.models;

import java.util.Locale;

public class Person {

    private String firstName;

    private String secondName;

    private Integer age;

    private PersonAddress address;

    public Person(){}

    public Person(String firstName, String secondName, int age, PersonAddress personAddress){
        this.firstName = firstName;
        this.secondName = secondName;
        this.age = age;
        address = personAddress;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public PersonAddress getAddress() {
        return address;
    }

    public void setAddress(PersonAddress address) {
        this.address = address;
    }

    @Override
    public String toString() {
        String s = super.toString();
        s += String.format(Locale.ENGLISH, " %s %s %d", firstName, secondName, age);
        if (address != null && address.getStreet() != null && address.getStreet().getStreetName() != null)
            s += " " + address.getStreet().getStreetName();
        return s;
    }

    public static class Settings {
        public Person mUser;

        public Settings(Person person) {
            mUser = person;
        }
    }

    public static class Message {
        public Person mUser;

        public Message(Person user) {
            mUser = user;
        }
    }
}
