# RESTX (REST eXceptions)

[![Build status][build glyph]][github actions]

# What is RESTX?

RESTX is a library designed to support developers of HTTP-based Kotlin/Java services by removing the burden of reinventing the wheel
in the area of generating structured and informing error response model (hopefully, in a simple, intuitive manner).

## Desing overview

![general flow diagram]  
_Fault handling process flow_

RESTX's design is based on a concept of **fault** - an object that holds information about an error happened during processing request. Despite
the library's name, faults can be of any type, but probably in most cases any kind (subtype) of an exception type will be used.

Faults are processed by **response generators** - components that generate **responses** using information from fault objects passed to them
according to the user-defined configuration.

A response is consisted of _status_ (as understood by HTTP) and **_payload_**, which in turn, consists information about an actual error.
In RESTX, there are two kinds of service errors distinguished, each with its own payload representation. Those are:
- _operation errors_ - typically representing any fault that happens during processing of a request (after its validation) and 
are described by:
  - _code_ - machine-readable identifier (usually API-wide) of an error's cause, e.g. `INTERNAL_SERVICE_ERROR`, `AUTHORIZATION_FAILURE`, etc.
  - _message_ - human-readable description of an error's cause
- _request data errors_ - representation of any kind of error caused by the invalid data in a processed request. One may say it's
specialization of an operation error, extending its model with _source_ property that describes location of an invalid data in the request, 
e.g. named query param, body property or header.

The above payload types represent the family of _single-error payloads_ (they hold information about single error), but there's also 
a possibility that the service processes may end with a failures caused by multiple errors (e.g. in case of input validation it may be 
reasonable to report all errors at once, instead of returning response containing only the first one found).  
For such purposes, RESTX provides so-called _multi-error payloads_. Those are, simply speaking, composites of a single-error payloads, 
described by a single property named _errors_, in which a collection of "component" payloads is kept.

As it was earlier spoken, in RESTX objects of any type can be treated as faults. There might be cases in which faults of the same type 
represent different service errors and thus should be converted into payloads with different code and message (basing, for example, on one of 
the fault's property).  
To support such cases, payload generators use (fault) **causes** in their work. Causes can be understood as simple wrappers that let to 
identify concrete "reasons of fault". They are created during payload generation process by **cause resolvers** and to be further used by 
**cause processors** which are responsible for actual generation of response payloads.

## Usage examples

Here's the example of configuration and usage of the most basic generator that processes faults of Exception type:

```kotlin
  val generator = RestX.respondTo<Exception> {
    representing { operationError {
      identifiedBy { type() } // identify faults by its type - could be omitted, as it's a default behavior
      withCode { sameAsCauseId() } // generate payloads with code same as fault's identifier - could be omitted, as it's a default behavior
      withMessage { generatedAs { context.localizedMessage } } // generate payloads with exception message 
    } }
    withStatus(500) // HTTP status of a response
  }
  
  val response = generator.responseOf(RuntimeException("Service failure"))
  println(response.payload) // OperationError(code=java.lang.RuntimeException, message=Service failure)
  println(response.status) // HttpStatus(code=500)
```

Generator that responds to input data errors, may be crated like:

```kotlin
  class InvalidParamException(val type: Source.Type, val location: String, message: String) : RuntimeException(message)
  
  val generator = RestX.respondTo<InvalidParamException> {
    representing { requestDataError {
      identifiedBy("INVALID_PARAM")
      withMessage { generatedAs { context.localizedMessage } }
      pointingInvalidValue { resolvedBy { cause ->
        cause.context.let { it.type.toSource(it.location) }
      } }
    } }
    withStatus(400)
  }
  
  val response = generator.responseOf(InvalidParamException(Source.Type.QUERY, "queryParam1", "Invalid value"))
  println(response.payload) // RequestDataError(code=INVALID_PARAM, message=Invalid value, source=Source(type=QUERY, location=queryParam1))
  println(response.status) // HttpStatus(code=400)
```

Another, more real-life use case, would involve creation of a generator that returns payloads with some custom codes, instead of
fault's type name:

```kotlin
  val generator = RestX.respondTo<Exception> {
    representing { operationError {
      withCode { mapBased( // codes will be taken from defined map - key == fault id (type name, as defined above)
        Exception::class.qualifiedName!! to "GENERIC_FAILURE",
        RuntimeException::class.qualifiedName!! to "RUNTIME_FAILURE",
        IOException::class.qualifiedName!! to "IO_FAILURE"
      ) }
      withMessage { mapBased( // messages can also be retrieved this way
        Exception::class.qualifiedName!! to "Mapped message for exception",
        RuntimeException::class.qualifiedName!! to "Mapped message for runtime exception",
        IOException::class.qualifiedName!! to "Mapped message for I/O exception"
      ) }
    } }
    withStatus(500) // HTTP status of a response
  }

var response = generator.responseOf(RuntimeException("Runtime failure"))
println(response.payload) // OperationError(code=RUNTIME_FAILURE, message=Mapped message for runtime exception)

response = generator.responseOf(IOException("I/O failure"))
println(response.payload) // OperationError(code=IO_FAILURE, message=Mapped message for I/O exception)
```

Below generator will return multi-error payloads:

```kotlin
  val generator = RestX.respondTo<List<Exception>> {
    representing { compositeOf<Exception> {
      extractedAs { it }
      eachRepresenting { operationError {
        withMessage { generatedAs { context.localizedMessage } }
      } }
    } }
    withStatus(500)
  }
  
  val response = generator.responseOf(listOf(
    Exception("Generic error"), IllegalArgumentException("Bad argument"), NumberFormatException("Wrong number")
  ))
  println(response.payload) // MultiErrorPayload(errors=[OperationError(code=java.lang.Exception, message=Generic error), 
                            //      OperationError(code=java.lang.IllegalArgumentException, message=Bad argument), 
                            //      OperationError(code=java.lang.NumberFormatException, message=Wrong number)])
  println(response.status) // HttpStatus(code=500)
```

Generators can also be composed:

```kotlin
  val predefinedGenerator = RestX.respondTo<IllegalStateException> {
    representing { operationError {
      withCode { fixed("ILLEGAL_STATE") } // generate payloads with fixed code...
      withMessage { fixed("Illegal state exception") } // ...and fixed message as well
    } }
    withStatus(500) // HTTP status of a response
  }
  
  val compositeGenerator = RestX.compose { registeredByFaultType {
    register { predefinedGenerator } // register pre-defined generator
    register { // register in-line generator
      respondTo<IllegalArgumentException> {
        representing { operationError {
          withMessage { fixed("Illegal argument exception") }
        } }
        withStatus(400)
      }
    }
  } }
  
  var response = compositeGenerator.responseOf(IllegalStateException())
  println(response.payload) // OperationError(code=ILLEGAL_STATE, message=Illegal state exception)
  println(response.status) // HttpStatus(code=500)
  
  response = compositeGenerator.responseOf(IllegalArgumentException())
  println(response.payload) // OperationError(code=java.lang.IllegalArgumentException, message=Illegal argument exception)
  println(response.status) // HttpStatus(code=400)
```

## License

[GPL](./LICENSE)

<!-- References -->
[build glyph]: https://github.com/dwachura/restx/actions/workflows/master-ci.yml/badge.svg?branch=master
[github actions]: https://github.com/dwachura/restx/actions/workflows/master-ci.yml
[general flow diagram]: ./.docs/assets/general-flow-diagram.png "flow diagram"
