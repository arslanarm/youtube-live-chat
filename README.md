# Youtube Live Chat

This project is currently just kotlin port of 
[youtube-live-chat-downloader](https://github.com/abhinavxd/youtube-live-chat-downloader)
written in golang.

## Installation
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.arslanarm:youtube-live-chat:<TAG>")
}
```
or clone the project and run :publishToMavenLocal
and
```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("me.plony:youtube-live-chat:1.0-SNAPSHOT")
}
```

## Usage
Firstly you need to define the `LiveChat` instance providing the
url of the stream you want to listen
```kotlin
val chat = LiveChat("<url of the stream>")
```

Next you need to initialize the live chat.
It will get the HTML page of the live stream and
also extract the necessary data from the HTML page
```kotlin
chat.init() 
```

Youtube's innertube api works as a polling model, that means
we need to create a while loop and constantly ask for more messages
```kotlin
while (true) {
    val messages = try {
        chat.nextMessages() ?: continue
    } catch (e: StreamIsClosed) {
        println("Stream ended")
        break
    }
    println(messages.joinToString("\n") { "${it.authorName}: ${it.message}" })
}
```

You can pass the OutputStrategy to the LiveChat
```kotlin
suspend fun main() {
    val chat = LiveChat("https://www.youtube.com/watch?v=jfKfPfyJRdk", ColdFlowOutputStrategy())

    chat.nextMessages()
        .onEach { 
            println("${it.authorName} : ${it.message}")
        }
        .collect()
}
```
It will change the behaviour of the `nextMessages` method