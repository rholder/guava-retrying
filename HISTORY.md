##1.0.7 - 2015-01-20
* New composite wait strategy #12 (shasts)
* Adding block strategies to the Retryer to decide how to block (tchdp)

##1.0.6 - 2014-03-26
* Javadoc updates for Java 8 (shasts)
* Bug from System.nanoTime() (fror), fix in #15
* Travis CI testing now working for Java 8

##1.0.5 - 2013-12-04
* Added Javadoc for all versions
* Added FibonacciWaitStrategy (joschi)
* Updated tested Guava version range from 10.x.x - 15.0 (joschi)
* Updated all dependencies (joschi)
* Updated to Gradle 1.9 (joschi)

##1.0.4 - 2013-07-08
* Added tested Guava version range from 10.x.x - 14.0.1
* Added Exception cause propagation to RetryException to fix #3

##1.0.3 - 2013-01-16
* Added time limit per attempt in a Retryer (dirkraft)
* Added license text

##1.0.2 - 2012-11-22
* Added Gradle wrapper support
* Updated top-level package to com.github.rholder.retry

##1.0.1 - 2012-08-29
* Added Javadoc links
* Added exponential wait strategy and unit tests

##1.0.0 - 2012-08-26
* Initial stable release, packaging for Maven central, no changes from original source
