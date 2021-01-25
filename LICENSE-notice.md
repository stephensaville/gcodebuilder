# Open Source Licenses

This project depends on other open source libraries, which are typically downloaded from Maven Central when compiling
the source distribution, but can be included directly as jar archives in binary distributions of this project for the
convenience of users who are not software developers. Your use of these libraries in executable or source code form is
subject to the terms and conditions of each library's individual license. The main dependencies are mentioned here along
with references to their licenses and where you can download source code.

## OpenJDK

Some binary distributions may contain an embedded copy of the [OpenJDK](http://openjdk.java.net/) runtime, which is 
licensed under [GPL v2 + CLASSPATH](http://openjdk.java.net/legal/gplv2+ce.html). Binary distributions typically bundle
the [Amazon Coretto](https://aws.amazon.com/corretto/) or [AdoptOpenJDK](https://adoptopenjdk.net/) distribution, but
GCodeBuilder should compile and run against any distribution of OpenJDK 11. Source code is available from
[GitHub](https://github.com/openjdk/jdk).

## OpenJFX

GCodeBuilder depends on the [OpenJFX](https://openjfx.io/) implementation of the JavaFX GUI framework, which is also
licensed under [GPL v2 + CLASSPATH](http://openjdk.java.net/legal/gplv2+ce.html) and available to download in
executable form from [GluonHQ](https://gluonhq.com/products/javafx/). Source code is available from
[GitHub](https://github.com/openjdk/jfx).

## Google Guava

GCodeBuilder depends on [Google Guava](https://github.com/google/guava), which is licensed under the same
[Apache 2.0](https://github.com/google/guava/blob/master/COPYING) license as GCodeBuilder. Source code is available from
[GitHub](https://github.com/google/guava).

## Jackson

GCodeBuilder depends on the 2.x version of [Jackson](https://github.com/FasterXML/jackson), which is licensed under the
same [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) license as GCodeBuilder. Source code is available from
GitHub for the [jackson-core](https://github.com/FasterXML/jackson-core) and
[jackson-databind](https://github.com/FasterXML/jackson-databind) components that GCodeBuilder depends on.

## Log4j

GCodeBuilder depends on the 2.x version of [Log4j](https://logging.apache.org/log4j/2.x/index.html), which is licensed
under the same [Apache 2.0](https://logging.apache.org/log4j/2.x/license.html) license as GCodeBuilder. Source code and
executables are available to download from [Apache](https://logging.apache.org/log4j/2.x/download.html).

## JUnit

GCodeBuilder unit tests depend on the 5.x version of [JUnit](https://github.com/junit-team/junit5), which is licensed
under the [Eclipse Public License v 2.0](https://github.com/junit-team/junit5/blob/main/LICENSE.md). Source code is
available from [GitHub](https://github.com/junit-team/junit5).

## Project Lombok

GCodeBuilder depends on [Project Lombok](https://projectlombok.org/), which is licensed under the
[MIT License](https://github.com/rzwitserloot/lombok/blob/master/LICENSE). Source code is available from
[GitHub](https://github.com/rzwitserloot/lombok).
