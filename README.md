# Implement simple websocket channel for study akka-stream

## project setup

I chose [mill](http://www.lihaoyi.com/mill/) as a build tool 

[mill](http://www.lihaoyi.com/mill/) has more intuitive syntax and faster build speed than sbt. and support for intelliJ is also great.

## install `mill` and generate an Intellij project

```
brew install mill
```

> Mill supports IntelliJ by default. Use `mill mill.scalalib.GenIdea/idea` to generate an IntelliJ project config for your build.
This also configures IntelliJ to allow easy navigate & code-completion within your build file itself.

```
mill mill.scalalib.GenIdea/idea
```

## run
```
mill --watch channel.run
```

## test
```
mill --watch channel.test
```
