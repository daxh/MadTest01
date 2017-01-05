package com.daxh.explore.madtest01.tests;

import com.annimon.stream.Optional;
import com.annimon.stream.function.BiFunction;
import com.annimon.stream.function.Predicate;
import com.daxh.explore.madtest01.tests.models.OptPerson;
import com.orhanobut.logger.Logger;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LambdasUsages {

    public static void lambdasAsVariables() {
        // As short one line definition
        // with 'auto' return statement
        // Custom interface used to store
        // lambda as variable
        BiFunc<String, String, String> func = (String v1, String v2) -> Integer.toString(Integer.parseInt(v1) + Integer.parseInt(v2));
        String s1 = func.apply("123", "456");

        // Almost the same simple example
        // but with multi-line lambda expression
        // An LSA interface used to store
        // lambda as variable
        BiFunction<Integer, Integer, String> func2 = (Integer i1, Integer i2) -> {
            String res = String.valueOf(i1 + i2);
            return "Result = " + res;
        };
        String s2 = func2.apply(123, 456);

        // Testing some LSA and standard interfaces
        // as lambdas storage
        Predicate<Integer> predicate = i -> i % 2 == 0;
        Runnable runnable = () -> predicate.test(10);

        // Lambdas variables and optionals
        Optional<BiFunc<String, String, String>> funcOpt = Optional.of((v1, v2) -> Integer.toString(Integer.parseInt(v1) + Integer.parseInt(v2)));
        String s = funcOpt.get().apply("1", "2");

        // Sorting usages
        String [] strs = {"aaa", "bb", "cccccccc", "a", "b", "zzzz"};
        Comparator<String> comparator = (lhs, rhs) -> lhs.length() > rhs.length() ? lhs.length() : rhs.length();
        Arrays.sort( strs, comparator);
    }

    public static void lambdasAndClasses() {
        OptPerson optPerson1 = new OptPerson(null);
        optPerson1.doSomeWork();

        OptPerson optPerson2 = new OptPerson(() -> Logger.d("Some work done"));
        optPerson2.doSomeWork();

        OptPerson optPerson3 = new OptPerson();
        optPerson3.doSomeWork(null);
        optPerson3.doSomeWork(() -> Logger.d("Some work done"));
    }

    public static void lambdasAsArgs() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

        // Summing up all elements
        int sum1 = sumAll(numbers);
        int sum2 = sumAll(numbers, n -> true);

        // Summing up all even elements
        int sum3 = sumAllEven(numbers);
        int sum4 = sumAll(numbers, n -> n % 2 == 0);

        // And even more if we use lambdas
        int sum5 = sumAll(numbers, n -> n > 3);

        Logger.d("%d %d %d %d %d", sum1, sum2, sum3, sum4, sum5);
    }

    private static int sumAll(List<Integer> numbers) {
        int total = 0;
        for (int number : numbers) {
            total += number;
        }
        return total;
    }

    private static int sumAllEven(List<Integer> numbers) {
        int total = 0;
        for (int number : numbers) {
            if (number % 2 == 0) {
                total += number;
            }
        }
        return total;
    }

    private static int sumAll(List<Integer> numbers, Predicate<Integer> p) {
        int total = 0;
        for (int number : numbers) {
            if (p.test(number)) {
                total += number;
            }
        }
        return total;
    }


    interface BiFunc<F, S, R> {
        R apply(F f, S s);
    }
}
