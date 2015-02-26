# skybar
Skybar: Live code coverage engine

Skybar shows you a web UI of your java code with live-updating per-line execution counts.

![Skybar](https://raw.githubusercontent.com/WinterTechForum/skybar/master/skybar.jpg)

For a convenient toy app to try it out with, see [Skybar Demo](https://github.com/WinterTechForum/skybar-demo).

# Usage
Build the jar:

```
./gradlew
```

This will produce `build/libs/skybar-[version]-all.jar`. Use this jar as the argument to `-javaagent` in a `java` invocation. You'll also need to provide several system properties:

- `skybar.includedPackage`: package prefix to instrument, slash-separated as in `com/foo/bar/baz`. 
- `skybar.serverPort`: port for web ui, defaults to `4321`.
- `skybar.source.path`: filesystem path to source

Here's an example invocation using the skybar demo app:

```
java \
  -Dskybar.includedPackage=com/skybar/demo \
  -Dskybar.serverPort=4321 \
  -Dskybar.source.path=../skybar-demo/src/main/java \
  -javaagent:path/to/skybar-1.0-SNAPSHOT-all.jar \
  -jar ../skybar-demo/target/skybar-demo-1.0-SNAPSHOT-jetty-console.war --headless
```

To use both the debugger together with skybar, you can include an `-agentlib` line as well, like:

```
java \
  -agentlib:jdwp=transport=dt_socket,address=localhost:9009,server=y,suspend=y \
  -javaagent:build/libs/skybar-1.0-SNAPSHOT-all.jar \
  ...
```

Once that's running, connect to [http://localhost:4321](http://localhost:4321) (change the port as needed if you're not using the default) and use your app. You should see live updates to the number of times each line of code is executed.
