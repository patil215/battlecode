package revisedstrat;

/**
 * Created by patil215 on 1/12/17.
 */
public class CommunicationsManager {

    /*
    Communications that need to be stored:
    - Enemy locations
    - Archon need help locations
    - Gardener need help locations
    - Spam broadcast locations (potentially)
    - Map information
    - Unit tallies (ex. number of scouts, etc)
     */

    private final int ENEMY_LOC_START = 0;
    private final int ENEMY_LOC_END = 20;

    private final int ARCHON_HELP_LOC_START = 21;
    private final int ARCHON_HELP_LOC_END = 24;

    private final int GARDENER_HELP_LOC_START = 25;
    private final int GARDENER_HELP_LOC_END = 75;

    public void addEnemyLocation() {

    }

    public void getRecentEnemyLocation() {

    }

    public void getArchonHelpLocation() {
        // return null if no archon needs help
    }

    public void saveArchonHelpLocation() {

    }

    public void getGardenerHelpLocation() {

    }

    public void saveGardenerHelpLocation() {

    }

    private static int zipLocation(int x, int y) {
        return (int) (((0.5) * (x + y) * (x + y + 1)) + y);
    }

    private static int[] unzipLocation(int z) {
        int w = (int) Math.floor((Math.sqrt(8 * z + 1) - 1) / 2);
        int t = ((int) Math.pow(w, 2) + w) / 2;
        int y = z - t;
        int x = w - y;
        return new int[]{x, y};
    }

}
