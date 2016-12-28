package com.daxh.explore.madtest01.tests;

import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.daxh.explore.madtest01.tests.models.Person;
import com.daxh.explore.madtest01.tests.models.PersonAddress;
import com.daxh.explore.madtest01.tests.models.PersonAddressStreet;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;

public class StreamsUsages {

    public static void start(boolean b) {
        if (b) {
            findPersonsWithConfiguredStreetNames();
            findPersonsWithStreetNamesContaining3();
            findConfiguredPersonAdressesOfPersonsOlderThan4();
            makeEveryoneOlder();
            makeEveryoneOlderConvertToAges();
        }
    }

    private static void findPersonsWithConfiguredStreetNames() {
        ArrayList arrayList = Stream.of(getPersons())
                .filter((value) ->
                        Optional.of(value)
                            .map(Person::getAddress)
                            .map(PersonAddress::getStreet)
                            .map(PersonAddressStreet::getStreetName)
                            .isPresent()
                )
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static void findPersonsWithStreetNamesContaining3() {
        ArrayList arrayList = Stream.of(getPersons())
                .filter((value) ->
                        Optional.of(value)
                            .map(Person::getAddress)
                            .map(PersonAddress::getStreet)
                            .map(PersonAddressStreet::getStreetName)
                            .map(s -> s.contains("3")).orElse(false)
                )
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static void findConfiguredPersonAdressesOfPersonsOlderThan4() {
        ArrayList arrayList = Stream.of(getPersons())
                .filter((value) -> {
                    Optional<Person> person = Optional.of(value);
                    boolean older = person
                            .map(Person::getAge).map(a -> a > 4).orElse(false);
                    boolean streetNameProvided = person
                            .map(Person::getAddress)
                            .map(PersonAddress::getStreet)
                            .map(PersonAddressStreet::getStreetName)
                            .map(s -> !TextUtils.isEmpty(s)).orElse(false);
                    return older && streetNameProvided;
                })
                .map(Person::getAddress)
                .map(PersonAddress::getStreet)
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static void makeEveryoneOlder() {
        ArrayList arrayList = Stream.of(getPersons())
                .peek(person -> person.setAge(person.getAge() + 5))
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static void makeEveryoneOlderConvertToAges() {
        ArrayList arrayList = Stream.of(getPersons())
                .map(person -> person.getAge() + 5)
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static ArrayList<Person> getPersons() {
        ArrayList<Person> persons = new ArrayList<>();

        persons.add(new Person("Aaaaa0", "Bbbbb0", 0, null));
        persons.add(new Person("Aaaaa1", "Bbbbb1", 1, null));
        persons.add(new Person("Aaaaa2", "Bbbbb2", 2, new PersonAddress(new PersonAddressStreet("Ccccccc2"))));
        persons.add(new Person("Aaaaa3", "Bbbbb3", 3, new PersonAddress(new PersonAddressStreet("Ccccccc3"))));
        persons.add(new Person("Aaaaa3", "Bbbbb3", 4, new PersonAddress(new PersonAddressStreet("Ccccccc3"))));
        persons.add(new Person("Aaaaa5", "Bbbbb5", 5, null));
        persons.add(new Person("Aaaaa6", "Bbbbb6", 6, new PersonAddress(new PersonAddressStreet())));
        persons.add(new Person("Aaaaa7", "Bbbbb7", 3, new PersonAddress(new PersonAddressStreet("Ccccccc7"))));
        persons.add(new Person("Aaaaa8", "Bbbbb8", 8, new PersonAddress()));
        persons.add(new Person("Aaaaa9", "Bbbbb9", 9, new PersonAddress()));
        persons.add(new Person("Aaaaa10", "Bbbbb10", 10, new PersonAddress(new PersonAddressStreet("Ccccccc10"))));
        persons.add(new Person("Aaaaa11", "Bbbbb11", 11, new PersonAddress(new PersonAddressStreet("Ccccccc11"))));

        return persons;
    }

}
