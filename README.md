# MCLeaksApiClient
Java based client for checking if an account is MCLeaks.

Allows you to interact with my MCLeaks account checker through a RESTful service.

## Requirements
Java 8 is required in order to use the API.

## How to use

### As a dependency

```xml

<dependencies>
  <dependency>
    <groupId>com.github.TheMrGong</groupId>
    <artifactId>MCLeaksApiClient</artifactId>
    <version>1.1</version>
  </dependency>
  ...
</dependencies>

<repositories>
  <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
  </repository>
  ...
</repositories>
```

### In your projects

First, you need to get an instance of the API. To do this, use the builder provided.

```java
 MCLeaksAPI api = MCLeaksAPI.builder()
                .threadCount(2)
                .expireAfter(10, TimeUnit.MINUTES).build();
```

The thread count specifies how many concurrent requests may be running at one time.
  If you have a lot of requests going at once, you may increase this accordingly 

The expireAfter parameter describes how long to cache data after fetching. 
  It is recommended to keep this value high as the status of an account being
   MCLeaks or not is unlikely to change.

~~You must have a valid API key in order to use this service. 
If you would like an API key, contact me at gongora654@gmail.com with the subject `API Key Request`.
  Please describe why you want an API key and what you plan to use it with.~~
###### As of 3/27/2017, an API key is no longer required.

#### Checking if an account is MCLeaks

```java
api.checkAccount("BwA_BOOMSTICK", isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);
```

The first parameter denotes the account to check. Following that, 
a ``Consumer`` is used as a callback for your result. If an error was
thrown, the second ``Consumer`` acts as an error handler. An error
may be thrown through the following cases:

* The username supplied doesn't follow Minecraft name conventions
* An internal server error occured
* The `GET` method was used instead of the `POST` method
* Unable to contact the server

The full example can be viewed [here](example/Example.java).

## Compilation

Build with `mvn clean install`. Get the jar out of the `/target` folder

The latest version can be found [here](https://github.com/TheMrGong/MCLeaksApiClient/releases/latest).


## License

This project is licensed under the [MIT License](LICENSE).