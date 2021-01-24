## Introduction to GCodeBuilder

GCodeBuilder is a simple cross-platform tool that makes it easy to generate GCode for hobby 2.5d CNC routers. The
interface allows users to draw shapes (rectangles, circles and paths) in a 2D plane, and then apply recipes
(pocket, profile) to these shapes to specify cutting operations. Finally, users can Generate GCode to turn shapes
and recipes into GCode to save and send to a CNC router.

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
best support. In addition to OpenJFX, GCodeBuilder also depends on Jackson, Log4j, JUnit and Lombok. The primary
developer uses IntelliJ IDEA Community Edition for Java development.