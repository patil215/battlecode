package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class ArchonLogic extends RobotLogic {

    public ArchonLogic(RobotController rc) {
        super(rc);
    }

    @Override
    public void run() {

        // TODO move try/catch to while loop
        try {

        	BroadcastManager.tryAndSetUpLocationPointerIndexValues(rc);

        	BroadcastManager.saveLocation(rc, rc.getLocation(),LocationInfoType.ARCHON_HELP);

            // Spawn a gardener on the first move
            if (shouldSpawnInitialGardener()) {
                spawnGardener();
            }

            while (true) {
                // Try to spawn gardener if should spawn gardener
                if (shouldSpawnGardener()) {
                    spawnGardener();
                }
                // Try to move in random direction
                MapLocation moveLocation = pickNextLocation();
                if(moveLocation != null) {
                    move(moveLocation);
                }

                RobotInfo[] foes = rc.senseNearbyRobots(-1,getEnemyTeam());

                if(foes.length>0){
                	BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.ARCHON_HELP);
                }

                endTurn();
            }

        } catch (GameActionException e) {
            e.printStackTrace();
        }
    }


    // TODO: Instead of rotating in a singular direction, rotate left/right back and forth to spawn away from enemy
    private void spawnGardener() throws GameActionException {
        Direction directionAttempt = rc.getLocation().directionTo(getRandomEnemyInitialArchonLocation()).opposite();
        // Keep rotating until there is space to spawn a gardener
        for(int i = 0; i < 100; i++) {
            if (rc.canHireGardener(directionAttempt)) {
                rc.hireGardener(directionAttempt);
                break;
            } else {
                directionAttempt = directionAttempt.rotateRightDegrees(11); // Relatively prime to 360 to avoid infinite loop (hopefully)
            }
        }
    }

    /*
    An archon should only spawn the initial gardener if no other archons have spawned a unit.
     */
    private boolean shouldSpawnInitialGardener() {
        if (rc.getInitialArchonLocations(rc.getTeam()).length >= rc.getRobotCount()) {
            return true;
        }
        return false;
    }

    // TODO: Make this mathematically based on the number of bullets, robots (bullet demand), and trees
    private boolean shouldSpawnGardener() {
        if (rc.getRoundNum() > 150) {
            if (Math.random()>.75) {
                return true;
            }
        }
        return false;
    }

    // TODO move accounting for bullet evasion, use moveTowards() logic to "wiggle" out of traps
    private MapLocation pickNextLocation() throws GameActionException {
        for(int i = 0; i < 100; i++) {
            Direction randomDir = Utils.randomDirection();
            MapLocation attemptedNewLocation = rc.getLocation().add(randomDir, rc.getType().strideRadius);
            if (isValidNextArchonLocation(attemptedNewLocation)) {
                System.out.println("Found location to move to");
                return attemptedNewLocation;
            }
        }
        return null;
    }

    private boolean isValidNextArchonLocation(MapLocation location) throws GameActionException {
        if (!rc.canSenseLocation(location)) {
            return false;
        }
        if (!rc.onTheMap(location)) {
            return false;
        }
        if (!rc.canMove(location)) {
            return false;
        }
        MapLocation avgEnemyLoc = Utils.getAvgArchonLocations(rc, getEnemyTeam());
        MapLocation avgAllyLoc = Utils.getAvgArchonLocations(rc, rc.getTeam());
        return location.distanceTo(avgAllyLoc) <= location.distanceTo(avgEnemyLoc) + 0.01;
    }

}
