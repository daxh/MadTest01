package com.daxh.explore.madtest01.tests;

import android.support.v4.util.Pair;

import com.daxh.explore.madtest01.tests.models.Person;
import com.daxh.explore.madtest01.tests.models.PersonAddress;
import com.daxh.explore.madtest01.tests.models.PersonAddressStreet;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;

public class BasicRxUsages {

    public static void rxAndLongRunningTaskWithResult() {
        // Don't use Observable.create
        // https://artemzin.com/blog/rxjava-defer-execution-of-function-via-fromcallable/
        Observable.fromCallable(BasicRxUsages::longRunningTask)
                .subscribeOn(Schedulers.newThread())            // each time new separate thread
//            .subscribeOn(Schedulers.io())                     //  or use IO thread
//            .subscribeOn(Schedulers.computation())            //  or use Computation thread
                .observeOn(AndroidSchedulers.mainThread())      // Observe results on main thread
                .doOnSubscribe(() -> Logger.d("Point #1"))      // Thread-Main | Just before task starts
                .doOnNext(result ->
                        Logger.d("Point #3.1 " + result))       // Thread-Main | Right after task completed and result available
                .doOnCompleted(() -> Logger.d("Point #3.2"))    // Thread-Main | Everything completed
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    public static void rxAndLongRunningTaskVoid() {
        // Don't use Observable.create
        // https://artemzin.com/blog/rxjava-defer-execution-of-function-via-fromcallable/
        Observable.fromCallable(() -> {
            longRunningTaskVoid();
            return null;
        })
                .subscribeOn(Schedulers.computation())          // For example let's use computation thread
                .observeOn(AndroidSchedulers.mainThread())      // Observe results on main thread
                .doOnSubscribe(() -> Logger.d("Point #4"))      // Thread-Main | Just before task starts
                .doOnNext(result ->
                        Logger.d("Point #6.1 " + result))       // Thread-Main | Right after task completed and result available
                .doOnCompleted(() -> Logger.d("Point #6.2"))    // Thread-Main | Everything completed
                .doOnError(Throwable::printStackTrace)
                .subscribe();
    }

    public static void combiningFewTasksWhichDependsOnEachOtherInSerial() {
        // Don't use Observable.create
        // https://artemzin.com/blog/rxjava-defer-execution-of-function-via-fromcallable/
        Observable.fromCallable(() -> getNewPerson(1))
                .subscribeOn(Schedulers.io())                   // Thread - IO   | Run tasks here
                .doOnSubscribe(() -> Logger.d("Point #1"))      // Thread - Main | As whole rxchain started from Main
                .flatMap(user -> {                              // Get Observable, transform content, return Observable, keep wrapper (in this case - Observable)
                    Logger.d("Point #2");                       // Thread - IO   |
                    // fetchPersonSettings and fetchPersonMessages will be
                    // executed serially on IO thread one after one
                    return Observable.zip(
                            Observable.fromCallable(() -> fetchPersonSettings(user)),
                            Observable.fromCallable(() -> fetchPersonMessages(user)),
                            Pair::create);
                    })
                .observeOn(AndroidSchedulers.mainThread())      // Thread - Main | From now we will observe results on Main thread
                .doOnNext(settingsMsgsListPair ->
                        // Just printing results, but we
                        // can do here whatever we need
                        Logger.d("Point #3 " + settingsMsgsListPair))
                .flatMap(settingsMsgsListPair -> {                  // Get one type (List of Pairs of settings and msgs list), return completely another type (Observable)
                    // Transforms to Observable from ArrayList of person
                    // messages. Rx will then fires up a related doOnNext
                    // for each element of an array one by one
                    Logger.d("Point #4 " + settingsMsgsListPair);
                    return Observable.from(settingsMsgsListPair.second);
                })
                .doOnNext(message -> {
                    // This will fires up few times, one
                    // per element in messages array list
                    // We are still on Main thread
                    Logger.d("Point #5 " + message);
                })
                .subscribe();

        // Pay attention to difference between 'map'
        // operator and 'flatMap'.
        // _Map_ — transform the items emitted by an
        // Observable by applying a function to each
        // item
        // _FlatMap_ — transform the items emitted by
        // an Observable into Observables, then flatten
        // the emissions from those into a single
        // Observable
        // In other words , first completely
        // transforms input to output. The second
        // keeps the shell or wrapper (in this case
        // this is Observable) and transforms it is
        // content from one type to another
    }

    public static void combiningFewTasksWhichDependsOnEachOtherInParallel() {
        Observable.fromCallable(() -> getNewPerson(1))
                .subscribeOn(Schedulers.io())                   // Thread - IO   | Run tasks here
                .doOnSubscribe(() -> Logger.d("Point #1"))      // Thread - Main | As whole rxchain started from Main
                .observeOn(AndroidSchedulers.mainThread())      // Thread - Main | Switch to Thread - Main
                .doOnNext(person ->
                        Logger.d("Point #2 " + person))         // Thread - Main | Perform just one operation on Thread - Main
                .observeOn(Schedulers.io())                     // Thread - IO   | Switch back to Thread - IO
                .flatMap(user -> {                              // Get Observable, transform content, return Observable, keep wrapper (in this case - Observable)
                    Logger.d("Point #3");                       // Thread - IO   |
                    // fetchPersonSettings and fetchPersonMessages
                    // will be executed in parallel each on own IO
                    // thread instance spawned be Rx specifically
                    return Observable.zip(
                            Observable.fromCallable(() -> fetchPersonSettings(user)).subscribeOn(Schedulers.io()),
                            Observable.fromCallable(() -> fetchPersonMessages(user)).subscribeOn(Schedulers.io()),
                            Pair::create);
                })
                .observeOn(AndroidSchedulers.mainThread())      // Thread - Main | From now we will observe results on Main thread
                .doOnNext(settingsMsgsListPair ->
                        // Just printing results, but we
                        // can do here whatever we need
                        Logger.d("Point #4 " + settingsMsgsListPair))
                .flatMap(settingsMsgsListPair -> {              // Get one type (List of Pairs of settings and msgs list), return completely another type (Observable)
                    // Transforms to Observable from ArrayList of person
                    // messages. Rx will then fires up a related doOnNext
                    // for each element of an array one by one
                    Logger.d("Point #5 " + settingsMsgsListPair);
                    return Observable.from(settingsMsgsListPair.second);
                })
                .doOnNext(message -> {
                    // This will fires up few times, one
                    // per element in messages array list
                    // We are still on Main thread
                    Logger.d("Point #6");
                })
                .subscribe();
    }

    public static void forwardingExceptionsToSubscriber() {
        Observable
                // This operator accepts Callable.call function
                // that rethrows checked exceptions that's why
                // we don't need any additional try/catch blocks
                // or error handling operators
                .fromCallable(() -> getNewPersonOrError(1))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> Logger.d("Point #1"))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(person -> {
                    double random = Math.random();

                    if (random < .25){
                        Logger.d("EXCEPTION: " + "Point #2 " + person);
                        try {
                            throw new IOException("Point #2 " + person);
                        } catch (IOException e) {
                            // If you forced to catch checked exceptions
                            // inside operator then we could use the
                            // following to downstream them to subscriber
                            throw Exceptions.propagate(e);
                        }
                    }

                    if (random < .5){
                        Logger.d("EXCEPTION: " + "Point #2 " + person);
                        // Unchecked exceptions will be sent
                        // downstream to subscriber by default
                        throw new RuntimeException("Point #2 " + person);
                    }

                    Logger.d("Point #2 " + person);
                })
                .observeOn(Schedulers.io())
                .flatMap(user -> {
                    Logger.d("Point #3");
                    return Observable.zip(
                            Observable
                                    // This operator accepts Callable.call function
                                    // that rethrows checked exceptions that's why
                                    // we don't need any additional try/catch blocks
                                    // or error handling operators. Works when inside
                                    // 'zip' too.
                                    .fromCallable(() -> fetchPersonSettingsOrError(user)).subscribeOn(Schedulers.io()),
                            Observable
                                    // This operator accepts Callable.call function
                                    // that rethrows checked exceptions that's why
                                    // we don't need any additional try/catch blocks
                                    // or error handling operators. Works when inside
                                    // 'zip' too.
                                    .fromCallable(() -> fetchPersonMessagesOrError(user)).subscribeOn(Schedulers.io()),
                            Pair::create);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(settingsMsgsListPair ->
                        Logger.d("Point #4 " + settingsMsgsListPair))
                .flatMap(settingsMsgsListPair -> {
                    Logger.d("Point #5 " + settingsMsgsListPair);
                    return Observable.from(settingsMsgsListPair.second);
                })
                .subscribe(
                    // onNext
                    message -> Logger.d("Point #6"),
                    // onError
                    throwable -> {
                        // All exceptions could be processed
                        // in one place - here, if this is a
                        // desirable behavior
                        Logger.d("onError " + throwable.getMessage());
                    },
                    // onCompleted
                    () -> Logger.d("DONE")
                );

        // More error handling operators
        // could be found here:
        // https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators
    }

    public static void detailedErrorHandling() {
        Observable
                // This operator accepts Callable.call function
                // that rethrows checked exceptions that's why
                // we don't need any additional try/catch blocks
                // or error handling operators
                .fromCallable(() -> getNewPersonOrError(1))
                // Retry operation above for 3 times before
                // fallback into subscribers's onError
                .retry(3)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> Logger.d("Point #1"))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(person -> {
                    double random = Math.random();

                    if (random < .25){
                        Logger.d("EXCEPTION1: " + "Point #2 " + person);
                        try {
                            throw new IOException("Point #2 " + person);
                        } catch (IOException e) {
                            // If you forced to catch checked exceptions
                            // inside operator then we could use the
                            // following to downstream them to subscriber
                            throw Exceptions.propagate(e);
                        }
                    }

                    if (random < .5){
                        Logger.d("EXCEPTION2: " + "Point #2 " + person);
                        // Unchecked exceptions will be sent
                        // downstream to subscriber by default
                        throw new RuntimeException("Point #2 " + person);
                    }

                    Logger.d("Point #2 " + person);
                })
                // Retry operation above when Runtime
                // Exception happened. In other cases
                // fallback into subscribers's onError
                .retryWhen(observable -> observable.flatMap(throwable -> {
                    if ((throwable instanceof RuntimeException)) {
                        Logger.d("RETRY: Point #2");
                        // Generate another dumb Observable
                        // to lead 'retryWhen' to resubscribe
                        // on source Observable once again
                        return Observable.just(null);
                    }

                    // Generate Observable with error
                    // to lead 'retryWhen' to fallback
                    // to subscribers onError
                    Logger.d("FALLBACK: Point #2");
                    return Observable.error(throwable);
                }))
                .observeOn(Schedulers.io())
                .flatMap(user -> {
                    Logger.d("Point #3");
                    return Observable.zip(
                            Observable
                                    // This operator accepts Callable.call function
                                    // that rethrows checked exceptions that's why
                                    // we don't need any additional try/catch blocks
                                    // or error handling operators. Works when inside
                                    // 'zip' too.
                                    .fromCallable(() -> fetchPersonSettingsOrError(user))
                                    .subscribeOn(Schedulers.io())
                                    // Generate some default value in case of error
                                    .onErrorReturn(throwable -> {
                                        Logger.d("DEFAULT: fetchPersonSettingsOrError");
                                        return new Person.Settings(user);
                                    }),
                            Observable
                                    // This operator accepts Callable.call function
                                    // that rethrows checked exceptions that's why
                                    // we don't need any additional try/catch blocks
                                    // or error handling operators. Works when inside
                                    // 'zip' too.
                                    .fromCallable(() -> fetchPersonMessagesOrError(user))
                                    .subscribeOn(Schedulers.io())
                                    // Generate empty list in case of error
                                    .onErrorReturn(throwable -> {
                                        Logger.d("DEFAULT: fetchPersonSettingsOrError");
                                        return new ArrayList<>();
                                    }),
                            Pair::create);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(settingsMsgsListPair ->
                        Logger.d("Point #4 " + settingsMsgsListPair))
                .flatMap(settingsMsgsListPair -> {
                    Logger.d("Point #5 " + settingsMsgsListPair);
                    return Observable.from(settingsMsgsListPair.second);
                })
                .subscribe(
                        // onNext
                        message -> Logger.d("Point #6"),
                        // onError
                        throwable -> {
                            // All exceptions could be processed
                            // in one place - here, if this is a
                            // desirable behavior
                            Logger.d("onError " + throwable.getMessage());
                        },
                        // onCompleted
                        () -> Logger.d("DONE")
                );

        // More error handling operators
        // could be found here:
        // https://github.com/ReactiveX/RxJava/wiki/Error-Handling-Operators
    }

    private static String longRunningTask() {
        // According to rxAndLongRunningTaskWithResult
        // everything inside this function happening on
        // RxNewThreadScheduler-1 (or something like that).
        // In other words on new thread spawned by Rx.

        Logger.d("Point #2.1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logger.d("Point #2.2");

        return "Result";
    }

    private static void longRunningTaskVoid() {
        // According to rxAndLongRunningTaskVoid everything
        // inside this function happening on
        // RxComputationScheduler-1 (or something like that).
        // In other words on specific computation
        // thread spawned by Rx.

        Logger.d("Point #5.1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logger.d("Point #5.2");
    }

    private static Person getNewPerson(int num){
        longRunningTaskSimulation("getNewPerson");

        PersonAddressStreet personAddressStreet = new PersonAddressStreet("Ccccc" + num);
        PersonAddress personAddress = new PersonAddress(personAddressStreet);
        return new Person("Aaaaa" + num, "Bbbbb" + num, num, personAddress);
    }

    public static Person getNewPersonOrError(int num) throws IOException, InterruptedException {
        longRunningTaskSimulationOrError("getNewPersonOrError");

        PersonAddressStreet personAddressStreet = new PersonAddressStreet("Ccccc" + num);
        PersonAddress personAddress = new PersonAddress(personAddressStreet);
        return new Person("Aaaaa" + num, "Bbbbb" + num, num, personAddress);
    }

    private static Person.Settings fetchPersonSettings(Person person){
        longRunningTaskSimulation("fetchPersonSettings");

        return new Person.Settings(person);
    }

    public static Person.Settings fetchPersonSettingsOrError(Person person) throws IOException, InterruptedException {
        longRunningTaskSimulationOrError("fetchPersonSettingsOrError");

        return new Person.Settings(person);
    }

    private static ArrayList<Person.Message> fetchPersonMessages(Person person){
        longRunningTaskSimulation("fetchPersonMessages");

        ArrayList<Person.Message> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            messages.add(new Person.Message(person));
        }
        return messages;
    }

    public static ArrayList<Person.Message> fetchPersonMessagesOrError(Person person) throws IOException, InterruptedException {
        longRunningTaskSimulationOrError("fetchPersonMessagesOrError");

        ArrayList<Person.Message> messages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            messages.add(new Person.Message(person));
        }
        return messages;
    }

    static private void longRunningTaskSimulation(String s) {
        Logger.d("STARTED: " + s);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logger.d("COMPLETED: " + s);
    }

    static private void longRunningTaskSimulationOrError(String s) throws IOException, InterruptedException {
        Logger.d("STARTED: " + s);

        Thread.sleep(750);
        if (Math.random() < .5){
            Logger.d("EXCEPTION: " + s);
            throw new IOException(s);
        }
        Thread.sleep(750);

        Logger.d("COMPLETED: " + s);
    }
}
