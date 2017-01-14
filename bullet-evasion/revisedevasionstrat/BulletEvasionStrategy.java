package revisedevasionstrat;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by dhruv on 1/14/17.
 */

class AggregateEvasion {
    static RobotController rc;

    public AggregateEvasion(RobotController rc) {
        this.rc = rc;
    }

    static int move() throws GameActionException {
        float angleThreshold = 30;
        float distanceThreshold = 5;
        BulletInfo[] dangerousBullets = (BulletInfo[]) Arrays.stream(rc.senseNearbyBullets())
                // Consider only those bullets which pose an immediate threat
                .filter(b -> isImminentThreat(b, angleThreshold, distanceThreshold))
                .toArray();

        if (dangerousBullets.length == 0) return -1;

        MapLocation[] lookAhead = new MapLocation[dangerousBullets.length];
        for (int i = 0; i < dangerousBullets.length; i++) {
            BulletInfo bullet = dangerousBullets[i];
            lookAhead[i] = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
        }

        Direction d = new Direction(0, 0);
        int n = 4; // directions to check
        double stride = Utils.getStrideRadius(rc.getType());


        float ax = 0;
        float ay = 0;

        for (MapLocation l : lookAhead) {
            ax += l.x;
            ay += l.y;
        }

        MapLocation average = new MapLocation(ax, ay);
        Direction targetDir = rc.getLocation().directionTo(average).opposite();
        if (rc.canMove(targetDir)) {
            rc.move(targetDir);
            return 0;
        } else {
            return -1;
        }

    }

    static boolean isImminentThreat(BulletInfo bullet, float angleThreshold, float distanceThreshold) {
        Direction bulletToTarget = bullet.getLocation().directionTo(rc.getLocation());
        Direction bulletDir = bullet.getDir();

        // if angle and distance are within the threshold
        return bulletDir.degreesBetween(bulletToTarget) < angleThreshold
                && bullet.getLocation().distanceTo(rc.getLocation()) < distanceThreshold;
    }
}
