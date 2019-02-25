# Chromecast media player **(Android)**
Sample of player with chromecast supporting

## Architecture
The architecture of the player is based on the state machine and the delegates system for controlling playback.

### State Machine
Inside the MainMediaPlayer is a State Machine with such a multitude of states:

* Empty - the initial state before initialization.
* Preparing - player in initializes playback of media content.
* Prepared - media is loaded and ready to play.
* Playing
* Paused
* WaitingForNetwork
* Error

<img src="/docs/images/PlayerStates.png">

### Delegates system
Main MediaPlayer have system of delegates and listeners. 

PlayingDelegate is an object for control media playing (ext. ExoPlayer, MediaPlayer, Chromecast). All delegates can receive Player status events: Play, Pause, etc. - but only the lead delegate will play the media content.
StateListeners is an object for notification of entities dependent on the state of the media player (ext. MediaSession)

<img src="/docs/images/PlayerArchitecture.png">



## License

```

MIT License

Copyright (c) 2019 Konstantin Kulikov (kostyaxxx8@gmail.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```