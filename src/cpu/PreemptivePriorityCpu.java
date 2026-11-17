// Owner: [Member Name]
import java.util.ArrayList;
import java.util.List;

// Preemptive Priority scheduling. Lower number = higher priority. No aging.
// The simulation moves one time unit at a time so preemption is easy to follow.
public class PreemptivePriorityCpu {

    static class Process {
        int pid;
        int arrival;
        int burst;
        int priority;
        int remaining;
        int startTime;
        int completion;
        int turnaround;
        int waiting;
        int response;

        Process(int pid, int arrival, int burst, int priority) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.remaining = burst;
            this.startTime = -1;
        }
    }

    static class GanttBlock {
        String label;
        int start;
        int end;

        GanttBlock(String label, int start, int end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    static class CpuResult {
        List<Process> processes = new ArrayList<Process>();
        List<GanttBlock> gantt = new ArrayList<GanttBlock>();
        int totalTime;
        int busyTime;
        double avgWT;
        double avgTAT;
        double avgRT;
        double cpuUtilization;
        double throughput;

        void finish(List<Process> list, int busyTime, int totalTime) {
            this.processes = list;
            this.busyTime = busyTime;
            this.totalTime = totalTime;

            double sumWT = 0;
            double sumTAT = 0;
            double sumRT = 0;
            for (int i = 0; i < list.size(); i++) {
                Process p = list.get(i);
                p.turnaround = p.completion - p.arrival;
                p.waiting = p.turnaround - p.burst;
                p.response = p.startTime - p.arrival;
                sumWT = sumWT + p.waiting;
                sumTAT = sumTAT + p.turnaround;
                sumRT = sumRT + p.response;
            }

            int n = list.size();
            if (n > 0) {
                avgWT = sumWT / n;
                avgTAT = sumTAT / n;
                avgRT = sumRT / n;
            }
            if (totalTime > 0) {
                cpuUtilization = ((double) busyTime / totalTime) * 100.0;
                throughput = (double) n / totalTime;
            }
        }
    }

    static CpuResult simulate(List<Process> input) {
        CpuResult result = new CpuResult();

        // Work on copies so the input list is never changed.
        List<Process> list = new ArrayList<Process>();
        for (int i = 0; i < input.size(); i++) {
            Process p = input.get(i);
            list.add(new Process(p.pid, p.arrival, p.burst, p.priority));
        }
        int n = list.size();
        if (n == 0) {
            return result;
        }

        int time = 0;        // the clock always starts at 0
        int busy = 0;
        int done = 0;
        Process current = null;   // process that ran in the previous time unit

        while (done < n) {
            // Find the best process among those that have arrived and are not finished.
            Process best = null;
            for (int i = 0; i < n; i++) {
                Process p = list.get(i);
                if (p.remaining > 0 && p.arrival <= time) {
                    if (best == null || isBetter(p, best)) {
                        best = p;
                    }
                }
            }

            // Nobody is ready: the CPU is idle for this time unit.
            if (best == null) {
                current = null;
                addBlock(result, "IDLE", time);
                time++;
                continue;
            }

            // Keep the current process unless the best one has a STRICTLY lower
            // priority number (equal priority does not preempt).
            if (current == null) {
                current = best;
            } else if (best.priority < current.priority) {
                current = best;
            }

            // First time this process gets the CPU (for Response Time).
            if (current.startTime == -1) {
                current.startTime = time;
            }

            // Run the process for 1 time unit.
            addBlock(result, "P" + current.pid, time);
            current.remaining--;
            time++;
            busy++;

            if (current.remaining == 0) {
                current.completion = time;
                done++;
                current = null;
            }
        }

        result.finish(list, busy, time);
        return result;
    }

    // Tie-break order: lower priority number, then earlier arrival, then lower PID.
    static boolean isBetter(Process a, Process b) {
        if (a.priority != b.priority) {
            return a.priority < b.priority;
        }
        if (a.arrival != b.arrival) {
            return a.arrival < b.arrival;
        }
        return a.pid < b.pid;
    }

    // Adds 1 time unit to the Gantt chart. If the previous block has the same label
    // and ends right now, extend it instead of creating a new block.
    static void addBlock(CpuResult result, String label, int time) {
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

    public static void main(String[] args) {
        List<Process> processes = new ArrayList<Process>();
        processes.add(new Process(1, 0, 8, 3));
        processes.add(new Process(2, 1, 4, 1));
        processes.add(new Process(3, 2, 2, 4));
        processes.add(new Process(4, 3, 1, 2));
        processes.add(new Process(5, 25, 3, 1));

        CpuResult result = simulate(processes);

        System.out.println("=== Preemptive Priority Gantt Chart ===");
        for (int i = 0; i < result.gantt.size(); i++) {
            GanttBlock b = result.gantt.get(i);
            System.out.println(b.start + " -> " + b.label + " -> " + b.end);
        }

        System.out.println();
        System.out.println("PID\tAT\tBT\tPR\tCT\tTAT\tWT\tRT");
        for (int i = 0; i < result.processes.size(); i++) {
            Process p = result.processes.get(i);
            System.out.println("P" + p.pid + "\t" + p.arrival + "\t" + p.burst + "\t" + p.priority + "\t"
                    + p.completion + "\t" + p.turnaround + "\t" + p.waiting + "\t" + p.response);
        }

        System.out.println();
        System.out.printf("Average WT  : %.2f%n", result.avgWT);
        System.out.printf("Average TAT : %.2f%n", result.avgTAT);
        System.out.printf("Average RT  : %.2f%n", result.avgRT);
        System.out.printf("CPU Utilization: %.2f%%%n", result.cpuUtilization);
        System.out.printf("Throughput  : %.4f processes/unit%n", result.throughput);
        System.out.println("Total time  : " + result.totalTime);
        System.out.println("Busy time   : " + result.busyTime);
    }
}