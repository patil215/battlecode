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
				MapLocation moveLocation = pickNextLocation();
				if (moveLocation != null) {
					move(moveLocation);
				}

				broadcastForHelpIfNeeded();

				endTurn();
			}

		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	private final boolean SPAWN_AWAY_FROM_ENEMY_LOC = false;

	private void spawnGardener() throws GameActionException {
		if (SPAWN_AWAY_FROM_ENEMY_LOC) {
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
		return rc.getInitialArchonLocations(rc.getTeam()).length >= rc.getRobotCount();
	}

	private final int ROUNDS_TO_WAIT_BEFORE_SPAWNING_MORE_THAN_INITIAL_GARDENER = 125;
	private final double GARDENER_SPAWN_CHANCE = 0.75;

	private boolean shouldSpawnGardener() {
		if (rc.getRoundNum() > ROUNDS_TO_WAIT_BEFORE_SPAWNING_MORE_THAN_INITIAL_GARDENER) {
			if (Math.random() > GARDENER_SPAWN_CHANCE) {
				return true;
			}
		}
		return false;
	}

	private MapLocation pickNextLocation() throws GameActionException {

		// Try to move away from other units first
		Direction awayDir = getDirectionAway(rc.senseNearbyRobots());
		if (awayDir != null) {
			for (int i = 5; i >= 1; i--) {
				MapLocation attemptedNewLocation = rc.getLocation().add(awayDir, (float) (i * 0.2));
				if (isValidNextArchonLocation(attemptedNewLocation)) {
					return attemptedNewLocation;
				}
			}
		}

		for (int i = 0; i < 50; i++) {
			Direction randomDir = Utils.randomDirection();
			MapLocation attemptedNewLocation = rc.getLocation().add(randomDir, rc.getType().strideRadius);
			if (isValidNextArchonLocation(attemptedNewLocation)) {
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
		return location.distanceTo(avgAllyLoc) <= ((location.distanceTo(avgEnemyLoc) * 0.7) + 0.01);
	}

	private void broadcastForHelpIfNeeded() throws GameActionException {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());

		if (foes.length > 0) {
			BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.ARCHON_HELP);
		}
	}

}
