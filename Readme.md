# Android peer client

[![Build Status](https://travis-ci.org/cine-io/cineio-peer-android.svg?branch=master)](https://travis-ci.org/cine-io/cineio-peer-android)

The android library for [cine.io peer](https://www.cine.io/products/peer).


## Installation

Add the following to your `build.gradle`.

```groovy
dependencies {
  compile 'io.cine:cineio-peer-android-sdk:0.0.4'
}
```

Ensure [Maven central](http://search.maven.org/) is included in your `build.gradle`. This should happen by default when building a project with Google's recommended Android IDE, [Android Studio](https://developer.android.com/sdk/installing/studio.html).

```
apply plugin: 'android'
buildscript {
  repositories {
    mavenCentral()
  }
}
repositories {
  mavenCentral()
}
```

Download cineio-peer-android-sdk to your application with `./gradlew build`.

## Usage

### Initialization

```java
import io.cine.peerclient.CinePeerClient;

// Some other potential imports:
// import java.util.ArrayList;
// import org.webrtc.MediaStream;
```

```java
public class MainActivity extends Activity implements CinePeerCallback {
    CinePeerClient cinePeerClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize the client
        String PUBLIC_KEY = "YOUR_PUBLIC_KEY";
        CinePeerClientConfig config = new CinePeerClientConfig(PUBLIC_KEY, this);
        config.setVideo(true);
        config.setAudio(true);
        //config.setSecretKey(CINEIO_SECRET_KEY); //optional
        cinePeerClient = CinePeerClient.init(config);

        //initialize the view
        CinePeerView vsv = cinePeerClient.createView();
        Runnable eglContextReadyCallback = null;
        VideoRendererGui.setView(vsv, eglContextReadyCallback);
    }
}
```

## Example App

The best way to see it in action is to run the example app locally. There's some trickiness around rendering the peer videos. You can find the example Activity here: https://github.com/cine-io/cineio-peer-android/blob/master/CineIOPeerExampleApp/src/main/java/io/cine/cineiopeerclientexampleapp/exampleapp/MainActivity.java
