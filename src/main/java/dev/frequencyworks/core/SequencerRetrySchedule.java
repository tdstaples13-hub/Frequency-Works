package dev.frequencyworks.core;

/** Pure scheduling rules for a blocked Item Sequencer operation.
 * Only world game ticks count; unloaded chunks never run background attempts.
 */
public final class SequencerRetrySchedule {
    private SequencerRetrySchedule() {}
    public static final long FAST_WINDOW = 1_200L; // 60 seconds
    public static final long FAST_INTERVAL = 20L; // 1 second
    public static final long SLOW_INTERVAL = 2_000L; // 100 seconds

    /** firstFailureTick persists until an actual item transfer or instruction
     * completes. The first block attempt schedules another check in 20 ticks.
     */
    public static long nextAttempt(long firstFailureTick,long nowTick) {
        long elapsed = Math.max(0L,nowTick-firstFailureTick);
        return nowTick+(elapsed<FAST_WINDOW?FAST_INTERVAL:SLOW_INTERVAL);
    }
}
