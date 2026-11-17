// Owner: [Member Name]
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// C-SCAN: go to the physical end of the disk, jump to the other end, keep the same direction.
public class CScanDisk {

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

        int lastCylinder = diskSize - 1;

        if (direction.equals("Right")) {
            // Step 1: serve every request >= head, going up. (Duplicates cost 0 movement.)
            int pendingBelow = 0;
            for (int i = 0; i < sorted.size(); i++) {
                int r = sorted.get(i);
                if (r >= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pendingBelow++;
                }
            }

            // Step 2: always go to the physical boundary N-1 (unless already there).
            if (result.currentHead != lastCylinder) {
                result.move(lastCylinder, "boundary " + lastCylinder, false, false);
            }

            // Step 3: only if requests remain below, return jump to 0, then keep moving Right.
            if (pendingBelow > 0) {
                result.move(0, "return jump", true, false);
                for (int i = 0; i < sorted.size(); i++) {
                    int r = sorted.get(i);
                    if (r < initialHead) {
                        result.move(r, "", false, true);
                    }
                }
            }
        } else {
            // Direction is "Left": mirror image of the code above.
            // Step 1: serve every request <= head, going down (largest first).
            int pendingAbove = 0;
            for (int i = sorted.size() - 1; i >= 0; i--) {
                int r = sorted.get(i);
                if (r <= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pendingAbove++;
                }
            }

            // Step 2: go to the physical boundary 0 (unless already there).
            if (result.currentHead != 0) {
                result.move(0, "boundary 0", false, false);
            }

            // Step 3: if requests remain above, jump to N-1 and keep moving Left.
            if (pendingAbove > 0) {
                result.move(lastCylinder, "return jump", true, false);
                for (int i = sorted.size() - 1; i >= 0; i--) {
                    int r = sorted.get(i);
                    if (r > initialHead) {
                        result.move(r, "", false, true);
                    }
                }
            }
        }

        result.finish(sorted.size());
        return result;
    }

    static void printResult(String direction, DiskResult result) {
        System.out.println("=== C-SCAN (" + direction + ") ===");
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