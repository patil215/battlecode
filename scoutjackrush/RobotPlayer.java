package scoutjackrush;

import java.util.Arrays;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
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
		case LUMBERJACK:
			runJack();
		}
	}

	private static void runJack() throws GameActionException {
		Direction move = randomDirection();
		while (true) {
			RobotInfo[] foes = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
			if (foes.length > 0) {
				RobotInfo target = findNearestEnemyChar(foes);
				Direction targetDirection = rc.getLocation().directionTo(target.location);
				if (rc.canMove(targetDirection)) {
					rc.move(targetDirection);
					if (rc.canStrike()) {
						rc.strike();
					}
				} else {
					chopIfTreeInTheWay(targetDirection);
				}
			} else {
				if (rc.readBroadcast(1) != -1) {
					move = RobotPlayer.moveTowards(new MapLocation(rc.readBroadcast(1), rc.readBroadcast(2)));
					if (rc.canMove(move)) {
						rc.move(move);
					} else {
						chopIfTreeInTheWay(move);
					}
				} else {
					move = RobotPlayer.moveWithRandomBounce(move);
				}
			}
			Clock.yield();
		}
	}

	private static void chopIfTreeInTheWay(Direction targetDirection) throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees();
		float lowestHealth = Float.MAX_VALUE;
		int lowestHealthID = -1;
		boolean foundTreeToChop = false;
		for (int index = 0; index < trees.length && !foundTreeToChop; index++) {
			// TODO: Make sure that this is a tree in the way
			if (trees[index].team != rc.getTeam() && rc.canChop(trees[index].ID)
					&& rc.getLocation().directionTo(trees[index].location).degreesBetween(targetDirection) < 90
					&& trees[index].health < lowestHealth) {
				lowestHealth = trees[index].health;
				lowestHealthID = trees[index].ID;
			}
		}
		if (lowestHealthID != -1) {
			rc.chop(lowestHealthID);
		}
	}

	private static void runScout() throws GameActionException {
		try {
			Direction move = randomDirection();
			while (true) {
				RobotInfo[] foes = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
				// There are opponents
				if (foes.length > 0) {
					RobotInfo target = RobotPlayer.getHighestPriorityTarget(foes);
					if (target != null && rc.canFireSingleShot()) {
						rc.fireSingleShot(rc.getLocation().directionTo(target.location));
						rc.broadcast(1, (int) target.location.x);
						rc.broadcast(2, (int) target.location.y);
					}
					RobotInfo largestThreat = RobotPlayer.findNearestEnemy(foes);
					if (largestThreat != null) {
						Direction threatDirection = rc.getLocation().directionTo(largestThreat.location);
						// Move away from the nearest enemy if it is too close.
						if (rc.getLocation().distanceTo(largestThreat.location) < 7) {
							if (rc.canMove(threatDirection.opposite())) {
								rc.move(threatDirection.opposite());
							}
						}
						// Move towards the nearest enemy if far away.
						else {
							if (rc.canMove(threatDirection)) {
								rc.move(threatDirection);
							}
						}
					} else {
						RobotInfo closestEconTarget = RobotPlayer.findNearestEnemyChar(foes);
						if (closestEconTarget != null) {
							if (RobotPlayer.fireBulletImpact(rc.getLocation().directionTo(closestEconTarget.location),
									rc.getLocation(), (float) 1.5) <= 0) {
								Direction toMove = RobotPlayer.moveTowards(closestEconTarget.location);
								if (rc.canMove(toMove)) {
									rc.move(toMove);
								}
							}
						}
					}
				} else {
					if (rc.readBroadcast(1) != -1) {
						move = RobotPlayer.moveTowards(new MapLocation(rc.readBroadcast(1), rc.readBroadcast(2)));
						if (rc.canMove(move)) {
							rc.move(move);
						}
					} else {
						move = RobotPlayer.moveWithRandomBounce(move);
					}
				}
				if (!rc.hasAttacked() && !rc.hasMoved()) {
					move = RobotPlayer.moveWithRandomBounce(move);
				}
				Clock.yield();
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	private static void runGardener() throws GameActionException {
		try {
			boolean isDead = false;
			Direction move = randomDirection();
			int turnCount = 20;
			// Right now, this is a pure econ strategy. Remove the || true and
			// replace with something like Math.random > .5 to return to a scout
			// strategy.
			if ((rc.getTreeCount() < 6 || Math.random() > .2)) {
				while (true) {
					if (!isDead && rc.getHealth() < 5) {
						isDead = true;
						rc.broadcast(0, rc.readBroadcast(0) - 1);
					}
					if (move != null) {
						System.out.println(rc.getLocation());
						if (turnCount == 0
								|| rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(0, (float) .01), 3)) {
							move = null;
						} else {
							turnCount--;
							move = moveWithRandomBounce(move);
						}
					} else {
						Direction plant = Direction.getNorth();
						for (int deg = 5; deg < 360 && !rc.canPlantTree(plant); deg += 5) {
							plant = Direction.getNorth().rotateLeftDegrees(deg);
						}
						if (rc.canPlantTree(plant)) {
							rc.plantTree(plant);
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
					if (rc.getTeamBullets() >= 10000) {
						rc.donate(10000);
					}
					Clock.yield();
				}
			} else {
				while (true) {
					if (!isDead && rc.getHealth() < 5) {
						isDead = true;
						rc.broadcast(0, rc.readBroadcast(0) - 1);
					}
					move = RobotPlayer.moveWithRandomBounce(move);
					// Change to something possible to add lumberjacks to the
					// strategy.
					if (Math.random() < 1 && rc.getTeamBullets() >= 80) {
						if (rc.canBuildRobot(RobotType.SCOUT, move.opposite())) {
							rc.buildRobot(RobotType.SCOUT, move.opposite());
						}
					} else {
						if (rc.canBuildRobot(RobotType.LUMBERJACK, move.opposite())) {
							rc.buildRobot(RobotType.LUMBERJACK, move.opposite());
						}
					}
					Clock.yield();
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

	}

	private static void runArchon() throws GameActionException {
		rc.broadcast(1, -1);
		rc.broadcast(2, -1);
		Direction move = randomDirection();
		while (true) {
			if (Math.round(rc.getTeamBullets()) % 20 == 0) {
				rc.broadcast(1, -1);
				rc.broadcast(2, -1);
			}
			int numGardeners = rc.readBroadcast(0);
			if ((rc.readBroadcast(0) < 12 || Math.random() > .1) && rc.canHireGardener(move.opposite())) {
				rc.hireGardener(move.opposite());
				rc.broadcast(0, numGardeners + 1);
			}
			move = moveWithRandomBounce(move);
			if (rc.getTeamBullets() >= 10000) {
				rc.donate(10000);
			}
			Clock.yield();
		}
	}

	static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	private static Direction moveWithRandomBounce(Direction move) throws GameActionException {
		if (rc.canMove(move)) {
			rc.move(move);
		} else {
			for (int count = 0; count < 20 && !rc.canMove(move); count++) {
				move = randomDirection();
			}
			if (rc.canMove(move)) {
				rc.move(move);
			}
		}
		return move;
	}

	static boolean willBeHitByBullet(BulletInfo info, MapLocation playerLocation) throws GameActionException {
		MapLocation location = info.location;
		while (rc.canSenseLocation(location)) {
			if (rc.isLocationOccupiedByTree(location)) {
				return false;
			} else if (rc.isLocationOccupiedByRobot(location)) {
				if (rc.senseRobotAtLocation(location).ID == rc.getID()) {
					return true;
				}
				return false;
			}
			location = location.add(info.dir, info.speed);
		}
		return false;
	}

	private static Direction moveTowards(Direction move) {
		if (rc.canMove(move)) {
			return move;
		} else {
			boolean foundMoveAngle = false;
			for (float deltaAngle = 0; deltaAngle < 180 && !foundMoveAngle; deltaAngle += 45) {
				if (rc.canMove(move.rotateRightDegrees(deltaAngle))) {
					move = move.rotateRightDegrees(deltaAngle);
					foundMoveAngle = true;
				} else if (rc.canMove(move.rotateLeftDegrees(deltaAngle))) {
					move = move.rotateLeftDegrees(deltaAngle);
					foundMoveAngle = true;
				}
			}
		}
		return move;
	}

	private static RobotInfo findNearestEnemy(RobotInfo[] closeRobots) {
		float nearestDistance = Float.MAX_VALUE;
		RobotInfo nearestEnemy = null;
		for (int index = 0; index < closeRobots.length; index++) {
			float newDistance = rc.getLocation().distanceTo(closeRobots[index].location);
			if (newDistance < nearestDistance && closeRobots[index].type != RobotType.ARCHON
					&& closeRobots[index].type != RobotType.GARDENER) {
				nearestDistance = newDistance;
				nearestEnemy = closeRobots[index];
			}
		}
		return nearestEnemy;
	}

	private static RobotInfo findNearestEnemyChar(RobotInfo[] closeRobots) {
		float nearestDistance = Float.MAX_VALUE;
		RobotInfo nearestEnemy = null;
		for (int index = 0; index < closeRobots.length; index++) {
			float newDistance = rc.getLocation().distanceTo(closeRobots[index].location);
			if (newDistance < nearestDistance) {
				nearestDistance = newDistance;
				nearestEnemy = closeRobots[index];
			}
		}
		return nearestEnemy;
	}

	private static Direction moveTowards(MapLocation destination) throws GameActionException {
		Direction move;
		move = rc.getLocation().directionTo(destination);
		return moveTowards(move);
	}

	private static int fireBulletImpact(Direction dir, MapLocation location, float speed) throws GameActionException {
		location = location.add(dir, rc.senseRobot(rc.getID()).getRadius() + GameConstants.BULLET_SPAWN_OFFSET);
		while (rc.canSenseLocation(location)) {
			if (rc.isLocationOccupied(location)) {
				if (rc.isLocationOccupiedByTree(location)) {
					if (rc.senseTreeAtLocation(location).team == rc.getTeam().opponent()) {
						return 0;
					} else {
						return 0;
					}
				} else if (rc.isLocationOccupiedByRobot(location)) {
					if (rc.senseRobotAtLocation(location).team == rc.getTeam().opponent()) {
						return 1;
					} else {
						return -1;
					}
				}
			}
			location = location.add(dir, speed);
		}
		return 0;
	}

	static RobotInfo getHighestPriorityTarget(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0) {
			return null;
		}

		int maxIndex = -1;
		double maxPriority = -1;

		for (int index = 0; index < enemies.length; index++) {
			double priority = enemies[index].getType().attackPower / enemies[index].health;

			if ((priority > maxPriority || (maxPriority == 0 && enemies[index].health < enemies[maxIndex].health))
					&& RobotPlayer.fireBulletImpact(rc.getLocation().directionTo(enemies[index].location),
							rc.getLocation(), (float) 1.5) > 0) {
				maxIndex = index;
				maxPriority = priority;
			}
		}

		if (maxIndex >= 0) {
			return enemies[maxIndex];
		}
		return null;
	}
}