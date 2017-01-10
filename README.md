# MadTest01
This is the first try to explore such Modern Android Development (MAD) techniques as Java 8 like Lambdas,
Optional Values, Stream API in conjunction with Reactive Extensions and to understand how all this stuff
could be effectively used in regular development.

Things that covered in this example:

1. Basic usages of:

    1.1 Lambdas (Retrolambda: https://github.com/evant/gradle-retrolambda )

    1.2 Optionals (LSA: https://github.com/aNNiMON/Lightweight-Stream-API)

    1.3 Stream API (LSA: https://github.com/aNNiMON/Lightweight-Stream-API)

2. Some ways to use Rx in android development ()

2.1 Very basic examples of how to use Rx itself for execution of long running tasks and errors handling.

2.2 Setting up interaction between Rx & Android UI while executing long running tasks and handling errors.

2.3 Rx'fied Android Dialogs and Popup views.

2.4 Rx, android activities lifecycle and memory leaks. Using LeakCanary.
		        
The following dependencies used to add Rx to android:

1 RxAndroid https://github.com/ReactiveX/RxAndroid

2 RxJava https://github.com/ReactiveX/RxJava

3 RxBindings https://github.com/JakeWharton/RxBinding

4 RxLifecycle https://github.com/trello/RxLifecycle

This is a Leak Canary dependency:

https://github.com/square/leakcanary

I want to highlight that such important questions as 'Long running tasks execution and how to allow
them to survive android configuration changes (for example screen orientation changes)', 'View states
restoration after configuration changes' are not covered here. This will be explored later in a separate
example.

I hope that one day all these marvellous stuff will become a blogpost or a tutorial.


