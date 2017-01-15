package revisedevasionstrat;

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

    public static double getStrideRadius(RobotType rt) {
        switch (rt) {
            case ARCHON:
            case TANK:
            case SOLDIER: {
                return 1;
            }

            case GARDENER: {
                return 2;
            }

            case SCOUT: {
                return 2.5;
            }

            case LUMBERJACK: {
                return 1.5;
            }

            default: return 1;
        }
    }
}
