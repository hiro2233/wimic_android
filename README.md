# WiMic Android

WiMic Android is a fork and continuation of [Plumble](https://github.com/acomminos/Plumble),
a robust GPLv3 Mumble client for Android originally written by Andrew Comminos.
It uses the [Rimic](https://github.com/hiro2233/rimic_android) protocol implementation
(forked from Comminos's [Jumble](https://github.com/acomminos/Jumble)).

WiMic Android should run on Android 4.0 (IceCreamSandwich, API 14) and later.

WiMic Android is available on [Google Play](https://play.google.com/store/apps/details?id=bo.htakey.wimic).

There is an instructions configuration and [WiMic Server/Client](https://github.com/hiro2233/wimic) installation on https://hiro2233.github.io/wimic/docs/.

## Building on GNU/Linux

TODO: rimic-spongycastle should be built as a sub-project of Rimic's Gradle,
but currently isn't.

    git submodule update --init --recursive

    pushd libraries/rimic/libs/rimic-spongycastle
    ../../gradlew jar
    popd

    ./gradlew assembleDebug

## License

WiMic's [LICENSE](LICENSE) is GNU GPL v3.
