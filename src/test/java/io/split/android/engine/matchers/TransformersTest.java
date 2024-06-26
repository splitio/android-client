package io.split.android.engine.matchers;

import org.junit.Test;

import java.util.Calendar;

import static io.split.android.engine.matchers.Transformers.asDate;
import static io.split.android.engine.matchers.Transformers.asDateHourMinute;
import static io.split.android.engine.matchers.Transformers.asLong;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class TransformersTest {

    @Test
    public void asLongWorks() {
        assertThat(asLong("19"), is(nullValue()));
        assertThat(asLong(null), is(nullValue()));
        assertThat(asLong(19), is(equalTo(19L)));
        assertThat(asLong((long) -19), is(equalTo((long) -19)));
    }

    @Test
    public void asDateWorks() {
        long april11_2016 = 1460400024000L;
        long april12_2016 = 1460420421903L;

        assertThat(asDate(april12_2016), is(equalTo(1460419200000L)));
        assertThat(asDate(april11_2016), is(equalTo(1460332800000L)));
        assertThat(asDate(null), is(nullValue()));
        assertThat(asDate(Calendar.getInstance()), is(nullValue()));
    }

    @Test
    public void asDateHourMinuteWorks() {
        long april11_2016_18_40 = 1460400024000L;
        long april12_2016_midnight_20 = 1460420421903L;

        assertThat(asDateHourMinute(april12_2016_midnight_20), is(equalTo(1460420400000L)));
        assertThat(asDateHourMinute(april11_2016_18_40), is(equalTo(1460400000000L)));
        assertThat(asDate(null), is(nullValue()));
        assertThat(asDate(Calendar.getInstance()), is(nullValue()));
    }

}
