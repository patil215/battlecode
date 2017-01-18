package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.UnitCountInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

	private Direction moveDir;
	private final int NUM_ROUNDS_TO_SETTLE = 30;
	private final double TANK_SPAWNER_CHANCE = 0.5;

	private final Direction ENEMY_BASE_OPPOSITE_DIRECTION;
	private final boolean SHOULD_SPAWN_TANKS;
	private final int MIN_FREE_SPACE_REQUIREMENT = 4;

	public GardenerLogic(RobotController rc) {
		super(rc);
		ENEMY_BASE_OPPOSITE_DIRECTION = rc.getLocation()
				.directionTo(Utils.getAvgArchonLocations(rc, getEnemyTeam())).opposite();
		SHOULD_SPAWN_TANKS = Math.random() < TANK_SPAWNER_CHANCE
				&& !(rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length);
		moveDir = Utils.randomDirection();
	}

	@Override
	public void run() {

		try {

			buildInitialRoundsUnits();

			int numRoundsSettling = 0;
			boolean settled = false;

			while (true) {

				if(!settled) {
					if (numRoundsSettling > NUM_ROUNDS_TO_SETTLE) {
						settled = true;
					} else {
						numRoundsSettling++;
						settled = moveTowardsGoodSpot();
					}
				} else {
					createTreeRingAndSpawnUnits();
				}

				detectEnemiesAndSendHelpBroadcast();

				waterLowestHealthTree();
				tryAndShakeATree();
				econWinIfPossible();
				drawBullshitLine();

				Clock.yield();
			}

		} catch (GameActionException e) {
			e.printStackTrace();
		}

	}

	private void createTreeRingAndSpawnUnits() throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(ENEMY_BASE_OPPOSITE_DIRECTION), 255, 255, 255);
		if (rc.getBuildCooldownTurns() == 0) {
			System.out.println("Should be able to spawn a unit");
			Direction startAngle;
			if (SHOULD_SPAWN_TANKS) {
				startAngle = ENEMY_BASE_OPPOSITE_DIRECTION.rotateLeftDegrees(90);
			} else {
				startAngle = ENEMY_BASE_OPPOSITE_DIRECTION.rotateLeftDegrees(60);
			}

			System.out.println("Starting to try angles");
			while (!rc.canPlantTree(startAngle)
					&& Math.abs(ENEMY_BASE_OPPOSITE_DIRECTION.degreesBetween(startAngle)) >= 50) {
				startAngle = startAngle.rotateLeftDegrees(10);
				System.out.println("Trying an angle of " + startAngle);
			}
			System.out.println("Done trying angles");
			if (rc.canPlantTree(startAngle)) {
				System.out.println("Trying to plant a tree");
				rc.plantTree(startAngle);
			} else {
				System.out.println("Trying to spawn unit");
				spawnUnit(ENEMY_BASE_OPPOSITE_DIRECTION);
			}
		}
	}

	private void buildInitialRoundsUnits() throws GameActionException {
		if (rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length) {
			tryAndBuildUnit(RobotType.SCOUT);
			while (rc.getBuildCooldownTurns() != 0) {
				Clock.yield();
			}
			tryAndBuildUnit(RobotType.SOLDIER);
		}
	}

	private void tryAndBuildUnit(RobotType toBuild) throws GameActionException {
		Direction test = Direction.getNorth();
		for (int deltaDegree = 0; deltaDegree < 360; deltaDegree++) {
			if (rc.canBuildRobot(toBuild, test.rotateLeftDegrees(deltaDegree))) {
				rc.buildRobot(toBuild, test.rotateLeftDegrees(deltaDegree));
				return;
			}
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

	private RobotType determineUnitToSpawn(Direction intendedDirection) throws GameActionException {
		if (BroadcastManager.getUnitCount(rc, UnitCountInfoType.ALLY_SCOUT) < 3) {
			return RobotType.SCOUT;
		} else if (rc.canBuildRobot(RobotType.TANK, intendedDirection)) {
			return RobotType.TANK;
		} else if (Math.random() > .8) {
			return RobotType.LUMBERJACK;
		} else {
			return RobotType.SOLDIER;
		}
	}

	private void spawnUnit(Direction direction) throws GameActionException {
		if (rc.canBuildRobot(RobotType.LUMBERJACK, direction) && rc.getBuildCooldownTurns() == 0
				&& rc.getTeamBullets() >= 100) {
			System.out.println("Can build");
			System.out.println(
					"We have around " + BroadcastManager.getUnitCount(rc, UnitCountInfoType.ALLY_SCOUT) + "Scouts");
			RobotType typeToBuild = determineUnitToSpawn(direction);
			rc.buildRobot(typeToBuild, direction);
		} else {
			System.out.println("Can't build");
		}
	}

	/*
	 * Attempts to move to a good location. Returns true if a good location was
	 * found after the move.
	 */
	private boolean moveTowardsGoodSpot() throws GameActionException {
		// Try to find a free space to settle until 20 turns have elapsed
		if (!isGoodLocation()) {
			moveDir = moveWithRandomBounce(moveDir);
			return false;
		} else {
			return true;
		}
	}

	private boolean isGoodLocation() {
		try {
			// Check for free space of 3 radius - gives space to spawn trees
			return !rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(0, (float) .01), MIN_FREE_SPACE_REQUIREMENT)
					&& rc.onTheMap(rc.getLocation().add(0, (float) .01), MIN_FREE_SPACE_REQUIREMENT);
		} catch (GameActionException e) {
			return false;
		}
	}
}
