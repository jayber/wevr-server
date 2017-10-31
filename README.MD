This is the server component necessary for https://github.com/jayber/wevr

It provides WebRTC signalling and game state to the wevr component. In addition, the server provides information about
currently running Games or rooms on the `/count` websocket endpoint.

There is an instance running at www.vrlobby.co and the wevr component is configured to use that by default. Therefore
you don't have to have your own instance of this server to use the wevr component. However you may want to for privacy,
capacity or customizability reasons.

Wevr-server is built using [Scala](https://www.scala-lang.org/) and [Play 2.6](https://www.playframework.com/).
However all you really need is sbt 0.13 or above installed then type

`sbt run`

on the command line from the directory containing `build.sbt`, or

`sbt dist`

to build a deployable zip file containing everything you need to run on a server.