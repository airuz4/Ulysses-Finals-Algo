// Owner: Johann Gomez
package cpu;

import java.util.List;

// Preemptive Priority scheduling. Lower number = higher priority. No aging.
// The simulation moves one time unit at a time so preemption is easy to follow.
public class PreemptivePriorityCpu implements CpuScheduler {

    @Override
    public CpuResult simulate(List<Process> input) {
        CpuResult result = new CpuResult();
        List<Process> list = CpuScheduler.copyOf(input);
        int n = list.size();
        if (n == 0) {
            return result;
        }
        for (Process p : list) {
            if (p.burst <= 0) {
                throw new IllegalArgumentException("Burst time must be greater than 0.");
            }
        }

        int time = 0;        // the clock always starts at 0
        int busy = 0;
        int done = 0;

        while (done < n) {
            // Pick the best ready process. Because ties break by earlier arrival,
            // a newly arrived process with equal priority never preempts a running one.
            Process best = null;
            for (Process p : list) {
                if (p.remaining > 0 && p.arrival <= time) {
                    if (best == null || isBetter(p, best)) {
                        best = p;
                    }
                }
            }

            // Nobody is ready: the CPU is idle for this time unit.
            if (best == null) {
                addBlock(result, "IDLE", time);
                time++;
                continue;
            }

            // Remember the first time this process gets the CPU (for Response Time).
            if (best.startTime == -1) {
                best.startTime = time;
            }

            // Run the process for 1 time unit.
            addBlock(result, "P" + best.pid, time);
            best.remaining--;
            time++;
            busy++;

            if (best.remaining == 0) {
                best.completion = time;
                done++;
            }
        }

        result.finish(list, busy, time);
        return result;
    }

    // Tie-break order: lower priority number, then earlier arrival, then lower PID.
    private boolean isBetter(Process a, Process b) {
        if (a.priority != b.priority) {
            return a.priority < b.priority;
        }
        if (a.arrival != b.arrival) {
            return a.arrival < b.arrival;
        }
        return a.pid < b.pid;
    }

    // Adds 1 time unit to the Gantt chart. If the previous block has the same label
    // and ends right now, we just extend it instead of creating a new block.
    private void addBlock(CpuResult result, String label, int time) {
        int size = result.gantt.size();
        if (size > 0) {
            GanttBlock last = result.gantt.get(size - 1);
            if (last.label.equals(label) && last.end == time) {
                last.end = time + 1;
                return;
            }
        }
        result.gantt.add(new GanttBlock(label, time, time + 1));
    }
}