# Minstrel 🎵

**Because the world *definitely* needed another Android music player.**

Welcome to **Minstrel**, a handcrafted, artisanal, small-batch music player app that does exactly what thousands of other apps do—but this one is *mine*. (And let's be honest, an AI wrote like half the code, so it's technically a cyborg creation.)

## Why Minstrel?

You might be asking, *"Why build a music player when Spotify/YouTube Music/Poweramp exists?"*

To which I say: **Shut up.**

This is about *control*. This is about *freedom*. This is about learning how `Media3` works without crying (too much).

### Features
*   **Plays Music:** It takes audio files and makes sound come out of your phone. Groundbreaking, I know.
*   **Pause Button:** For when people try to talk to you.
*   **Next/Previous:** Experience the thrill of skipping songs you added to your own library.
*   **Modern UI:** Built with **Jetpack Compose**, so it looks pretty even if it crashes.
*   **Background Play:** It keeps playing when you close the app! (This took way longer to implement than I'd like to admit.)

## The "Under the Hood" Stuff (for nerds)

We didn't just slap this together with `MediaPlayer`. Oh no. We over-engineered this beauty with the latest and greatest Android libraries, because why write simple code when you can write *complex* code?

*   **Kotlin:** Because Java is so 2015.
*   **Jetpack Compose:** XML layouts are dead to us.
*   **Hilt:** Dependency Injection, because manually passing objects around is for peasants.
*   **Media3 / ExoPlayer:** The heavy lifter that actually plays the files.
*   **Coroutines & Flow:** Managing threads like a boss (or at least trying to avoid `ANR`s).
*   **GitHub Actions CI/CD:** That's right, we have a build pipeline. We release APKs automatically. We are professionals.

## Installation

1.  Go to the [Releases](../../releases) page.
2.  Download the latest `.apk`.
3.  Install it.
4.  Ignore the Google Play Protect warning—it's just jealous.

## Building It Yourself

If you want to experience the joy of Gradle build times:

1.  Clone this repo.
2.  Open in Android Studio.
3.  Hit **Run**.
4.  Wait.
5.  Enjoy.

## Contributing

Found a bug? **Keep it to yourself.** (Just kidding, file an issue.)

Want to fix a bug? **Submit a PR.** But be warned: the code style is "whatever the AI suggested at 2 AM," so good luck.

## License

Do whatever you want with it. It's probably broken anyway.
