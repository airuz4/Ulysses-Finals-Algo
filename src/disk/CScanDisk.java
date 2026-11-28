// Owner: Airuz Balungay
package disk;

import java.util.ArrayList;
import java.util.List;

// C-SCAN: sweep in one direction; if requests remain on the other side, go to the
// physical end of the disk, jump to the opposite end, and keep the same direction.
public class CScanDisk implements DiskScheduler {

    @Override
    public DiskResult simulate(List<Integer> requests, int initialHead, int diskSize, String direction) {
        DiskResult result = new DiskResult(initialHead);
        List<Integer> sorted = DiskScheduler.sortedValidRequests(requests, diskSize);
        List<Integer> pending = new ArrayList<>();
        int lastCylinder = diskSize - 1;
        int boundary;
        int opposite;

        // Step 1: serve requests in the chosen direction; save the rest for after the wrap.
        if (direction.equals("Right")) {
            boundary = lastCylinder;
            opposite = 0;
            for (int r : sorted) {
                if (r >= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pending.add(r);
                }
            }
        } else {
            boundary = 0;
            opposite = lastCylinder;
            for (int i = sorted.size() - 1; i >= 0; i--) {
                int r = sorted.get(i);
                if (r <= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pending.add(r);   // stored largest first
                }
            }
        }

        // Step 2: only wrap around if requests remain on the other side.
        if (!pending.isEmpty()) {
            if (result.currentHead != boundary) {
                result.move(boundary, "boundary " + boundary, false, false);
            }

            // If the first remaining request sits on the opposite boundary, the jump serves it directly.
            int startIndex = 0;
            if (pending.get(0) == opposite) {
                result.move(opposite, "return jump", true, true);
                startIndex = 1;
            } else {
                result.move(opposite, "return jump", true, false);
            }

            for (int i = startIndex; i < pending.size(); i++) {
                result.move(pending.get(i), "", false, true);
            }
        }

        result.finish(sorted.size());
        return result;
    }
}