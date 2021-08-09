package io.dwsoft.restx.core.annotations

/**
 * Initializer functions are used to define configuration to be used to initialize other
 * objects. They take single parameter of type T.() -> Unit (initializer block of object of type T)
 * and return no result, i.e. their signatures are expected to be like the one below:
 *
 * fun foo(initBlock: Bar.() -> Unit)
 */
annotation class RestXInitializer

/**
 * Factory functions are used to define 'building block' to be used to provide other
 * objects. They take single parameter of type T.() -> R (factory block of object of type R)
 * and return result of type R (object from factory block). Receiver of factory block represents
 * 'factories object' - object providing some pre-defined 'creation functions' (e.g. other
 * [RestXFactory] or [RestXInitializer] functions).
 *
 * Signatures of factory functions are expected to be like the one below:
 *
 * fun foo(factoryBlock: BarFactories.() -> Bar): Bar
 */
annotation class RestXFactory