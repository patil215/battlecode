package revisedstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/12/17.
 */
public class Utils {

    public static MapLocation getAvgArchonLocations(RobotController rc, Team team) {
        float avgX = 0;
        float avgY = 0;
        MapLocation[] initialLocs = rc.getInitialArchonLocations(team);
        for(MapLocation loc : initialLocs) {
            avgX += loc.x;
            avgY += loc.y;
        }
        return new MapLocation(avgX / initialLocs.length, avgY / initialLocs.length);
    }

    public static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    public static Direction diagonalDirection() {
        int[] diagonals = new int[] {0, 45, 90, 135, 180, 225, 270, 315, 360};
        return new Direction(diagonals[(int) (Math.random() * diagonals.length)]);
    }
}
