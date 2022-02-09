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
  - _message_ - human-readable description of an error's cause (with localization support, if needed)
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

[Samples][core samples dir]

## License

[GPL](./LICENSE)

<!-- References -->
[build glyph]: https://github.com/dwachura/restx/actions/workflows/master-ci.yml/badge.svg?branch=master
[github actions]: https://github.com/dwachura/restx/actions/workflows/master-ci.yml
[general flow diagram]: ./.docs/assets/general-flow-diagram.png "flow diagram"
[core samples dir]: ./core/samples
