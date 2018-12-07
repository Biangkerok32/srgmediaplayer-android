package ch.srg.mediaplayer.testutils.assertions;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Matcher;

import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by npietri on 06.10.16.
 */

public class RecyclerViewAssertion implements ViewAssertion {

    private final Matcher<Integer> matcherItemCount;

    public RecyclerViewAssertion(Matcher<Integer> matcherItemCount) {
        this.matcherItemCount = matcherItemCount;
    }

    @Override
    public void check(View view, NoMatchingViewException noViewFoundException) {
        if (noViewFoundException != null) {
            throw noViewFoundException;
        }

        RecyclerView recyclerView = (RecyclerView) view;
        assertThat(recyclerView.getAdapter().getItemCount(), matcherItemCount);
    }
}