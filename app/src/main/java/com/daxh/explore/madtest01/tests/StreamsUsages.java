package com.daxh.explore.madtest01.tests;

import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.IntStream;
import com.annimon.stream.Optional;
import com.annimon.stream.PrimitiveIterator;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
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
            makeEveryoneOlderConvertToIntStreamAges();
            findAverageAge();
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

    private static void makeEveryoneOlderConvertToIntStreamAges() {
        // Note 'boxed' sometimes could
        // be extremely useful to deal
        // with IntStream

        ArrayList arrayList = Stream.of(getPersons())
                .mapToInt(person -> person.getAge() + 5)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        Logger.d(arrayList);
    }

    private static void findAverageAge() {
        double averageAge = 0;

        // Surprisingly LSA IntStream hasn't 'average'
        // method unlike Java 8 Stream API, but it could
        // be easily achieved with custom operator

        averageAge = Stream.of(getPersons())
                .mapToInt(Person::getAge)
                .custom(stream -> {
                    long count = 0, sum = 0;
                    while (stream.iterator().hasNext()) {
                        count++;
                        sum += stream.iterator().nextInt();
                    }
                    return (count == 0) ? 0 : sum / (double) count;
                });

        // Or even in a such way:
        averageAge = Stream.of(getPersons())
                .mapToInt(Person::getAge)
                .custom(new Average());

        // More examples of custom operators
        // could be found there:
        // https://github.com/aNNiMON/Lightweight-Stream-API/blob/master/stream/src/test/java/com/annimon/stream/CustomOperators.java

        Logger.d("Average Age = %f", averageAge);
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

    private static class Average implements Function<IntStream, Double> {
        @Override
        public Double apply(IntStream stream) {
            long count = 0, sum = 0;
            final PrimitiveIterator.OfInt it = stream.iterator();
            while (it.hasNext()) {
                count++;
                sum += it.nextInt();
            }
            return (count == 0) ? 0 : sum / (double) count;
        }
    }

}
