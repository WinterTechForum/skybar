[![Build Status](https://semaphoreapp.com/api/v1/projects/c5cdee73-a0d4-47a6-a7f3-2b13a32969fb/360557/badge.png)](https://semaphoreapp.com/marshallpierce/skybar)
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

This will produce `build/libs/skybar-[version]-all.jar`. Use this jar as the argument to `-javaagent` in a `java` invocation. You'll also need to provide several config properties (see `SkybarConfig` for more info):

- `skybar.instrumentation.classRegex`: class name regex for classes to instrument. The name is slash-separated as in `com/foo/bar/baz`. 
- `skybar.webUi.port`: port for web ui, defaults to `54321`. Or use 0 to have it pick an available port.
- `skybar.source.fsPath`: filesystem path to source

These can be specified in a properties file that is specified in the `skybar.configFile` system property, or specified one at a time with system properties. If both are present, the system property-defined config values are used.

Here's an example invocation using the skybar demo app:

```
java \
  -Dskybar.instrumentation.classRegex='com/skybar/demo/.*' \
  -Dskybar.webUi.port=4321 \
  -Dskybar.source.fsPath=../skybar-demo/src/main/java \
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

# What's with the name?
This project started as a 1-day hackathon at WTF2015. We wanted to set the bar high, and the sky was the limit...
