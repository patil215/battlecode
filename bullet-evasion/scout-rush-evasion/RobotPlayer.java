package scoutrushevasion;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class RobotPlayer {
    static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions
        // from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each
        // RobotType.
        // You can add the missing ones or rewrite this into your own control
        // structure.
        switch (rc.getType()) {
        case ARCHON:
            runArchon();
            break;
        case GARDENER:
            runGardener();
            break;
        case SCOUT:
            runScout();
            break;
        }
    }

    private static float danger(BulletInfo bullet) {
        Direction toMe = bullet.location.directionTo(rc.getLocation());
        float angle = toMe.degreesBetween(bullet.getDir());
        float measure = bullet.getDamage() * angle * bullet.location.distanceTo(rc.getLocation());

        return measure;
    }

    private static BulletInfo findMostDangerousBullet() {
        BulletInfo[] bullets = rc.senseNearbyBullets();
        if (bullets.length > 0) {
            // Move perpendicular to the most dangerous bullet
            float maxDanger = danger(bullets[0]);
            BulletInfo mostDangerous = bullets[0];
            for (BulletInfo bullet : bullets) {
                float danger = danger(bullet);
                if (danger > maxDanger) {
                    mostDangerous = bullet;
                    maxDanger = danger;
                }
            }
            return mostDangerous;
        }

        return null;
    }

    private static void runScout() throws GameActionException {
        Direction move = randomDirection();
        Team enemy = rc.getTeam().opponent();
        try {
            while (true) {

                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                if (robots.length != 0) {
                    if (rc.canFireSingleShot()) {
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                BulletInfo[] bullets = rc.senseNearbyBullets();
                if (bullets.length > 0) {
                    // Move perpendicular to the most dangerous bullet
                    float maxDanger = danger(bullets[0]);
                    BulletInfo mostDangerous = bullets[0];
                    for (BulletInfo bullet : bullets) {
                        float danger = danger(bullet);
                        if (danger > maxDanger) {
                            mostDangerous = bullet;
                            maxDanger = danger;
                        }
                    }

                    Direction bulletDir = mostDangerous.getDir();
                    move = new Direction(-bulletDir.getDeltaY(5), bulletDir.getDeltaX(5));
                }


                if (rc.canMove(move)) {
                    rc.move(move);
                } else {
                    rc.move(randomDirection());
                }
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private static void runGardener() throws GameActionException {
        Direction move = randomDirection();
        int treeCount = 2;
        int roundsToSettle = (int) (Math.random() * 20);
        try {
            while (true) {
                if (roundsToSettle > 0 && rc.canMove(move)) {
                    rc.move(move);
                    roundsToSettle--;
                } else if (roundsToSettle > 0) {
                    move = randomDirection();
                } else {
                    if (treeCount > 0) {
                        if (rc.canPlantTree(move)) {
                            rc.plantTree(move);
                            treeCount--;
                        } else {
                            move = move.rotateLeftDegrees((float) (Math.PI / 5));
                        }
                    } else {
                        if (rc.canBuildRobot(RobotType.SCOUT, move)) {
                            rc.buildRobot(RobotType.SCOUT, move);
                        } else {
                            move = move.rotateLeftDegrees((float) (Math.PI / 5));
                        }
                    }

                    TreeInfo[] trees = rc.senseNearbyTrees();

                    if (trees.length > 0) {
                        int minHealthID = -1;
                        float minHealth = Float.MAX_VALUE;
                        for (int count = 0; count < trees.length; count++) {
                            if (rc.canWater(trees[count].ID) && trees[count].health < minHealth) {
                                minHealth = trees[count].health;
                                minHealthID = trees[count].ID;
                            }
                        }
                        if (rc.canWater(minHealthID)) {
                            rc.water(minHealthID);
                        }
                    }
                }

                if (rc.getTeamBullets() >= 10000) {
                    rc.donate(10000);
                }
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private static void runArchon() throws GameActionException {
        int numGardeners = 7;
        Direction move = randomDirection();
        while (true) {
            if (rc.canMove(move)) {
                rc.move(move);
            } else {
                move = randomDirection();
            }
            if (rc.canHireGardener(move.opposite()) && numGardeners > 0) {
                rc.hireGardener(move.opposite());
                numGardeners--;
            }
            if (rc.getTeamBullets() >= 10000) {
                rc.donate(10000);
            }
            Clock.yield();
        }

    }

    static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }
}
