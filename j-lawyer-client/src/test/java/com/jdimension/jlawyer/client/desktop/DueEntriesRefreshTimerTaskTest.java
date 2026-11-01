package com.jdimension.jlawyer.client.desktop;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * When the desktop due list is reloaded automatically: only while the desktop is in front, on a
 * changed calendar version, after a day change and once the last load is older than the maximum
 * age.
 */
public class DueEntriesRefreshTimerTaskTest {

    private static final long VERSION = 42;

    private final AtomicInteger versionReads = new AtomicInteger();
    private final AtomicInteger reloads = new AtomicInteger();

    private static long at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 11, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private DueEntriesRefreshTimerTask task(boolean active, long serverVersion, long loadedAt, long now) {
        return new DueEntriesRefreshTimerTask(() -> active, () -> {
            versionReads.incrementAndGet();
            return serverVersion;
        }, () -> VERSION, () -> loadedAt, reloads::incrementAndGet, () -> now);
    }

    @Test
    public void nothingHappensWhileTheDesktopIsNotInFront() {
        task(false, VERSION + 1, at(2, 10, 0), at(3, 10, 0)).run();
        assertEquals(0, versionReads.get());
        assertEquals(0, reloads.get());
    }

    @Test
    public void unchangedVersionOnlyChecksTheVersion() {
        task(true, VERSION, at(2, 10, 0), at(2, 10, 5)).run();
        assertEquals(1, versionReads.get());
        assertEquals(0, reloads.get());
    }

    @Test
    public void changedVersionReloads() {
        task(true, VERSION + 1, at(2, 10, 0), at(2, 10, 1)).run();
        assertEquals(1, reloads.get());
    }

    @Test
    public void unreachableServerDoesNotReload() {
        new DueEntriesRefreshTimerTask(() -> true, () -> {
            throw new Exception("server down");
        }, () -> VERSION, () -> at(2, 10, 0), reloads::incrementAndGet, () -> at(2, 10, 1)).run();
        assertEquals(0, reloads.get());
    }

    @Test
    public void dayChangeReloadsWithoutVersionChange() {
        task(true, VERSION, at(2, 23, 55), at(3, 0, 1)).run();
        assertEquals(1, reloads.get());
    }

    @Test
    public void oldListReloadsWithoutVersionChange() {
        task(true, VERSION, at(2, 10, 0), at(2, 10, 15)).run();
        assertEquals(1, reloads.get());
    }

    @Test
    public void listNeverLoadedReloads() {
        task(true, VERSION, 0, at(2, 10, 0)).run();
        assertEquals(1, reloads.get());
    }

    @Test
    public void stoppedTaskDoesNothing() {
        DueEntriesRefreshTimerTask t = task(true, VERSION + 1, at(2, 10, 0), at(2, 10, 1));
        t.stop();
        t.run();
        assertEquals(0, versionReads.get());
        assertEquals(0, reloads.get());
    }
}
