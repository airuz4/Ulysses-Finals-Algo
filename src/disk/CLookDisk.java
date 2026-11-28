// Owner: Justin Aviles
package disk;

import java.util.ArrayList;
import java.util.List;

// C-LOOK: go only as far as the last request, jump to the furthest request on the
// other side, keep the same direction.
public class CLookDisk implements DiskScheduler {

    @Override
    public DiskResult simulate(List<Integer> requests, int initialHead, int diskSize, String direction) {
        DiskResult result = new DiskResult(initialHead);
        List<Integer> sorted = DiskScheduler.sortedValidRequests(requests, diskSize);
        List<Integer> pending = new ArrayList<>();

        // Step 1: serve requests in the chosen direction; save the rest for after the jump.
        if (direction.equals("Right")) {
            for (int r : sorted) {
                if (r >= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pending.add(r);
                }
            }
        } else {
            for (int i = sorted.size() - 1; i >= 0; i--) {
                int r = sorted.get(i);
                if (r <= initialHead) {
                    result.move(r, "", false, true);
                } else {
                    pending.add(r);   // stored largest first
                }
            }
        }

        // Step 2: if requests remain on the other side, jump straight to the furthest one,
        // then continue sweeping in the same direction.
        for (int i = 0; i < pending.size(); i++) {
            result.move(pending.get(i), i == 0 ? "return jump" : "", i == 0, true);
        }

        result.finish(sorted.size());
        return result;
    }
}