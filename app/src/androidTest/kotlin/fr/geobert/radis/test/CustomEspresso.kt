package fr.geobert.radis.test

import org.hamcrest.Matcher
import android.view.View
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Description
import android.widget.AdapterView
import fr.geobert.radis.ui.drawer.NavDrawerItem
import android.support.test.espresso.ViewAssertion
import android.support.test.espresso.NoMatchingViewException
import android.support.test.espresso.util.TreeIterables
import junit.framework.AssertionFailedError
import android.support.test.espresso.util.HumanReadables
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*

// sugar wrapper for the backquote
fun iz<T>(matcher: Matcher<T>): Matcher<T> = `is`(matcher)

fun iz<T>(value: T): Matcher<T> = iz(equalTo(value))

// Custom espresso matchers
// kotlin translation of https://gist.github.com/cpeppas/b5ffe6bd29b67d96416a
fun withResourceName(resourceName: String) = withResourceName(iz(resourceName))


fun withResourceName(resourceNameMatcher: Matcher<String>): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(view: View): Boolean {
            val id = view.getId()
            return id != View.NO_ID && id != 0 && view.getResources() != null &&
                    resourceNameMatcher.matches(view.getResources().getResourceName(id))
        }

        override fun describeTo(desc: Description) {
            desc.appendText("with resource name: ")
            resourceNameMatcher.describeTo(desc)
        }
    }
}

fun withAdaptedData(dataMatcher: Matcher<Any>): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(p0: Description) {
            p0.appendText("with class name: ")
            dataMatcher.describeTo(p0)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is AdapterView<*>) {
                return false
            }
            val adapter = view.getAdapter()
            for (i in 0..adapter.getCount()) {
                if (dataMatcher.matches(adapter.getItem(i))) {
                    return true
                }
            }
            return false
        }

    }
}

fun withNavDrawerItem(itemTitleMatcher: Matcher<String>): Matcher<Any> {
    return object : TypeSafeMatcher<Any>() {
        override fun describeTo(p0: Description) {
            p0.appendText("with nav drawer item: ")
            itemTitleMatcher.describeTo(p0)
        }

        override fun matchesSafely(item: Any): Boolean {
            if (item !is NavDrawerItem) return false
            if (itemTitleMatcher.matches(item.getTitle())) {
                return true
            }
            return false
        }
    }
}

fun withNavDrawerItem(title: String) = withNavDrawerItem(equalTo(title))

fun has(expectedCount: Int, selector: Matcher<View>): ViewAssertion {
    return object : ViewAssertion {
        override fun check(p0: View?, p1: NoMatchingViewException?) {
            if (p0 != null) {
                val descendants = TreeIterables.breadthFirstViewTraversal(p0)
                val selected = descendants.filter { selector.matches(it) }
                if (selected.count() != expectedCount) {
                    throw AssertionFailedError(HumanReadables.getViewHierarchyErrorMessage(p0, selected,
                            "Found ${selected.count()} views instead of $expectedCount matching: $selector",
                            "****MATCHES****"))
                }
            }
        }
    }
}

fun has(expectedCount: Int, clazz: Class<out View>) = has(expectedCount, ViewMatchers.isAssignableFrom(clazz))

fun isActionBarSpinner(): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(p0: View): Boolean {
            return withClassName(iz("android.support.v7.internal.widget.SpinnerCompat")).matches(p0)
        }

        override fun describeTo(p0: Description) {
            p0.appendText("with view of type SpinnerCompat")
        }

    }
}