package io.split.android.client.fallback;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class FallbackTreatmentsConfigurationBuilderTest {

    @Test
    public void builderAppliesSanitization() {
        FallbacksSanitizer sanitizer = Mockito.mock(FallbacksSanitizer.class);

        Map<String, FallbackTreatment> map = new HashMap<>();
        map.put("valid_flag", new FallbackTreatment("on"));
        FallbackConfiguration byFactory = FallbackConfiguration.builder()
                .global(new FallbackTreatment("off"))
                .byFlag(map)
                .build();

        FallbackConfiguration sanitized = FallbackConfiguration.builder()
                .global(null) // assume sanitizer drops invalid global, example
                .byFlag(map)
                .build();

        when(sanitizer.sanitize(byFactory)).thenReturn(sanitized);

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .sanitizer(sanitizer) // package-private method
                .byFactory(byFactory)
                .build();

        assertSame(sanitized, cfg.getByFactory());
        verify(sanitizer, times(1)).sanitize(byFactory);
    }

    @Test
    public void nullByFactoryAllowed_andSanitizerNotInvoked() {
        FallbacksSanitizer sanitizer = Mockito.mock(FallbacksSanitizer.class);

        FallbackTreatmentsConfiguration cfg = FallbackTreatmentsConfiguration.builder()
                .sanitizer(sanitizer) // package-private method
                .byFactory(null)
                .build();

        assertNull(cfg.getByFactory());
        verify(sanitizer, never()).sanitize(any());
    }
}
