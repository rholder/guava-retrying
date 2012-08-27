This is a fork of the excellent RetryerBuilder code posted to
http://code.google.com/p/guava-libraries/issues/detail?id=490 by Jean-Baptiste Nizet (JB).  I've added a Gradle build
for pushing it up to my little corner of Maven Central so that others can easily pull it into their existing projects
with minimal effort.

Maven
--------

    <dependency>
      <groupId>com.github.rholder</groupId>
      <artifactId>guava-retrying</artifactId>
      <version>1.0.0</version>
    </dependency>

Gradle
--------

    compile "com.github.rholder:guava-retrying:1.0.0"

Example
--------
A minimal sample of some of the functionality would look like:

    Callable<Boolean> callable = new Callable<Boolean>() {
        public Boolean call() throws Exception {
            return true; // do something useful here
        }
    };

    Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfResult(Predicates.<Boolean>isNull())
            .retryIfExceptionOfType(IOException.class)
            .retryIfRuntimeException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .build();
    try {
        retryer.call(callable);
    } catch (RetryException e) {
        e.printStackTrace();
    } catch (ExecutionException e) {
        e.printStackTrace();
    }
