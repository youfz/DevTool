package com.test.tool

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
 @Test fun homeOpensJsonAndComparisonEditor() {
  ActivityScenario.launch(MainActivity::class.java).use {
   onView(withText(R.string.modules_json)).perform(click())
   onView(withHint(R.string.json_input)).check(matches(isDisplayed()))
   onView(withText(R.string.compare)).perform(click())
   onView(withHint(R.string.json_other)).check(matches(isDisplayed()))
  }
 }
}