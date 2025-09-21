package de.lehrbaum.firefly

/**
 * Marker annotation for UI tests that require a graphical environment.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiTest
