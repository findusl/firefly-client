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

	private fun Description.shouldSkipUiTest(): Boolean = getAnnotation(UiTest::class.java) != null || testClass?.getAnnotation(UiTest::class.java) != null

	private companion object {
		private const val SKIP_UI_TESTS_PROPERTY = "firefly.skipUiTests"
	}
}
