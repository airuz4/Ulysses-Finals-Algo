package disk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Shared interface for all disk scheduling algorithms.
public interface DiskScheduler {

    // direction is "Right" (toward N-1) or "Left" (toward 0).
    DiskResult simulate(List<Integer> requests, int initialHead, int diskSize, String direction);

    // Keeps only valid cylinders (0 to diskSize-1) and returns them sorted ascending.
    // Requests equal to the head and duplicates are kept on purpose.
    static List<Integer> sortedValidRequests(List<Integer> requests, int diskSize) {
        List<Integer> sorted = new ArrayList<>();
        for (int r : requests) {
            if (r >= 0 && r < diskSize) {
                sorted.add(r);
            }
        }
        Collections.sort(sorted);
        return sorted;
    }

    // Everything the UI needs to show after a simulation.
    class DiskResult {
        public List<Integer> path = new ArrayList<>();          // every head position, starting at the initial head
        public List<Boolean> jumpFlags = new ArrayList<>();     // same size as path; true if we JUMPED to that position
        public List<Boolean> requestFlags = new ArrayList<>();  // same size as path; false for non-request boundary stops
        public List<Integer> servedOrder = new ArrayList<>();   // only the requests, in the order they were served
        public List<String> steps = new ArrayList<>();          // e.g. "|53 - 98| = 45"
        public int totalHeadMovement;
        public double averageSeekDistance;
        public int currentHead;             // where the head is right now during the simulation

        public DiskResult(int initialHead) {
            currentHead = initialHead;
            path.add(initialHead);
            jumpFlags.add(false);
            requestFlags.add(true);
        }

        // Moves the head to a new cylinder and records the step.
        // note      = extra text for the step list (e.g. "return jump"), or "" for none
        // isJump    = true for a return jump (the graph can draw it differently)
        // isRequest = true if this position is an actual request being served
        public void move(int to, String note, boolean isJump, boolean isRequest) {
            int distance = Math.abs(currentHead - to);
            String text = "|" + currentHead + " - " + to + "| = " + distance;
            if (!note.isEmpty()) {
                text = text + "  (" + note + ")";
            }
            steps.add(text);

            totalHeadMovement += distance;
            currentHead = to;
            path.add(to);
            jumpFlags.add(isJump);
            requestFlags.add(isRequest);
            if (isRequest) {
                servedOrder.add(to);
            }
        }

        // Average Seek Distance = Total Head Movement / Number of Requests.
        public void finish(int requestCount) {
            if (requestCount > 0) {
                averageSeekDistance = (double) totalHeadMovement / requestCount;
            } else {
                averageSeekDistance = 0;
            }
        }
    }
}