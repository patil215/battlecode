package revisedevasionstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

    private Direction moveDir;
    private int numRoundsSettling;
    private boolean settled;

    public GardenerLogic(RobotController rc) {
        super(rc);
        numRoundsSettling = 0;
        moveDir = Utils.randomDirection();
    }

    @Override
    public void run() {

        try {

            while(true) {
                if(!settled && numRoundsSettling < 20) {
                    moveTowardsGoodSpot();
                    numRoundsSettling++;
                }

                if(settled) {
                    // Look for a place to spawn a tree
                    int oppositeDirDegrees = (int) rc.getLocation().directionTo(rc.getInitialArchonLocations(getEnemyTeam())[0]).opposite().getAngleDegrees();
                    Direction spawnDir = Direction.getWest().rotateRightDegrees(oppositeDirDegrees).rotateRightDegrees(60);

                    int numRotations = 0;
                    while(!rc.canPlantTree(spawnDir) && numRotations < 5) {
                        spawnDir = spawnDir.rotateRightDegrees(60);
                        numRotations++;
                    }

                    // If can spawn tree, spawn tree
                    if(numRotations < 5 && rc.canPlantTree(spawnDir)) {
                        rc.plantTree(spawnDir);
                    } else {
                        spawnUnit(spawnDir);
                    }
                }

                waterLowestHealthTree();

                Clock.yield();
            }

        } catch (GameActionException e) {
            e.printStackTrace();
        }

    }

    private void waterLowestHealthTree() throws GameActionException {
        TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());
        if (trees.length > 0) {
            int minHealthID = -1;
            float minHealth = Float.MAX_VALUE;
            for (int index = 0; index < trees.length; index++) {
                if (rc.canWater(trees[index].ID) && trees[index].health < minHealth) {
                    minHealth = trees[index].health;
                    minHealthID = trees[index].ID;
                }
            }
            if (rc.canWater(minHealthID)) {
                rc.water(minHealthID);
            }
        }
    }

    private void spawnUnit(Direction direction) throws GameActionException {
        if(rc.canBuildRobot(RobotType.LUMBERJACK, direction)) {
            if (Math.random() < .5) {
                rc.buildRobot(RobotType.SCOUT, direction);
            } else {
                rc.buildRobot(RobotType.LUMBERJACK, direction);
            }
        }
    }

    private void moveTowardsGoodSpot() throws GameActionException {
        // Try to find a free space to settle until 20 turns have elapsed
        if (!isGoodLocation()) {
            moveDir = moveWithRandomBounce(moveDir);
        } else {
            settled = true;
        }
    }

    private boolean isGoodLocation() {
        try {
            // Check for free space of 3 radius - gives space to spawn trees
            return !rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(0, (float) .01), 3)
            && rc.onTheMap(rc.getLocation().add(0, (float) .01), 3);
        } catch (GameActionException e) {
            return false;
        }
    }
}
