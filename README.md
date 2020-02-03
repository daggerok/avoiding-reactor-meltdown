# how to block in reactor [![Build Status](https://travis-ci.org/daggerok/avoiding-reactor-meltdown.svg?branch=master)](https://travis-ci.org/daggerok/avoiding-reactor-meltdown)
...when it's needed

## problem

In short: we should never block! But sometimes we just cannot avoid blocking calls...
For instance, when we have to use library which hasn't reactive non-blocking support,
and maybe even not planed to be reactive, legacy libraries, JDBC, ThreadLocal, etc...

## identify problem

* use [reactor-tools](name-service/src/main/java/daggerok/Main.java#L39)
* use [BlockHound](name-service/src/main/java/daggerok/Main.java#L38)

## solve problem

* bad: create separate microservice, which will call blocking one
* good: perform all blocking calls [in a separate thread](name-service/src/main/java/daggerok/Main.java#L134) provided by special defined scheduled

## run and test

1. start apps:
   ```bash
   jdk11 ; ./mvnw -f mongo spring-boot:run
   jdk11 ; ./mvnw -f name-service spring-boot:run
   ```
1. execute all REST API calls from [api.http](api.http) file

## resources
* [YouTube: Avoiding Reactor Meltdown](https://www.youtube.com/watch?v=xCu73WVg8Ps)
