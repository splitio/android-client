package tests.integration.sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static helper.IntegrationHelper.splitChangeV2;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import fake.HttpClientMock;
import fake.HttpResponseMock;
import fake.HttpResponseMockDispatcher;
import helper.DatabaseHelper;
import helper.IntegrationHelper;
import helper.TestableSplitConfigBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SyncConfig;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.utils.logger.Logger;
import tests.integration.shared.TestingHelper;

public class FlagSetsStreamingTest {

    // workm with set_3, set_4
    private static final String splitChange5 = splitChangeV2("5", "4", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjUsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6WyJzZXRfMyIsInNldF80Il0sImNvbmRpdGlvbnMiOlt7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJJTl9TRUdNRU5UIiwibmVnYXRlIjpmYWxzZSwidXNlckRlZmluZWRTZWdtZW50TWF0Y2hlckRhdGEiOnsic2VnbWVudE5hbWUiOiJuZXdfc2VnbWVudCJ9LCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJmcmVlIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbmV3X3NlZ21lbnQifSx7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjpudWxsLCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImZyZWUiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0");
    // workm with no sets
    private static final String splitChange4None = splitChangeV2("4", "3", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjUsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6W10sImNvbmRpdGlvbnMiOlt7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJJTl9TRUdNRU5UIiwibmVnYXRlIjpmYWxzZSwidXNlckRlZmluZWRTZWdtZW50TWF0Y2hlckRhdGEiOnsic2VnbWVudE5hbWUiOiJuZXdfc2VnbWVudCJ9LCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJmcmVlIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbmV3X3NlZ21lbnQifSx7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjpudWxsLCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImZyZWUiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0=");
    // workm with set_3
    private static final String splitChange4 = splitChangeV2("4", "3", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjQsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6WyJzZXRfMyJdLCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6ImNsaWVudCIsImF0dHJpYnV0ZSI6bnVsbH0sIm1hdGNoZXJUeXBlIjoiSU5fU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibmV3X3NlZ21lbnQifSwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOm51bGwsInVuYXJ5TnVtZXJpY01hdGNoZXJEYXRhIjpudWxsLCJiZXR3ZWVuTWF0Y2hlckRhdGEiOm51bGwsImJvb2xlYW5NYXRjaGVyRGF0YSI6bnVsbCwiZGVwZW5kZW5jeU1hdGNoZXJEYXRhIjpudWxsLCJzdHJpbmdNYXRjaGVyRGF0YSI6bnVsbH1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib24iLCJzaXplIjowfSx7InRyZWF0bWVudCI6Im9mZiIsInNpemUiOjB9LHsidHJlYXRtZW50IjoiZnJlZSIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJjb250YSIsInNpemUiOjB9XSwibGFiZWwiOiJpbiBzZWdtZW50IG5ld19zZWdtZW50In0seyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6ImNsaWVudCIsImF0dHJpYnV0ZSI6bnVsbH0sIm1hdGNoZXJUeXBlIjoiQUxMX0tFWVMiLCJuZWdhdGUiOmZhbHNlLCJ1c2VyRGVmaW5lZFNlZ21lbnRNYXRjaGVyRGF0YSI6bnVsbCwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOm51bGwsInVuYXJ5TnVtZXJpY01hdGNoZXJEYXRhIjpudWxsLCJiZXR3ZWVuTWF0Y2hlckRhdGEiOm51bGwsImJvb2xlYW5NYXRjaGVyRGF0YSI6bnVsbCwiZGVwZW5kZW5jeU1hdGNoZXJEYXRhIjpudWxsLCJzdHJpbmdNYXRjaGVyRGF0YSI6bnVsbH1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib24iLCJzaXplIjoxMDB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJmcmVlIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJjb250YSIsInNpemUiOjB9XSwibGFiZWwiOiJkZWZhdWx0IHJ1bGUifV19");
    // workm with set_1
    private static final String splitChange3 = splitChangeV2("3", "2", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjMsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6WyJzZXRfMSJdLCJjb25kaXRpb25zIjpbeyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6ImNsaWVudCIsImF0dHJpYnV0ZSI6bnVsbH0sIm1hdGNoZXJUeXBlIjoiSU5fU0VHTUVOVCIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjp7InNlZ21lbnROYW1lIjoibmV3X3NlZ21lbnQifSwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOm51bGwsInVuYXJ5TnVtZXJpY01hdGNoZXJEYXRhIjpudWxsLCJiZXR3ZWVuTWF0Y2hlckRhdGEiOm51bGwsImJvb2xlYW5NYXRjaGVyRGF0YSI6bnVsbCwiZGVwZW5kZW5jeU1hdGNoZXJEYXRhIjpudWxsLCJzdHJpbmdNYXRjaGVyRGF0YSI6bnVsbH1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib24iLCJzaXplIjowfSx7InRyZWF0bWVudCI6Im9mZiIsInNpemUiOjB9LHsidHJlYXRtZW50IjoiZnJlZSIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJjb250YSIsInNpemUiOjB9XSwibGFiZWwiOiJpbiBzZWdtZW50IG5ld19zZWdtZW50In0seyJjb25kaXRpb25UeXBlIjoiUk9MTE9VVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJrZXlTZWxlY3RvciI6eyJ0cmFmZmljVHlwZSI6ImNsaWVudCIsImF0dHJpYnV0ZSI6bnVsbH0sIm1hdGNoZXJUeXBlIjoiQUxMX0tFWVMiLCJuZWdhdGUiOmZhbHNlLCJ1c2VyRGVmaW5lZFNlZ21lbnRNYXRjaGVyRGF0YSI6bnVsbCwid2hpdGVsaXN0TWF0Y2hlckRhdGEiOm51bGwsInVuYXJ5TnVtZXJpY01hdGNoZXJEYXRhIjpudWxsLCJiZXR3ZWVuTWF0Y2hlckRhdGEiOm51bGwsImJvb2xlYW5NYXRjaGVyRGF0YSI6bnVsbCwiZGVwZW5kZW5jeU1hdGNoZXJEYXRhIjpudWxsLCJzdHJpbmdNYXRjaGVyRGF0YSI6bnVsbH1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib24iLCJzaXplIjoxMDB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJmcmVlIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJjb250YSIsInNpemUiOjB9XSwibGFiZWwiOiJkZWZhdWx0IHJ1bGUifV19");
    // workm with set_1, set_2
    private static final String splitChange2 = splitChangeV2("2", "1", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJjbGllbnQiLCJuYW1lIjoid29ya20iLCJ0cmFmZmljQWxsb2NhdGlvbiI6MTAwLCJ0cmFmZmljQWxsb2NhdGlvblNlZWQiOjE0NzM5MjIyNCwic2VlZCI6NTI0NDE3MTA1LCJzdGF0dXMiOiJBQ1RJVkUiLCJraWxsZWQiOmZhbHNlLCJkZWZhdWx0VHJlYXRtZW50Ijoib24iLCJjaGFuZ2VOdW1iZXIiOjIsImFsZ28iOjIsImNvbmZpZ3VyYXRpb25zIjp7fSwic2V0cyI6WyJzZXRfMSIsInNldF8yIl0sImNvbmRpdGlvbnMiOlt7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJJTl9TRUdNRU5UIiwibmVnYXRlIjpmYWxzZSwidXNlckRlZmluZWRTZWdtZW50TWF0Y2hlckRhdGEiOnsic2VnbWVudE5hbWUiOiJuZXdfc2VnbWVudCJ9LCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJmcmVlIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImluIHNlZ21lbnQgbmV3X3NlZ21lbnQifSx7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoiY2xpZW50IiwiYXR0cmlidXRlIjpudWxsfSwibWF0Y2hlclR5cGUiOiJBTExfS0VZUyIsIm5lZ2F0ZSI6ZmFsc2UsInVzZXJEZWZpbmVkU2VnbWVudE1hdGNoZXJEYXRhIjpudWxsLCJ3aGl0ZWxpc3RNYXRjaGVyRGF0YSI6bnVsbCwidW5hcnlOdW1lcmljTWF0Y2hlckRhdGEiOm51bGwsImJldHdlZW5NYXRjaGVyRGF0YSI6bnVsbCwiYm9vbGVhbk1hdGNoZXJEYXRhIjpudWxsLCJkZXBlbmRlbmN5TWF0Y2hlckRhdGEiOm51bGwsInN0cmluZ01hdGNoZXJEYXRhIjpudWxsfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjEwMH0seyJ0cmVhdG1lbnQiOiJvZmYiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImZyZWUiLCJzaXplIjowfSx7InRyZWF0bWVudCI6ImNvbnRhIiwic2l6ZSI6MH1dLCJsYWJlbCI6ImRlZmF1bHQgcnVsZSJ9XX0");
    // mauro_java with no sets
    private static final String noSetsSplitChange = splitChangeV2("2", "1", "0", "eyJ0cmFmZmljVHlwZU5hbWUiOiJ1c2VyIiwiaWQiOiJkNDMxY2RkMC1iMGJlLTExZWEtOGE4MC0xNjYwYWRhOWNlMzkiLCJuYW1lIjoibWF1cm9famF2YSIsInRyYWZmaWNBbGxvY2F0aW9uIjoxMDAsInRyYWZmaWNBbGxvY2F0aW9uU2VlZCI6LTkyMzkxNDkxLCJzZWVkIjotMTc2OTM3NzYwNCwic3RhdHVzIjoiQUNUSVZFIiwia2lsbGVkIjpmYWxzZSwiZGVmYXVsdFRyZWF0bWVudCI6Im9mZiIsImNoYW5nZU51bWJlciI6MTYwMjc5OTYzODM0NCwiYWxnbyI6MiwiY29uZmlndXJhdGlvbnMiOnt9LCJzZXRzIjpbXSwiY29uZGl0aW9ucyI6W3siY29uZGl0aW9uVHlwZSI6IldISVRFTElTVCIsIm1hdGNoZXJHcm91cCI6eyJjb21iaW5lciI6IkFORCIsIm1hdGNoZXJzIjpbeyJtYXRjaGVyVHlwZSI6IldISVRFTElTVCIsIm5lZ2F0ZSI6ZmFsc2UsIndoaXRlbGlzdE1hdGNoZXJEYXRhIjp7IndoaXRlbGlzdCI6WyJhZG1pbiIsIm1hdXJvIiwibmljbyJdfX1dfSwicGFydGl0aW9ucyI6W3sidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfV0sImxhYmVsIjoid2hpdGVsaXN0ZWQifSx7ImNvbmRpdGlvblR5cGUiOiJST0xMT1VUIiwibWF0Y2hlckdyb3VwIjp7ImNvbWJpbmVyIjoiQU5EIiwibWF0Y2hlcnMiOlt7ImtleVNlbGVjdG9yIjp7InRyYWZmaWNUeXBlIjoidXNlciJ9LCJtYXRjaGVyVHlwZSI6IklOX1NFR01FTlQiLCJuZWdhdGUiOmZhbHNlLCJ1c2VyRGVmaW5lZFNlZ21lbnRNYXRjaGVyRGF0YSI6eyJzZWdtZW50TmFtZSI6Im1hdXItMiJ9fV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6IlY0Iiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJ2NSIsInNpemUiOjB9XSwibGFiZWwiOiJpbiBzZWdtZW50IG1hdXItMiJ9LHsiY29uZGl0aW9uVHlwZSI6IlJPTExPVVQiLCJtYXRjaGVyR3JvdXAiOnsiY29tYmluZXIiOiJBTkQiLCJtYXRjaGVycyI6W3sia2V5U2VsZWN0b3IiOnsidHJhZmZpY1R5cGUiOiJ1c2VyIn0sIm1hdGNoZXJUeXBlIjoiQUxMX0tFWVMiLCJuZWdhdGUiOmZhbHNlfV19LCJwYXJ0aXRpb25zIjpbeyJ0cmVhdG1lbnQiOiJvbiIsInNpemUiOjB9LHsidHJlYXRtZW50Ijoib2ZmIiwic2l6ZSI6MTAwfSx7InRyZWF0bWVudCI6IlY0Iiwic2l6ZSI6MH0seyJ0cmVhdG1lbnQiOiJ2NSIsInNpemUiOjB9XSwibGFiZWwiOiJkZWZhdWx0IHJ1bGUifV19");

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private final AtomicInteger mSplitChangesHits = new AtomicInteger(0);
    private SplitRoomDatabase mRoomDb;

    @Before
    public void setUp() {
        mSplitChangesHits.set(0);
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
    }

    @Test
    public void sdkWithoutSetsConfiguredDoesExcludeUpdates() throws IOException, InterruptedException {
        /*
         * Initialize a factory with streaming enabled and no sets.
         *
         * Receive notification with new feature flag with no sets.
         *
         * Verify that the feature flag is added.
         */
        LinkedBlockingDeque<String> mStreamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, mStreamingData);

        int initialSplitsSize = mRoomDb.splitDao().getAll().size();

        // set up update listener
        CountDownLatch updateLatch = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(updateLatch));

        // push change
        pushToStreaming(mStreamingData, noSetsSplitChange);
        boolean updateAwait = updateLatch.await(5, TimeUnit.SECONDS);

        assertTrue(updateAwait);
        assertEquals(0, initialSplitsSize);
        assertEquals(1, mRoomDb.splitDao().getAll().size());
    }

    @Test
    public void sdkWithSetsConfiguredDeletedDueToEmptySets() throws IOException, InterruptedException {
        /*
         * Initialize a factory with set_1 & set_2 sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1", "set_2"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1"]}. It should process it since is still part of the config.Sets
         *
         * 3. Receive a SPLIT_UPDATE with {name:"test", "sets":[]}. The featureFlag should be removed.
         *
         */
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, "set_1", "set_2");

        // 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1", "set_2"]}
        CountDownLatch firstUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(firstUpdate));
        pushToStreaming(streamingData, splitChange2);
        boolean firstUpdateAwait = firstUpdate.await(5, TimeUnit.SECONDS);
        List<SplitEntity> entities = mRoomDb.splitDao().getAll();
        boolean firstUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"sets\":[\"set_1\",\"set_2\"]") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 2. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1"]}
        CountDownLatch secondUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(secondUpdate));
        pushToStreaming(streamingData, splitChange3);
        boolean secondUpdateAwait = secondUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean secondUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"sets\":[\"set_1\"]") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 3. Receive a SPLIT_UPDATE with {name:"test", "sets":[]}
        CountDownLatch thirdUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(thirdUpdate));
        pushToStreaming(streamingData, splitChange4None);
        boolean thirdUpdateAwait = thirdUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean thirdUpdateStored = entities.isEmpty();

        assertTrue(firstUpdateAwait);
        assertTrue(firstUpdateStored);
        assertTrue(secondUpdateAwait);
        assertTrue(secondUpdateStored);
        assertTrue(thirdUpdateAwait);
        assertTrue(thirdUpdateStored);
    }

    @Test
    public void sdkWithSetsConfiguredDeletedDueToNonMatchingSets() throws IOException, InterruptedException {
        /*
         * Initialize a factory with set_1 & set_2 sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1", "set_2"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_UPDATE with "sets":["set_1"]. The feature flag should be updated.
         *
         * 3. Receive a SPLIT_UPDATE with "sets":["set_3"]. The feature flag should be removed.
         *
         * 4. Receive a SPLIT_UPDATE with "sets":["set_3", "set_4"] No changes in storage.
         */

        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, "set_1", "set_2");

        // 1. Receive a SPLIT_UPDATE with "sets":["set_1", "set_2"]
        CountDownLatch firstUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(firstUpdate));
        pushToStreaming(streamingData, splitChange2);
        boolean firstUpdateAwait = firstUpdate.await(5, TimeUnit.SECONDS);
        List<SplitEntity> entities = mRoomDb.splitDao().getAll();
        boolean firstUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"sets\":[\"set_1\",\"set_2\"]") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 2. Receive a SPLIT_UPDATE with "sets":["set_1"]
        CountDownLatch secondUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(secondUpdate));
        pushToStreaming(streamingData, splitChange3);
        boolean secondUpdateAwait = secondUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean secondUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"sets\":[\"set_1\"]") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 3. Receive a SPLIT_UPDATE with "sets":["set_3"]
        CountDownLatch thirdUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(thirdUpdate));
        pushToStreaming(streamingData, splitChange4);
        boolean thirdUpdateAwait = thirdUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean thirdUpdateStored = entities.size() == 0;

        // 4. Receive a SPLIT_UPDATE with "sets":["set_3", "set_4"]
        CountDownLatch fourthUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(fourthUpdate));
        pushToStreaming(streamingData, splitChange5);
        boolean fourthUpdateAwait = fourthUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean fourthUpdateStored = entities.size() == 0;

        assertTrue(firstUpdateAwait);
        assertTrue(firstUpdateStored);
        assertTrue(secondUpdateAwait);
        assertTrue(secondUpdateStored);
        assertTrue(thirdUpdateAwait);
        assertTrue(thirdUpdateStored);
        assertTrue(fourthUpdateAwait);
        assertTrue(fourthUpdateStored);
    }

    @Test
    public void sdkWithSetsReceivesSplitKill() throws IOException, InterruptedException {
        /*
         * Initialize a factory with set_1 & set_2 sets configured.
         *
         * 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1", "set_2"]}. It should process it since is part of the config.Sets
         *
         * 2. Receive a SPLIT_KILL with {cn:2, name:"workm", "defaultTreatment":"off" }. The featureFlag should be killed and a fetch should be performed.
         */

        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, "set_1", "set_2");

        // 1. Receive a SPLIT_UPDATE with {name:"test", "sets":["set_1", "set_2"]}
        CountDownLatch firstUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(firstUpdate));
        pushToStreaming(streamingData, splitChange2);
        boolean firstUpdateAwait = firstUpdate.await(5, TimeUnit.SECONDS);
        List<SplitEntity> entities = mRoomDb.splitDao().getAll();
        boolean firstUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"sets\":[\"set_1\",\"set_2\"]") &&
                entities.get(0).getBody().contains("\"killed\":false") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 2. Receive a SPLIT_KILL with {cn:2, name:"test", "defaultTreatment":"off" }
        CountDownLatch secondUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(secondUpdate));
        pushToStreaming(streamingData, IntegrationHelper.splitKill("5", "workm"));
        boolean secondUpdateAwait = secondUpdate.await(5, TimeUnit.SECONDS);
        entities = mRoomDb.splitDao().getAll();
        boolean secondUpdateStored = entities.size() == 1 && entities.get(0).getBody().contains("\"killed\":true") &&
                entities.get(0).getBody().contains("\"name\":\"workm\"");

        // 3. A fetch is triggered due to the SPLIT_KILL
        boolean correctAmountOfChanges = mSplitChangesHits.get() == 3;

        assertTrue(firstUpdateAwait);
        assertTrue(firstUpdateStored);
        assertTrue(secondUpdateAwait);
        assertTrue(secondUpdateStored);
        assertTrue(correctAmountOfChanges);
    }

    @Test
    public void sdkWithSetsReceivesSplitKillForNonExistingFeatureFlag() throws IOException, InterruptedException {
        /*
         * Initialize a factory with set_1 & set_2 sets configured.
         *
         * 1. Receive a SPLIT_KILL with {cn:2, name:"workm", "defaultTreatment":"off" }. No changes in storage, a fetch should be performed.
         */
        LinkedBlockingDeque<String> streamingData = new LinkedBlockingDeque<>();
        SplitClient readyClient = getReadyClient(mContext, mRoomDb, streamingData, "set_1", "set_2");

        int initialEntities = mRoomDb.splitDao().getAll().size();

        // 1. Receive a SPLIT_KILL with {cn:2, name:"test", "defaultTreatment":"off" }
        CountDownLatch firstUpdate = new CountDownLatch(1);
        readyClient.on(SplitEvent.SDK_UPDATE, TestingHelper.testTask(firstUpdate));
        int initialChangesHits = mSplitChangesHits.get();
        pushToStreaming(streamingData, IntegrationHelper.splitKill("2", "workm"));
        boolean firstUpdateAwait = firstUpdate.await(5, TimeUnit.SECONDS);
        List<SplitEntity> entities = mRoomDb.splitDao().getAll();
        boolean firstUpdateStored = entities.isEmpty();

        // 2. A fetch is triggered due to the SPLIT_KILL
        int finalChangesHits = mSplitChangesHits.get();

        assertFalse(firstUpdateAwait);
        assertTrue(firstUpdateStored);
        assertEquals(initialEntities, 0);
        assertTrue(finalChangesHits > initialChangesHits);
    }

    @Nullable
    private SplitClient getReadyClient(
            Context mContext,
            SplitRoomDatabase splitRoomDatabase,
            BlockingQueue<String> streamingData,
            String... sets) throws IOException, InterruptedException {
        SplitClientConfig config = new TestableSplitConfigBuilder()
                .ready(30000)
                .trafficType("client")
                .enableDebug()
                .impressionsRefreshRate(1000)
                .impressionsCountersRefreshRate(1000)
                .syncConfig(SyncConfig.builder()
                        .addSplitFilter(SplitFilter.bySet(Arrays.asList(sets)))
                        .build())
                .featuresRefreshRate(2)
                .streamingEnabled(true)
                .eventFlushInterval(1000)
                .build();
        CountDownLatch authLatch = new CountDownLatch(1);
        Map<String, IntegrationHelper.ResponseClosure> responses = new HashMap<>();
        responses.put("splitChanges", (uri, httpMethod, body) -> {
            mSplitChangesHits.incrementAndGet();
            return new HttpResponseMock(200, IntegrationHelper.emptySplitChanges(-1, 1));
        });
        responses.put("mySegments/CUSTOMER_ID", (uri, httpMethod, body) -> new HttpResponseMock(200, IntegrationHelper.emptyMySegments()));
        responses.put("v2/auth", (uri, httpMethod, body) -> {
            authLatch.countDown();
            return new HttpResponseMock(200, IntegrationHelper.streamingEnabledToken());
        });

        HttpResponseMockDispatcher httpResponseMockDispatcher = IntegrationHelper.buildDispatcher(responses, streamingData);

        SplitFactory splitFactory = IntegrationHelper.buildFactory(
                IntegrationHelper.dummyApiKey(),
                IntegrationHelper.dummyUserKey(),
                config,
                mContext,
                new HttpClientMock(httpResponseMockDispatcher),
                splitRoomDatabase, null, null, null);

        CountDownLatch readyLatch = new CountDownLatch(1);
        SplitClient client = splitFactory.client();
        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecutionView(SplitClient client) {
                readyLatch.countDown();
            }
        });

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);
        boolean authAwait = authLatch.await(5, TimeUnit.SECONDS);
        TestingHelper.pushKeepAlive(streamingData);

        return (await && authAwait) ? client : null;
    }

    private static void pushToStreaming(LinkedBlockingDeque<String> streamingData, String message) throws InterruptedException {
        try {
            streamingData.put(message + "" + "\n");

            Logger.d("Pushed message: " + message);
        } catch (InterruptedException ignored) {
        }
    }
}
