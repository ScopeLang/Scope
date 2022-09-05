<img src="img/Scope.svg" alt="Scope Logo"/>

*Scope* is still under development and is not ready for use.
It will be object-oriented programming langauge that compiles into [`FASM`](https://flatassembler.net/) and then into an executable.

> **Warning**
>
> Scope currently only works on Linux. Other platforms are planned.

## Building

These dependencies are required to build and run the compiler:
- `java`
- `maven`
- `fasm`

After these are installed, run:

```bash
# Clone...
$ git clone https://github.com/ScopeLang/Scope.git
# Build...
$ mvn package
```
You can then use the `./scope` bash script to build and run a Scope project in the `./env/` directory like so:
```bash
$ ./scope run -d helloWorld
```
You can also just directly use the `./target/scopelang-1.0-jar-with-dependencies.jar` file like so:
```bash
$ java -jar target/scopelang-1.0-jar-with-dependencies.jar run -d env/helloWorld
```

## Why Java? ☕

ANTLR is in Java so Scope is in Java.