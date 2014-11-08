TestRunner {
	*postAllTestClassesWithPrefix { |prefix|
		this.postClasses(Test.allTestClassesWithPrefix(prefix));
	}

	*postAllTestClasses { 
		this.postClasses(Test.allTestClasses);
	}

	*postClasses { |classes|
		"% classes:".format(classes.size.asString).postln;
		classes.do { |testClass| testClass.postln };
		"".postln;
	}

	*runAllTestsInTestClassesWithPrefix { |prefix, silent=false, haltOnError=false, verboseErrorDump=false|
		^this.runAllTestsInTestClasses(Test.allTestClassesWithPrefix(prefix), silent, haltOnError, verboseErrorDump);
	}

	*runAllTestsInAllTestClasses { |silent=false, haltOnError=false, verboseErrorDump=false|
		^this.runAllTestsInTestClasses(Test.allTestClasses, silent, haltOnError, verboseErrorDump);
	}

	*runAllTestsInTestClasses { |testClasses, silent=false, haltOnError=false, verboseErrorDump=false|
		this.postClasses(testClasses);
		^this.runTestMethods(testClasses.collect { |testClass| testClass.methods.select { |method| Test.isTestMethod(method) } }.flatten, silent, haltOnError, verboseErrorDump);
	}

	*runAllTests { |testClass, silent=false, haltOnError=false, verboseErrorDump=false|
		^this.runTestMethods(testClass.allTestMethods, silent, haltOnError, verboseErrorDump);
	}
	
	*runTest { |testClass, testMethodName, silent=false, haltOnError=false, verboseErrorDump=false|
		var testMethod;
		testMethod = testClass.allTestMethods.detect( { |m| m.name == testMethodName } );
		if (testMethod.notNil) {
			^this.runTestMethods( [testMethod], silent, haltOnError, verboseErrorDump );
		} {
			Error(testMethodName.asString+"do not exist in"+testClass).throw;
		};
	}

	*runTestMethods { |testMethods, silent=false, haltOnError=false, verboseErrorDump=false|
		var testClasses, numTestClasses, illegalTestMethod, failedAssertions, numTests, numAssertions, numFailedAssertions, numErrors, summaryNumTests = 0, summaryNumAssertions = 0, summaryNumFailedAssertions = 0, summaryNumErrors = 0;

		testClasses = testMethods.collect { |method| method.ownerClass }.asSet.asArray.sort { |a, b| a.asString < b.asString };
		numTestClasses = testClasses.size;

		illegalTestMethod = testMethods.detect { |method| Test.isTestMethod(method).not };
		if (illegalTestMethod.notNil) {
			Error("Bad test method name: %".format(illegalTestMethod)).throw;
		};

		testClasses.do { |testClass|
			var testClassResults;
			testClassResults = Array.new;

			if (silent.not) {
				if (numTestClasses > 1) {
					"====== % ======".format(testClass.name).postln;
					"".postln;
				};
	
				"Started".postln;
	
				if (haltOnError) {
					"".postln;
					"Will halt on error in order to dump back trace...".postln;
					"".postln;
				};
			};

			testMethods.select { |testMethod| testMethod.ownerClass == testClass }.do { |testMethod|
				var testMethodResult;
				testMethodResult = this.runTestMethod(testMethod, haltOnError);
				if (silent.not) {
					switch (testMethodResult[\result], \passed, ".", \failed, "F", \error, "E").post;
				};
				testClassResults = testClassResults.add(testMethodResult);
			};

			if (silent.not) {
				"".postln;
			};

			failedAssertions = testClassResults.collect { |testMethodResult| testMethodResult[\failedAssertions] }.reject(_.isNil).flatten;

			numTests = testClassResults.size;
			numAssertions = testClassResults.sum { |testMethodResult| testMethodResult[\numAssertions] };
			numFailedAssertions = failedAssertions.size;
			numErrors = testClassResults.count { |testMethodResult| testMethodResult[\result] == \error };

			summaryNumTests = summaryNumTests + numTests;
			summaryNumAssertions = summaryNumAssertions + numAssertions;
			summaryNumFailedAssertions = summaryNumFailedAssertions + numFailedAssertions;
			summaryNumErrors = summaryNumErrors + numErrors;

			if (silent.not) {
				"Finished in % seconds.".format(testClassResults.last[\finished]-testClassResults.first[\started]).postln;
				"".postln;
				"% tests, % assertions, % failures, % errors".format(numTests, numAssertions, numFailedAssertions, numErrors).postln;
				"".postln;

				if (numFailedAssertions > 0) {
					this.postFailedAssertions(failedAssertions);
				};

				if (numErrors > 0) {
					this.postErrors(testClass, testClassResults.collect { |testMethodResult| testMethodResult[\error] }.reject(_.isNil), verboseErrorDump);
				};

				if (numTestClasses > 1) {
					"".postln;
				};
			};
		};

		if (numTestClasses > 1) {
			"====== SUMMARY ======".postln;
			"".postln;
			"% tests, % assertions, % failures, % errors".format(summaryNumTests, summaryNumAssertions, summaryNumFailedAssertions, summaryNumErrors).postln;
			"".postln;
		};

		^IdentityDictionary[
			(\numTests -> summaryNumTests),
			(\numAssertions -> summaryNumAssertions),
			(\numFailedAssertions -> summaryNumFailedAssertions),
			(\numErrors -> summaryNumErrors)
		];
	}

	*runTestMethod { |testMethod, haltOnError=false|
		var testClass, testClassInstance, started, finished, assertions, failedAssertions, numAssertions, numFailedAssertions, result, testMethodError, identityDictionary;

		started = Main.elapsedTime;

		testClass = testMethod.ownerClass;
		testClassInstance = testClass.new;

		if (testClass.methods.any { |method| method.name == 'setup' }) { testClassInstance.setup };

		try {
			testClassInstance.perform(testMethod.name);
		} { |error|
			if (haltOnError) {
				error.throw;
			} {
				testMethodError = IdentityDictionary[
					(\testMethod -> testMethod),
					(\error -> error)
				];
			};
		};

		if (testClass.methods.any { |method| method.name == 'teardown' }) { testClassInstance.teardown };

		assertions = testClassInstance.assertions;
		failedAssertions = assertions.reject { |assertion| assertion[\success] };
		numFailedAssertions = failedAssertions.size;

		result = case
			{ testMethodError.notNil } { \error }
			{ numFailedAssertions > 0 } { \failed } ? \passed;

		finished = Main.elapsedTime;

		identityDictionary = IdentityDictionary[
			(\result -> result),
			(\numAssertions -> assertions.size),
			(\started -> started),
			(\finished -> finished),
		];

		if (result == \failed) {
			identityDictionary[\failedAssertions] = failedAssertions;
		};

		if (result == \error) {
			identityDictionary[\error] = testMethodError;
		};

		^identityDictionary
	}

	*postFailedAssertions { |failedAssertions|
		"Failures:".postln;
		"".postln;
		failedAssertions.do { |failedAssertion, i|
			"%. % :: % assertion failed => expected: %, actual: %".format(
				(i+1).asString, failedAssertion[\testMethod].asString, failedAssertion[\assertionType], failedAssertion[\expected], failedAssertion[\actual]
			).postln;
		};
		"".postln;
		"Cmd-Y on method name to go to source of method where failure occured".postln;
		"".postln;
	}

	*postErrors { |testClass, errors, verbose|
		"Errors:".postln;
		"".postln;
		if (verbose) {
			"========================================================".postln;
		};
		errors.do { |error, i|
			((i+1).asString++"."+error[\testMethod].asString+":: an error of type"+error[\error].class+"was thrown.").postln;
			"\tTo recreate, evaluate:".postln;
			("\t\t"++testClass++".runTest(\\"++ error[\testMethod].name ++", haltOnError: true)").postln;

			if (verbose) {
				"DUMP:".postln;
				error[\error].dump;
				"========================================================".postln;
			};
		};
		if (verbose.not) {
			"".postln;
			("For more error details: "++testClass++".runAllTests(verboseErrorDump: true)").postln;
		};
		("To halt test run on error and dump back trace: "++testClass++".runAllTests(haltOnError: true)").postln;
		"".postln;
	}
}

