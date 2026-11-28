package cpu;

import java.util.ArrayList;
import java.util.List;

// Shared interface for all CPU scheduling algorithms.
// It also holds the data model classes used by the algorithms and by the UI.
public interface CpuScheduler {

    // Runs the algorithm on a copy of the given processes and returns the result.
    CpuResult simulate(List<Process> processes);

    // Returns fresh copies so an algorithm never modifies the caller's input list.
    static List<Process> copyOf(List<Process> input) {
        List<Process> copy = new ArrayList<>();
        for (Process p : input) {
            copy.add(new Process(p.pid, p.arrival, p.burst, p.priority));
        }
        return copy;
    }

    // One process (one row of the input table).
    class Process {
        public int pid;
        public int arrival;
        public int burst;
        public int priority;

        // Values filled in by the simulation
        public int remaining;   // burst time still left to run
        public int startTime;   // first time the CPU ran this process (-1 = not started)
        public int completion;  // Completion Time (CT)
        public int turnaround;  // TAT = CT - Arrival
        public int waiting;     // WT  = TAT - Burst
        public int response;    // RT  = First start - Arrival

        public Process(int pid, int arrival, int burst, int priority) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.remaining = burst;
            this.startTime = -1;
        }
    }

    // One block of the Gantt chart: a process (or "IDLE") from start to end.
    class GanttBlock {
        public String label;
        public int start;
        public int end;

        public GanttBlock(String label, int start, int end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }
    }

    // Everything the UI needs to show after a simulation.
    class CpuResult {
        public List<Process> processes = new ArrayList<>();   // same order as the input table
        public List<GanttBlock> gantt = new ArrayList<>();
        public int totalTime;             // clock value when the last process finishes
        public int busyTime;              // time units where the CPU ran a process
        public double avgWT;
        public double avgTAT;
        public double avgRT;
        public double cpuUtilization;     // in percent
        public double throughput;         // processes per time unit

        // Computes TAT, WT, RT for each process, then all averages and totals.
        public void finish(List<Process> list, int busyTime, int totalTime) {
            this.processes = list;
            this.busyTime = busyTime;
            this.totalTime = totalTime;

            double sumWT = 0;
            double sumTAT = 0;
            double sumRT = 0;
            for (Process p : list) {
                p.turnaround = p.completion - p.arrival;
                p.waiting = p.turnaround - p.burst;
                p.response = p.startTime - p.arrival;
                sumWT += p.waiting;
                sumTAT += p.turnaround;
                sumRT += p.response;
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
}