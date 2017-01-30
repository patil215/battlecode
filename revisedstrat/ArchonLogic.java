package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class ArchonLogic extends RobotLogic {

	private final float MIN_FREE_SPACE_REQUIREMENT;
	private boolean movedTowardsArchon;

	public ArchonLogic(RobotController rc) {
		super(rc);
		MIN_FREE_SPACE_REQUIREMENT = type.bodyRadius + 1;
		movedTowardsArchon = false;
	}

	@Override
	public void run() {

		try {

			BroadcastManager.initializeLocationPointerIndexValues(rc);

			// Spawn a gardener on the first move
			if (shouldSpawnInitialGardener()) {
				spawnGardener();
			}

			while (true) {
				// Try to spawn gardener if should spawn gardener
				if (shouldSpawnGardener()) {
					spawnGardener();
				}

				// Move
				moveToGoodLocation();

				broadcastForHelpIfNeeded();
				detectTreesAndAskLumberjacksForHelp();
				endTurn();
			}

		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	private void detectTreesAndAskLumberjacksForHelp() throws GameActionException {
		TreeInfo[] treesInWay = rc.senseNearbyTrees(MIN_FREE_SPACE_REQUIREMENT, Team.NEUTRAL);
		if (treesInWay.length > 0) {
			TreeInfo closestTree = (TreeInfo) getClosestBody(treesInWay);
			revisedstrat.BroadcastManager.saveLocation(rc, closestTree.location,
					revisedstrat.BroadcastManager.LocationInfoType.LUMBERJACK_GET_HELP);
		}
	}

	private final boolean SPAWN_AWAY_FROM_ENEMY_ARCHON_LOC = false;

	private void spawnGardener() throws GameActionException {
		if (SPAWN_AWAY_FROM_ENEMY_ARCHON_LOC) {
			Direction enemyOppositeLocation = rc.getLocation().directionTo(getRandomEnemyInitialArchonLocation())
					.opposite();
			// Keep rotating until there is space to spawn a gardener
			for (int i = 0; i < 50; i++) {
				int rotationAmount = 11 * (i / 2);
				if (i % 2 == 0) {
					rotationAmount = rotationAmount * -1;
				}
				Direction directionAttempt = enemyOppositeLocation.rotateRightDegrees(rotationAmount);
				if (rc.canHireGardener(directionAttempt)) {
					rc.hireGardener(directionAttempt);
					break;
				}
			}
		} else {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
			if (enemyRobots.length > 0) {
				RobotInfo enemyRobot = enemyRobots[0];
				// Spawn away from enemy
				Direction oppositeDirection = rc.getLocation().directionTo(enemyRobot.location).opposite();
				if (rc.canHireGardener(oppositeDirection)) {
					rc.hireGardener(oppositeDirection);
				}
			}
			for (int i = 0; i < 50; i++) {
				Direction directionAttempt = Utils.randomDirection();
				if (rc.canHireGardener(directionAttempt)) {
					rc.hireGardener(directionAttempt);
					break;
				}
			}
		}
	}

	// Only spawn initial gardener if no other archons have spawned a unit.
	private boolean shouldSpawnInitialGardener() {
		return allyArchonLocations.length >= rc.getRobotCount();
	}

	private final int ROUNDS_TO_WAIT_BEFORE_SPAWNING_MORE_THAN_INITIAL_GARDENER = 125;
	private final double GARDENER_SPAWN_CHANCE_EARLY = 0.3;
	private final double GARDENER_SPAWN_CHANCE_LATE = 0.15;
	private final int ROUNDS_WHEN_LATE = 500;

	private boolean shouldSpawnGardener() {
		if (rc.getRoundNum() > 100 && rc.getRobotCount() - allyArchonLocations.length == 0) {
			return true;
		}

		double spawnChance = (rc.getRoundNum() > ROUNDS_WHEN_LATE) ? GARDENER_SPAWN_CHANCE_LATE
				: GARDENER_SPAWN_CHANCE_EARLY;

		if (rc.getRoundNum() > ROUNDS_TO_WAIT_BEFORE_SPAWNING_MORE_THAN_INITIAL_GARDENER) {
			if (Math.random() < spawnChance && (!inDanger() || rc.getTeamBullets() >= 200)) {
				return true;
			}
		}
		return false;
	}

	private void moveToGoodLocation() throws GameActionException {

		/*
		 * TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL); BodyInfo
		 * closestTree = getClosestBody(trees); if(closestTree != null) {
		 * TreeInfo tree = (TreeInfo) closestTree; if (tree.containedBullets > 0
		 * && rc.getLocation().distanceTo(tree.location) < type.sensorRadius *
		 * 0.75) { Direction direction = getDirectionTowards(tree.location);
		 * MapLocation newLoc = rc.getLocation().add(direction,
		 * type.strideRadius); if (isValidNextArchonLocation(newLoc)) { return
		 * newLoc; } } }
		 */

		if (!movedTowardsArchon) {
			if(rc.getLocation()
				.distanceTo(enemyArchonLocations[0]) > (startLocation.distanceTo(enemyArchonLocations[0]) * 0.9)) {
				Direction toEnemy = rc.getLocation().directionTo(enemyArchonLocations[0]);
				MapLocation proposed = rc.getLocation().add(toEnemy, type.strideRadius - 0.01f);
				if (isValidNextArchonLocation(proposed)) {
					move(proposed);
				}
			} else {
				movedTowardsArchon = true;
			}
		}

		/*
		 * RobotInfo[] otherRobots = rc.senseNearbyRobots(-1, allyTeam); int
		 * count = 0; for(RobotInfo robot : otherRobots) {
		 * if(robot.getType().equals(RobotType.GARDENER)) { count++; } }
		 * RobotInfo[] gardeners = new RobotInfo[count]; int index = 0;
		 * for(RobotInfo robot : otherRobots) {
		 * if(robot.getType().equals(RobotType.GARDENER)) { gardeners[index] =
		 * robot; index++; } } Direction dirAway = getDirectionAway(gardeners);
		 * if (dirAway != null) { for (int i = 5; i >= 1; i--) { MapLocation
		 * attemptedNewLocation = rc.getLocation().add(dirAway, (float) (i *
		 * 0.2)); if (isValidNextArchonLocation(attemptedNewLocation)) {
		 * Direction moveDir = getDirectionTowards(attemptedNewLocation);
		 * if(moveDir != null) { move(moveDir); return; } } } }
		 */

		for (int i = 0; i < 50; i++) {
			Direction randomDir = Utils.randomDirection();
			MapLocation attemptedNewLocation = rc.getLocation().add(randomDir, type.strideRadius);
			if (isValidNextArchonLocation(attemptedNewLocation)) {
				move(randomDir);
			}
		}
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
		MapLocation avgEnemyLoc = Utils.getAvgArchonLocations(rc, enemyTeam);
		MapLocation avgAllyLoc = Utils.getAvgArchonLocations(rc, allyTeam);
		if (rc.getLocation().distanceTo(avgAllyLoc) <= ((rc.getLocation().distanceTo(avgEnemyLoc) * 0.7) + 0.01)) {
			return location.distanceTo(avgAllyLoc) <= ((location.distanceTo(avgEnemyLoc) * 0.7) + 0.01);
		}
		return true;
	}

	private void broadcastForHelpIfNeeded() throws GameActionException {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, enemyTeam);

		if (foes.length > 0) {
			BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.ARCHON_HELP);
		}
	}

}
