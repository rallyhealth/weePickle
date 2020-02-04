### Building weePickle

weePickle is based on Mill, not sbt. (There is an sbt file, but it's just
for the documentation.) In order to build this, you will need to install
Mill:
```
brew install mill
```

#### IntelliJ
You can generate an IntelliJ project structure with:
```
./genIdea.sh
```

#### Compile

To build the entire system, say:
```
mill __.compile
```
In Mill, a single underscore `_` is the wildcard, and a double underscore
`__` is a recursive wildcard that drills into sub-modules. So the above
formula means "find all modules and submodules, and compile them".

#### Testing

Similarly, to run all unit tests:
```
mill __.test
```

#### Packaging

To create the JAR files:
```
mill __.jar
```

(There's also a `__.assembly` task, which I believe creates fatjars,
but I can't see why we would care in this case.)

#### When Things Silently Fail

Mill has one iffy characteristic: when something is broken in the
`build.sc` file, it will often fail silently. No errors or anything;
just nothing happens.

When this occurs, the `resolve` command tends to be a lifesaver. This
steps back and just shows what tasks *would* be run by the given
command, and it does generally show the errors.

So for example, when compile seems to be dead in the water, say:
```
mill resolve __.compile
```
and it will generally show you what's broken.
