![GCB Icon](src/com/gcodebuilder/app/images/gcb_icon_128.png)

## Introduction to GCodeBuilder

GCodeBuilder is a simple cross-platform GUI tool that makes it easy to generate GCode for hobby 2.5d CNC routers. The
interface allows users to draw Shapes (rectangles, circles and paths) in a 2D plane, apply Recipes (pocket, profile) to
these shapes to specify cutting operations, and finally Generate GCode to turn Shapes and Recipes into GCode
instructions that can be saved to a file and sent to a CNC router to make things IRL.

## Installation

GCodeBuilder binary distributions are not available yet, but the application can be built and run from source using
Gradle on any platform supported by OpenJDK by following these simple instructions:

1. Install one of the following OpenJDK 11 distributions:
    * [Amazon Coretto](https://aws.amazon.com/corretto/)
    * [AdoptOpenJDK](https://adoptopenjdk.net/installation.html)
2. Install the [Gradle](https://gradle.org/install/) build tool.
3. Clone the gcodebuilder repository: ```git clone https://github.com/stephensaville/gcodebuilder.git```
4. Build and run using gradle: ```gradle run```

## History

GCodeBuilder was started by a CNC router hobbyist because he was frustrated with the software available on Linux and
MacOSX for generating GCode. Most of the decent desktop tools were either not free, only available for Windows, or much
more complicated than they needed to be. Other options required SVG drawings to be prepared in a separate app then
uploaded to a clunky web-based UI to program cut operations and generate GCode. This hobbyist was looking for something
that would provide a full idea-to-GCode experience with drawing, cut operations, and GCode generation in a single
intuitive GUI application. Fortunately, this CNC hobbyist was also a software developer with over 20 years of
experience, so he sat down towards the end of February 2020 to turn his idea into code. There turned out to be a lot of
time to work on this project because 2020 was a year that involved spending a lot of time at home, so by the end of the
year GCodeBuilder was a functional application ready to be released to the Open Source community in January 2021.

## Programming Environment

GCodeBuilder is written in Java using the OpenJFX version of the JavaFX GUI framework. GCodeBuilder *should* run on all
platforms supported by Java and OpenJFX, but the primary developer uses Linux and MacOSX systems, so these will have the
best support. In addition to OpenJFX, GCodeBuilder also depends on Jackson, Google Guava, Log4j, JUnit and Project
Lombok. The primary developer uses IntelliJ IDEA Community Edition for Java development.