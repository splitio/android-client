package io.split.android.engine.matchers;

import com.google.common.collect.Sets;

import io.split.android.engine.matchers.UserDefinedSegmentMatcher;
import io.split.android.engine.segments.Segment;
import io.split.android.engine.segments.StaticSegment;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for UserDefinedSegmentMatcher
 *
 */
public class UserDefinedSegmentMatcherTest {
    @Test
    public void works() {
        Set<String> keys = Sets.newHashSet("a", "b");
        Segment fetcher = new StaticSegment("foo", keys);

        UserDefinedSegmentMatcher matcher = new UserDefinedSegmentMatcher(fetcher);

        for (String key : keys) {
            assertThat(matcher.match(key, null, null, null), is(true));
        }

        assertThat(matcher.match("foo", null, null, null), is(false));
        assertThat(matcher.match(null, null, null, null), is(false));

    }

}
