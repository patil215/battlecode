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
}
