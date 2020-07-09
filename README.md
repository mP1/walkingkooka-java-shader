[![Build Status](https://travis-ci.com/mP1/walkingkooka-java-shader.svg?branch=master)](https://travis-ci.com/mP1/walkingkooka-java-shader.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/github/mP1/walkingkooka-java-shader/badge.svg?branch=master)](https://coveralls.io/github/mP1/walkingkooka-java-shader?branch=master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mP1/walkingkooka-java-shader.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-java-shader/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mP1/walkingkooka-java-shader.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mP1/walkingkooka-java-shader/alerts/)



# walkingkooka-java-shader

Providers several `BiFunction` that can be used to shade references to classes in java source text or java class files.


```java
final Charset charset = Charset.defaultCharset();
final byte[] before = "package package1;\nimport package2.Class3;\nclass Sample234 {\n  final package2.Class5 field = new package3.Class6();\n}".getBytes(charset);
System.out.println(new String(before, charset));
System.out.println();

final byte[] after = JavaShaders.javaFilePackageShader(Charset.forName("UTF-8"))
        .apply(before, Maps.of("package1", "package111", "package2", "package222"));
System.out.println(new String(after, charset));
```

prints the java source before

```java
package package1;
import package2.Class3;
class Sample234 {
  final package2.Class5 field = new package3.Class6();
}
```

prints the java source after shading

```java
package package111;
import package222.Class3;
class Sample234 {
  final package222.Class5 field = new package3.Class6();
}
```



## Getting the source

You can either download the source using the "ZIP" button at the top
of the github page, or you can make a clone using git:

```
git clone git://github.com/mP1/walkingkooka-java-shader.git
```
