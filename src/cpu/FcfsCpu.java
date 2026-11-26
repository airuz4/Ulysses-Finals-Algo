// Owner: [Member Name]
package cpu;

import java.util.ArrayList;
import java.util.List;

// First-Come First-Served (non-preemptive).
public class FcfsCpu implements CpuScheduler {

    @Override
    public CpuResult simulate(List<Process> input) {
        CpuResult result = new CpuResult();

        // Work on copies so the user's input list is never changed.
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            Process p = input.get(i);
            list.add(new Process(p.pid, p.arrival, p.burst, p.priority));
        }
        if (list.isEmpty()) {
            return result;
        }

        // Sorted order = order of service. Tie-break: earlier arrival, then lower PID.
        List<Process> order = new ArrayList<>(list);
        for (int i = 0; i < order.size() - 1; i++) {
            for (int j = 0; j < order.size() - 1 - i; j++) {
                if (comesAfter(order.get(j), order.get(j + 1))) {
                    Process temp = order.get(j);
                    order.set(j, order.get(j + 1));
                    order.set(j + 1, temp);
                }
            }
        }

        int time = 0;   // the clock always starts at 0
        int busy = 0;

        for (int i = 0; i < order.size(); i++) {
            Process p = order.get(i);

            // If nobody has arrived yet, the CPU sits idle until this process arrives.
            if (time < p.arrival) {
                result.gantt.add(new GanttBlock("IDLE", time, p.arrival));
                time = p.arrival;
            }

            // Run the whole burst (non-preemptive).
            p.startTime = time;
            time = time + p.burst;
            p.completion = time;
            busy = busy + p.burst;
            p.remaining = 0;
            result.gantt.add(new GanttBlock("P" + p.pid, p.startTime, time));
        }

        result.finish(list, busy, time);
        return result;
    }

    // Returns true if a should be served AFTER b.
    private boolean comesAfter(Process a, Process b) {
        if (a.arrival != b.arrival) {
            return a.arrival > b.arrival;
        }
        return a.pid > b.pid;
    }
}