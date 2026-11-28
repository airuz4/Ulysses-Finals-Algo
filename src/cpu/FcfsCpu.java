// Owner: Carlo Cuison
package cpu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// First-Come First-Served (non-preemptive).
public class FcfsCpu implements CpuScheduler {

    @Override
    public CpuResult simulate(List<Process> input) {
        CpuResult result = new CpuResult();
        List<Process> list = CpuScheduler.copyOf(input);
        if (list.isEmpty()) {
            return result;
        }

        // Sorted order = order of service. Tie-break: earlier arrival, then lower PID.
        List<Process> order = new ArrayList<>(list);
        order.sort(Comparator.comparingInt((Process p) -> p.arrival).thenComparingInt(p -> p.pid));

        int time = 0;   // the clock always starts at 0
        int busy = 0;

        for (Process p : order) {
            // If nobody has arrived yet, the CPU sits idle until this process arrives.
            if (time < p.arrival) {
                result.gantt.add(new GanttBlock("IDLE", time, p.arrival));
                time = p.arrival;
            }

            // Run the whole burst (non-preemptive).
            p.startTime = time;
            time += p.burst;
            p.completion = time;
            busy += p.burst;
            p.remaining = 0;
            result.gantt.add(new GanttBlock("P" + p.pid, p.startTime, time));
        }

        result.finish(list, busy, time);
        return result;
    }
}