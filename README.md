# MCLeaksApiClient
Java based client for checking if an account is MCLeaks.

Allows you to interact with my MCLeaks account checker through a RESTful service.

## Requirements
Java 8 is required in order to use the Java API.

## How to use

### As a dependency

```xml

<dependencies>
  <dependency>
      <groupId>me.gong</groupId>
      <artifactId>mcleaks-api</artifactId>
      <version>1.9.0-SNAPSHOT</version>
  </dependency>
  ...
</dependencies>

<repositories>
  <repository>
      <id>themrgong-repo</id>
      <url>https://repo.themrgong.xyz/repository/thirdparty</url>
  </repository>
  ...
</repositories>
```

In your projects
------

First, you need to get an instance of the API. To do this, use the builder provided.

```java
 MCLeaksAPI api = MCLeaksAPI.builder()
                .threadCount(2)
                .expireAfter(10, TimeUnit.MINUTES).build();
```

The thread count specifies how many concurrent requests may be running at one time.
If you have a lot of requests going at once, you may increase this accordingly.
You may also use the API synchronously, which will block the thread executing the method.

The expireAfter parameter describes how long to cache data after fetching. 
  It is recommended to keep this value high as the status of an account being
   MCLeaks or not is unlikely to change.
   
If you don't require cache'd results (implementing your own cache), instead of calling
```java
.expireAfter(10, TimeUnit.MINUTES)
```
You would instead call
```java
.nocache()
```
which will instead directly retrieve results instead of querying the cache.

~~You must have a valid API key in order to use this service. 
If you would like an API key, contact me at gongora654@gmail.com with the subject `API Key Request`.
  Please describe why you want an API key and what you plan to use it with.~~
###### As of 3/27/2017, an API key is no longer required.

Checking if an account is MCLeaks
-----

#### Asynchronously

Uses a callback and a separate (pooled) thread in order to ensure
the calling thread never blocks. 

##### Using usernames (not recommended)

```java
api.checkAccount(playerName, isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);
```

The first parameter denotes the account username to check. Following that, 
a ``Consumer`` is used as a callback for your result. If an error was
thrown, the second ``Consumer`` acts as an error handler. 

You aren't recommended on using this method as names can change,
causing a MCLeaks account to be flagged as a real account.

##### Using UUIDs (recommended)

```java
api.checkAccount(playerUUID, isMCLeaks ->
                System.out.println("Got: " + isMCLeaks), Throwable::printStackTrace);
```

Using the previous format, the first parameter now represents the account uuid to check.
The same results will be returned as with the other method, however this method likely to
be more consistent in the event that a player changes their username.

#### Synchronously


Blocks the thread calling the method until either the account has been 
checked or an error has occurred checking the account. Regarding the Bukkit API,
can be useful in the [AsyncPlayerPreLoginEvent](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/player/AsyncPlayerPreLoginEvent.html) in order to delay their login
until the API has finished its request.

##### Using usernames (not recommended)

```java
final MCLeaksAPI.Result nameResult = api.checkAccount(playerName);
```

Checks an account by its username, blocking the main thread until finishing
its execution. As stated above, this method may return inconsistent results.

##### Using UUIDs (recommended)

```java
final MCLeaksAPI.Result uuidResult = api.checkAccount(playerUUID);
```

Blocks the main thread until it has finished checking the account by its
uuid. 

Interacting with the `MCLeaksAPI.Result` is simple, using its 3 respective methods.
Use `#isMCLeaks` to, as the name implies, see if the account was MCLeaks.
If an error has occurred, `#getError` can be used to handle it appropriately.
As a convenience method, `#hasError` is provided to check if an error has
occurred. `#getError` will return `null` if no error has arisen.

Cleaning up
----

```java
api.shutdown();
```

After your project (plugin or otherwise) has shut down, call the `shutdown` method
to gracefully end lingering threads from the executor service. Any attempts to request
something from the API will now result in an error.

The full example can be viewed [here](example/Example.java).

## Compilation

Build with `mvn clean install`. Get the jar out of the `/target` folder

The latest version can be found [here](https://github.com/TheMrGong/MCLeaksApiClient/releases/latest).

## JavaDocs

For a full run down of all the methods, the JavaDocs can be seen [here](https://mcleaks.themrgong.xyz).

## Contact

You can contact me regarding questions or requests to remove an account from the database at my [email](mailto:contact@mail.themrgong.xyz). 
Please state what you are contacting me about in the subject line 
(either `Question - *summarization of question*` or `Account Removal Request - *account name*`)
If you are contacting me regarding removing an account, you must make sure to have changed the password before
doing so. **I will not remove an account if I am still able to log in as it.**


## License

This project is licensed under the [MIT License](LICENSE).

