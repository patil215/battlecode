package scoutjackrushevasion;

import battlecode.common.*;

import java.util.ArrayList;

/**
 * Created by dhruv on 1/14/17.
 */

class AggregateEvasion {

    public static Direction move(RobotController rc) throws GameActionException {
        float angleThreshold = 40;
        float distanceThreshold = 5;
//        BulletInfo[] dangerousBullets = (BulletInfo[]) Arrays.stream(rc.senseNearbyBullets())
//                 Consider only those bullets which pose an immediate threat
//                .filter(b -> isImminentThreat(b, angleThreshold, distanceThreshold, rc))
//                .toArray();

        ArrayList<BulletInfo> dangerousBulletList = new ArrayList<>();
        for (BulletInfo bullet : rc.senseNearbyBullets()) {
            if (isImminentThreat(bullet, angleThreshold, distanceThreshold, rc)) {
                dangerousBulletList.add(bullet);
            }
        }

        BulletInfo[] dangerousBullets = dangerousBulletList.toArray(new BulletInfo[dangerousBulletList.size()]);

        if (dangerousBullets.length == 0) return null;

        MapLocation[] lookAhead = new MapLocation[dangerousBullets.length];
        for (int i = 0; i < dangerousBullets.length; i++) {
            BulletInfo bullet = dangerousBullets[i];
//            lookAhead[i] = bullet.getLocation().add(bullet.getDir(), bullet.getSpeed());
            lookAhead[i] = bullet.getLocation();
        }

        Direction d = new Direction(0, 0);


        float ax = 0;
        float ay = 0;

        for (MapLocation l : lookAhead) {
            ax += l.x;
            ay += l.y;
        }

        MapLocation average = new MapLocation(ax, ay);
        Direction targetDir = rc.getLocation().directionTo(average).opposite();

        return targetDir;
    }

    static boolean isImminentThreat(BulletInfo bullet, float angleThreshold, float distanceThreshold, RobotController rc) {
        Direction bulletToTarget = bullet.getLocation().directionTo(rc.getLocation());
        Direction bulletDir = bullet.getDir();

        // if angle and distance are within the threshold
        return Math.abs(bulletDir.degreesBetween(bulletToTarget)) < angleThreshold
                && bullet.getLocation().distanceTo(rc.getLocation()) < distanceThreshold;
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
