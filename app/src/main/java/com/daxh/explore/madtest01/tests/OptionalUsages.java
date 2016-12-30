package com.daxh.explore.madtest01.tests;

import com.annimon.stream.Optional;
import com.daxh.explore.madtest01.tests.models.OptPerson;
import com.daxh.explore.madtest01.tests.models.OptPersonAddress;
import com.daxh.explore.madtest01.tests.models.OptPersonAddressStreet;
import com.daxh.explore.madtest01.tests.models.Person;
import com.daxh.explore.madtest01.tests.models.PersonAddress;
import com.daxh.explore.madtest01.tests.models.PersonAddressStreet;
import com.orhanobut.logger.Logger;

// Right now I think that general strategy to
// exclude NPE from project could be to use
// Optional.ofNullable to wrap almost everything
// over the project.
// Probably I am not completely right, but still

public class OptionalUsages {

    public static void createOptional() {
        //Empty Optional object
        Optional<Person> optPerson = Optional.empty();

        //Optional объект с ненулевым значением
        Person person = new Person();
        Optional<Person> optnnPerson = Optional.of(person);

        //Nullable Optional object
        Person person2 = null;
        Optional<Person> optnPerson = Optional.ofNullable(person2);
    }

    public static void useDefaultValue() {
        Optional<Person> personOpt = Optional.ofNullable(getPerson());

        // Provide default values if optional empty
        Person personOptNew = personOpt.orElse(new Person());

        // Throw exception if optional empty
        try {
            Person personNewThrow = personOpt.orElseThrow(Exception::new);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void nonNullChaining() {
        // Normally we doing it like this
        String streetName;
        Person person = getPerson();
        if (person != null) {
            PersonAddress personAddress = person.getAddress();
            if (personAddress != null) {
                PersonAddressStreet street = personAddress.getStreet();
                if (street != null) {
                    streetName = street.getStreetName();
                } else {
                    streetName = "EMPTY";
                }
            }
        }

        // But now we could do it like this
        Optional<Person> personOpt = Optional.ofNullable(getPerson());
        streetName = personOpt.map(Person::getAddress)
                .map(PersonAddress::getStreet)
                .map(PersonAddressStreet::getStreetName)
                .orElse("EMPTY");
        // We using 'map' when related
        // method returns explicit type

        // Or like this if we have appropriately
        // configured class interfaces
        Optional<OptPerson> optPerson = Optional.ofNullable(getOptPerson());
        streetName = optPerson.flatMap(OptPerson::getAddress)
                .flatMap(OptPersonAddress::getStreet)
                .flatMap(OptPersonAddressStreet::getStreetName)
                .orElse("EMPTY");
        // We using 'flatMap' when related method
        // returns type wrapped into Optional
    }

    public static void performIfExist() {
        // Normally we doing it like this
        Person person = getPerson();
        if(person != null) {
            Logger.d(person);
        } else {
            Logger.d("Person not found!");
        }

        // But now we could do it like this
        Optional<Person> personOpt = Optional.ofNullable(getPerson());

        // Not much different, but still useful
        if (personOpt.isPresent()) {
            Logger.d(personOpt.get());
        } else {
            Logger.d("Person not found!");
        }

        // Now really different
        Optional.ofNullable(getPerson())
                .executeIfPresent(Logger::d)
                .executeIfAbsent(() -> Logger.d("Person not found!"));
    }

    private static Person getPerson() {
        return new Person();
    }

    private static OptPerson getOptPerson() {
        return new OptPerson();
    }
}
