// Owner: [Member Name]
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// C-LOOK: go only as far as the last request, jump to the furthest request on the
// other side, keep the same direction.
public class CLookDisk {

    static class DiskResult {
        List<Integer> path;
        List<Boolean> jumpFlags;
        List<Integer> servedOrder;
        List<String> steps;
        int totalHeadMovement;
        double averageSeekDistance;
        int currentHead;

        DiskResult(int initialHead) {
            path = new ArrayList<Integer>();
            jumpFlags = new ArrayList<Boolean>();
            servedOrder = new ArrayList<Integer>();
            steps = new ArrayList<String>();
            totalHeadMovement = 0;
            averageSeekDistance = 0;
            currentHead = initialHead;
            path.add(initialHead);
            jumpFlags.add(false);
        }

        // Moves the head to a new cylinder and records the step.
        void move(int to, String note, boolean isJump, boolean isRequest) {
            int distance = Math.abs(currentHead - to);
            String text = "|" + currentHead + " - " + to + "| = " + distance;
            if (note.length() > 0) {
                text = text + "  (" + note + ")";
            }
            steps.add(text);

            totalHeadMovement = totalHeadMovement + distance;
            currentHead = to;
            path.add(to);
            jumpFlags.add(isJump);
            if (isRequest) {
                servedOrder.add(to);
            }
        }

        // Average Seek Distance = Total Head Movement / Number of Requests.
        void finish(int requestCount) {
            if (requestCount > 0) {
                averageSeekDistance = (double) totalHeadMovement / requestCount;
            } else {
                averageSeekDistance = 0;
            }
        }
    }

    static DiskResult simulate(List<Integer> requests, int initialHead, int diskSize, String direction) {
        DiskResult result = new DiskResult(initialHead);

        // Keep only valid cylinders (0 to diskSize-1) and sort them.
        // Requests equal to the head are kept on purpose.
        List<Integer> sorted = new ArrayList<Integer>();
        for (int i = 0; i < requests.size(); i++) {
            int r = requests.get(i);
            if (r >= 0 && r < diskSize) {
                sorted.add(r);
            }
        }
        Collections.sort(sorted);

        if (direction.equals("Right")) {
            // Step 1: serve every request >= head, going up. Stop at the last request.
            List<Integer> pendingBelow = new ArrayList<Integer>();
            for (int i = 0; i < sorted.size(); i++) {
                int r = sorted.get(i);
                if (r >= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pendingBelow.add(r);
                }
            }

            // Step 2: if requests remain below, jump to the smallest one, then continue Right.
            for (int i = 0; i < pendingBelow.size(); i++) {
                int r = pendingBelow.get(i);
                if (i == 0) {
                    result.move(r, "return jump", true, true);
                } else {
                    result.move(r, "", false, true);
                }
            }
        } else {
            // Direction is "Left": mirror image of the code above.
            // Step 1: serve every request <= head, going down (largest first).
            List<Integer> pendingAbove = new ArrayList<Integer>();
            for (int i = sorted.size() - 1; i >= 0; i--) {
                int r = sorted.get(i);
                if (r <= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pendingAbove.add(r);   // stored largest first
                }
            }

            // Step 2: if requests remain above, jump to the largest one, then continue Left.
            for (int i = 0; i < pendingAbove.size(); i++) {
                int r = pendingAbove.get(i);
                if (i == 0) {
                    result.move(r, "return jump", true, true);
                } else {
                    result.move(r, "", false, true);
                }
            }
        }

        result.finish(sorted.size());
        return result;
    }

    static void printResult(String direction, DiskResult result) {
        System.out.println("=== C-LOOK (" + direction + ") ===");
        System.out.println("Steps:");
        for (int i = 0; i < result.steps.size(); i++) {
            System.out.println("  " + result.steps.get(i));
        }

        System.out.print("Path: ");
        for (int i = 0; i < result.path.size(); i++) {
            if (i > 0) {
                if (result.jumpFlags.get(i)) {
                    System.out.print(" ~> ");
                } else {
                    System.out.print(" -> ");
                }
            }
            System.out.print(result.path.get(i));
        }
        System.out.println();

        System.out.println("Served order: " + result.servedOrder);
        System.out.println("Total head movement: " + result.totalHeadMovement);
        System.out.printf("Average seek distance: %.2f%n", result.averageSeekDistance);
        System.out.println();
    }

    public static void main(String[] args) {
        List<Integer> requests = new ArrayList<Integer>();
        requests.add(98);
        requests.add(183);
        requests.add(37);
        requests.add(122);
        requests.add(14);
        requests.add(124);
        requests.add(65);
        requests.add(67);

        int initialHead = 53;
        int diskSize = 200;

        DiskResult right = simulate(requests, initialHead, diskSize, "Right");
        printResult("Right", right);

        DiskResult left = simulate(requests, initialHead, diskSize, "Left");
        printResult("Left", left);
    }
}