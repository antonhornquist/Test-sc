# Test-sc

Unit test framework for SuperCollider

## Usage

Tests are implemented as subclasses of class Test typically named with suffix "Tests" such as "JSONTests".

Class TestRunner performs tests. Here are some useful functions:

* TestRunner.postAllTestClasses
* TestRunner.runAllTestsInAllTestClasses
* TestRunner.postAllTestClassesWithPrefix("Test")
* TestRunner.runAllTestsInTestClassesWithPrefix("Test")

## Requirements

These classes were developed and have been tested in SuperCollider 3.6.6.

## Installation

Copy the Test-sc folder to the user-specific or system-wide extension directory. Recompile the SuperCollider class library.

The user-specific extension directory may be retrieved by evaluating Platform.userExtensionDir in SuperCollider, the system-wide by evaluating Platform.systemExtensionDir.

## License

Copyright (c) Anton Hörnquist
