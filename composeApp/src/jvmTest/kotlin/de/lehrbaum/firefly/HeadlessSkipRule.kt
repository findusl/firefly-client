package de.lehrbaum.firefly

import java.awt.GraphicsEnvironment
import org.junit.Assume.assumeFalse
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

object HeadlessSkipRule : TestRule {
	override fun apply(base: Statement, description: Description): Statement =
		object : Statement() {
			override fun evaluate() {
				assumeFalse(GraphicsEnvironment.isHeadless())
				base.evaluate()
			}
		}
}
