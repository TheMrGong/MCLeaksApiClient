
# MCLeaksApiClient
[![Amount of names](https://mcleaks.themrgong.xyz/badge.svg)](https://mcleaks.themrgong.xyz/restapi)
 
Java based client for checking if an account is MCLeaks.

Allows you to interact with my MCLeaks account checker through a RESTful service.

## Requirements
Java 8 is required in order to use the Java API. If you'd rather implement 
your own API (in whatever language), head [here](https://mcleaks.themrgong.xyz/restapi) for the REST API specifications.

## FAQ

**Q:** _Why isn't there an extension?_  
**A:** There is an extension for the service, however, it's private to me
and a friend. The reason being that where MCLeaks able to view its source,
they would be able to discern how I bypass their anti-bot security. 

Due to the latest changes in MCLeaks, crowd-sourcing is no longer possible,
meaning existing public extensions will no longer function.


**Q:** _How are accounts obtained for the service?_  
**A:** As stated above, there is a private extension that grabs tokens
and submits them through a private channel. 

**Q:** _Why should I use this service?_  
**A:** Should someone retrieve their account and want to play on your 
server, you wouldn't want to deal with manually proving they've recovered
their account. 

The way I store alts is unique, in that I can check
if an alt is still usable, and if it is, I know the account is still
susceptible to a malicious user logging in it.

**Q:** _How reliable is the service?_  
**A:** The server is implemented using [Spark Framework](https://sparkjava.com) 
as the backbone. A secondary server runs as failover in the event of the
first server going down. This allows seamless updates with no down-time,
as your traffic is automatically rerouted by [nginx](https://www.nginx.com/)
to the backup server.

As a precaution, I have money on stand-by in order to upgrade the
servers should I see the server is under heavy-load.

**Q:** _How should MCLeaks accounts be handled?_  
**A:** You should only kick the account; banning removes the benefit
of someone being able to recover their account. 

If you ban the user, then they'll both have to be removed from my database
and be unbanned on your server. Kicking simplifies this, and allows
you to easily keep and up-to-date state with who is and isn't an MCLeaks account.

**Q:** _I'm frequently hitting your rate limit/my server has large influxes of players
**A:** Contact me at my [email](mailto:contact@mail.themrgong.xyz) with the subject line
``MCLeaks - Rate limit increase request``, describe what server this is for and how many
players the server generally gets. I'll then give you an API token which you then have to use
when building the API (or in the request header `API-Key`)


## How to use

### As a dependency

```xml

<dependencies>
  <dependency>
      <groupId>me.gong</groupId>
      <artifactId>mcleaks-api</artifactId>
      <version>1.9.-SNAPSHOT</version>
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
final MCLeaksAPI api = MCLeaksAPI.builder()
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

###### API Keys
If you've received an API key, use it by calling the method `apiKey`
```java
.apiKey("6c6b3d9bcfc74723b0fb3178cfb85286")
```
An API key isn't required, but it is used in order to determine your rate
limit. Without an API key, you'll be at the default rate limit.


~~You must have a valid API key in order to use this service. 
If you would like an API key, contact me at gongora654@gmail.com with the subject `API Key Request`.
  Please describe why you want an API key and what you plan to use it with.
As of 3/27/2017, an API key is no longer required.~~

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
blocking thread within the [AsyncPlayerPreLoginEvent](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/player/AsyncPlayerPreLoginEvent.html) can be useful to delay the players' login
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

Using the cache'd result
-----

The API provides a method to retrieve a username/uuid's mcleaks status without causing a request to be sent.
This is useful in circumstances where you would like to check if a username/uuid is cache'd. 

##### Getting cache'd username result (not recommended)

```java
final Optional<Boolean> cachedName = api.getCachedCheck(playerName);
```

##### Getting cache'd UUID result (recommended)

```java
final Optional<Boolean> cachedUUID = api.getCachedCheck(playerUUID);
```


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

## REST API Endpoints

Interested in making your own client or want to know all the available methods?
Documentation of all the REST API Endpoints can be found [here](https://mcleaks.themrgong.xyz/restapi/).

## Rate Limiting

The API is limited to 8 requests a second with a burst allowance of
50 requests in cases of going above the rate limit for a 
short period of time. Implementation uses the [token bucket algorithm](https://en.wikipedia.org/wiki/Token_bucket).
Should you exceed the limit there is currently no penalty besides having
to wait 1 second before your next request. If you are exceeding this limit,
use the contact below for an increase in your rate limit.

## Contact

You can contact me regarding questions such as an increase in your rate limit
or request an account from the database. To contact me, use my [email](mailto:contact@mail.themrgong.xyz)
and please state what you are contacting me about in the subject line 
(either `Question - *summarization of question*` or `Account Removal Request - *account name*`)
If you are contacting me regarding removing an account, you must make sure to have changed the password before
doing so. **I will not remove an account if I am still able to log in as it.**


## License

This project is licensed under the [MIT License](LICENSE).

