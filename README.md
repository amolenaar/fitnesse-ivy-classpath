# FitNesse classpath resolver

This FitNesse plugin allows you to easely add classpath dependencies to
your FitNesse classpath.

## Usage

In your FitNesse project root create a file `plugins.properties` if it is not
already there. In that file, add the following symbol type:

    SymbolTypes = fitnesse.wikitext.widgets.IvyClasspathSymbolType

Now in your acceptance suite page simply use the dependency resolver:

    !resolve

If you're using maven, define the pom file instead:

    !resolve -pom pom.xml

Need only a specific configuration (good idea!), use the `-c` switch:

    !resolve -c acceptance

## Maven too!

Since this resolver is based on Apache Ivy, it is capable of dealing with
both ivy and Maven POM files. The advantage you ask? Well, Ivy takes a lot
(a whole lot) less dependencies along with it, so using it in your FitNesse
project is a lot easier.

Have fun,

Arjan
