package fr.geobert.espresso;

import android.support.test.espresso.Root;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static org.hamcrest.Matchers.is;

public class DebugEspresso {
//    public static Matcher<Root> isDialog() {
//        return new TypeSafeMatcher<Root>() {
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("is dialog");
//            }
//
//            @Override
//            public boolean matchesSafely(Root root) {
//                int type = root.getWindowLayoutParams().get().type;
//                Log.d("DebugEspresso", "type " + type + " vs " + WindowManager.LayoutParams.TYPE_BASE_APPLICATION + " or " + WindowManager.LayoutParams.LAST_APPLICATION_WINDOW);
//                if ((type != WindowManager.LayoutParams.TYPE_BASE_APPLICATION
//                        && type < WindowManager.LayoutParams.LAST_APPLICATION_WINDOW)) {
//                    IBinder windowToken = root.getDecorView().getWindowToken();
//                    IBinder appToken = root.getDecorView().getApplicationWindowToken();
//                    Log.d("DebugEspresso", "win: " + windowToken + " vs app: " + appToken);
//                    if (windowToken == appToken) {
//                        // windowToken == appToken means this window isn't contained by any other windows.
//                        // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
//                        // therefore it must be a dialog box.
//                        return true;
//                    }
//                }
//                return false;
//            }
//        };
//    }

    /**
     * Matches {@link Root}s that are popups - like autocomplete suggestions or the actionbar spinner.
     */
    public static Matcher<Root> isAlertDialog() {
        return new TypeSafeMatcher<Root>() {
            @Override
            public boolean matchesSafely(Root item) {
                return withDecorView(withClassName(
                        is("android.widget.FrameLayout"))).matches(item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with decor view of type android.widget.FrameLayout");
            }
        };
    }
}
