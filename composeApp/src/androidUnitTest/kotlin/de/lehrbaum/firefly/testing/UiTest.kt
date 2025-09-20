package de.lehrbaum.firefly.testing

import org.junit.Assume
import org.junit.experimental.categories.Category
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

interface UiTestCategory

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Category(UiTestCategory::class)
annotation class UiTest

class UiTestRule : TestRule {
	override fun apply(base: Statement, description: Description): Statement =
		object : Statement() {
			override fun evaluate() {
				val skipUiTests = java.lang.Boolean.getBoolean(SKIP_UI_TESTS_PROPERTY)
				if (skipUiTests && description.shouldSkipUiTest()) {
					Assume.assumeTrue(
						"Skipping @UiTest ${description.displayName} because $SKIP_UI_TESTS_PROPERTY is true",
						false,
					)
				}
				base.evaluate()
			}
		}

	private fun Description.shouldSkipUiTest(): Boolean {
		if (getAnnotation(UiTest::class.java) != null) return true
		if (annotations.any { it.annotationClass == UiTest::class }) return true
		val resolvedClass = testClass ?: className?.let { runCatching { Class.forName(it) }.getOrNull() }
		if (resolvedClass?.getAnnotation(UiTest::class.java) != null) return true
		return annotations.any { annotation ->
			annotation is Category && annotation.value.any { it == UiTestCategory::class.java }
		}
	}

	private companion object {
		private const val SKIP_UI_TESTS_PROPERTY = "firefly.skipUiTests"
	}
}