Test {
	var <assertions;

	*allTestClassesWithPrefix { |prefix|
		^this.allTestClasses.select { |testClass| testClass.name.asString.containsStringAt(0, prefix) };
	}

	*allTestClasses {
		^Class.allClasses.select { |testClass| testClass.superclass == Test }
	}

	*allTestMethods {
		^this.methods.select { |method| this.isTestMethod(method) }
	}

	*runAllTests { |silent=false, haltOnError=false, verboseErrorDump=false|
		^TestRunner.runAllTests(this, silent, haltOnError, verboseErrorDump)
	}
	
	*runTest { |testMethodName, silent=false, haltOnError=false, verboseErrorDump=false|
		^TestRunner.runTest(this, testMethodName, silent, haltOnError, verboseErrorDump)
	}

	*isTestMethod { |method|
		^method.name.asString.containsStringAt(0, "test_")
	}

	assert { |test|
		this.prAddAssertion( thisMethod.name, test == true, true, test );
	}

	assertTrue { |expression|
		this.prAddAssertion( thisMethod.name, expression == true, true, expression )
	}

	assertFalse { |expression|
		this.prAddAssertion( thisMethod.name, expression == false, false, expression )
	}

	assertNil { |expression|
		this.prAddAssertion( thisMethod.name, expression.isNil, "nil", expression )
	}

	assertNotNil { |expression|
		this.prAddAssertion( thisMethod.name, expression.notNil, "!= nil", expression )
	}

	assertEqual { |in1, in2|
		this.prAddAssertion( thisMethod.name, in1==in2, "in1 == in2", "% != %".format(in1.asString, in2.asString) )
	}

	assertNotEqual { |in1, in2|
		this.prAddAssertion( thisMethod.name, in1!=in2, "in1 != in2", "% == %".format(in1.asString, in2.asString) )
	}

	assertErrorThrown { |errorClass, func|
		var success, expected, actual, firstArgumentErrorMessage;

		firstArgumentErrorMessage = "Bad first argument to" + thisMethod ++ ":"+errorClass++". First argument in call to" + thisMethod + "must be Error or a subclass of Error. Check test code body.";

		if (errorClass.respondsTo(\superclasses).not) {
			Error(firstArgumentErrorMessage).throw
		};

		if (((errorClass == Error) or: (errorClass.superclasses.includes(Error))).not) {
			Error(firstArgumentErrorMessage).throw
		};

		expected = "error of type"+errorClass+"thrown";
		try { func.value } { |error| 
			success = if (error.class == errorClass) { true } { false };
			actual = "error of type"+error.class+"thrown";
		};
		if (success.isNil) {
			success = false;
			actual = "no error thrown";
		};
		this.prAddAssertion( thisMethod.name, success, expected, actual );
	}

	assertAnyErrorThrown { |func|
		var success, expected, actual;
		expected = "an error of any type thrown";
		try { func.value } { |error| 
			success = true;
			actual = "error of type"+error.class+"thrown";
		};
		if (success.isNil) {
			success = false;
			actual = "no error thrown";
		};
		this.prAddAssertion( thisMethod.name, success, expected, actual );
	}

	assertNoErrorThrown { |func|
		var success, expected, actual;
		expected = "no error thrown";
		try { func.value } { |error| 
			success = false;
			actual = "error of type"+error.class+"thrown";
		};
		if (success.isNil) {
			success = true;
			actual = "no error thrown";
		};
		this.prAddAssertion( thisMethod.name, success, expected, actual );
	}

	prAddAssertion { |assertionType, success, expected, actual|
		var lookupTestMethod = { | backtrace|
			while {
				backtrace.notNil and: {
					Test.isTestMethod(backtrace.functionDef).not
				}
			} {
				backtrace = backtrace.caller;
			};
			if (backtrace.notNil) { backtrace.tryPerform(\functionDef) };
		}; // Function based on searchForCaller function in Error.sc of standard SuperCollider class library

		assertions = assertions.add(
			IdentityDictionary[
				(\testMethod -> lookupTestMethod.value(this.getBackTrace)),
				(\assertionType -> assertionType),
				(\success -> success),
				(\expected -> expected),
				(\actual -> actual)
			]
		);

	}
}
