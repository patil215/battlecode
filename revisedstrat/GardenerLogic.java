package revisedstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

	private Direction moveDir;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER = 50;
	private final boolean UNIT_SPAWNER_ELIGIBLE;
	private final boolean DEGENERATE_ELIGIBLE;
	private final int NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE = 300;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE = 25;
	private final int UNIT_SPAWNER_THRESHOLD = 200;
	private final boolean SHOULD_SPAWN_TANKS;
	private final float MIN_FREE_SPACE_REQUIREMENT = 5;

	public GardenerLogic(RobotController rc) {
		super(rc);
		double TANK_SPAWNER_CHANCE = rc.getRoundNum() * 2.0 / rc.getRoundLimit();
		SHOULD_SPAWN_TANKS = Math.random() < TANK_SPAWNER_CHANCE
				&& !(rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length);
		moveDir = Utils.diagonalDirection();
		UNIT_SPAWNER_ELIGIBLE = rc.getRoundNum() > UNIT_SPAWNER_THRESHOLD;
		DEGENERATE_ELIGIBLE = rc.getRoundNum() < NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE;
	}

	@Override
	public void run() {

		try {
			buildInitialRoundsUnits();

			int numRoundsSettling = 0;
			boolean settled = false;

			while (true) {

				if (!settled && !(DEGENERATE_ELIGIBLE && numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE)) {
					if (numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER && UNIT_SPAWNER_ELIGIBLE) {
						settled = moveTowardsGoodSpot();
						spawnUnit(Utils.randomDirection());
					} else {
						numRoundsSettling++;
						settled = moveTowardsGoodSpot();
					}
				} else {
					settled = true;
					createTreeRingAndSpawnUnits();
				}

				detectEnemiesAndSendHelpBroadcast();

				waterLowestHealthTree();
				drawBullshitLine();

				endTurn();
			}

		} catch (GameActionException e) {
			e.printStackTrace();
		}

	}

	private void createTreeRingAndSpawnUnits() throws GameActionException {

		Direction archonOppositeLocation = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam())[0])
				.opposite();
		rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(archonOppositeLocation), 255, 255, 255);
		if (rc.getBuildCooldownTurns() == 0) {
			Direction startAngle;
			if (SHOULD_SPAWN_TANKS) {
				startAngle = archonOppositeLocation.rotateLeftDegrees(90);
			} else {
				startAngle = archonOppositeLocation.rotateLeftDegrees(60);
			}

			while (!rc.canPlantTree(startAngle) && Math.abs(archonOppositeLocation.degreesBetween(startAngle)) >= 50) {
				startAngle = startAngle.rotateLeftDegrees(10);
			}
			if (rc.canPlantTree(startAngle)) {
				rc.plantTree(startAngle);
			} else {
				spawnUnit(archonOppositeLocation);
			}
		}
	}

	private void buildInitialRoundsUnits() throws GameActionException {
		if (rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length) {
			tryAndBuildUnit(RobotType.LUMBERJACK);
			while (rc.getBuildCooldownTurns() != 0) {
				endTurn();
			}
			tryAndBuildUnit(RobotType.LUMBERJACK);
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
		/*
		 * if (BroadcastManager.getUnitCount(rc, UnitCountInfoType.ALLY_SCOUT) <
		 * 3) { return RobotType.SCOUT; } else
		 */ if (rc.canBuildRobot(RobotType.TANK, intendedDirection)) {
			return RobotType.TANK;
		} else if (Math.random() > .7) {
			return RobotType.LUMBERJACK;
		} else {
			return RobotType.SOLDIER;
		}
	}

	private void spawnUnit(Direction direction) throws GameActionException {
		if (rc.canBuildRobot(RobotType.LUMBERJACK, direction) && rc.getBuildCooldownTurns() == 0
				&& rc.getTeamBullets() >= 100) {
			RobotType typeToBuild = determineUnitToSpawn(direction);
			rc.buildRobot(typeToBuild, direction);
		}
	}

	/*
	 * Attempts to move to a good location. Returns true if a good location was
	 * found after the move.
	 */
	private boolean moveTowardsGoodSpot() throws GameActionException {
		// Try to find a free space to settle until 20 turns have elapsed
		if (!isGoodLocation()) {
			moveDir = moveWithDiagonalBounce(moveDir);
			return false;
		} else {
			return true;
		}
	}

	private boolean isCircleOccupiedByTrees(float radius) {
		TreeInfo[] trees = rc.senseNearbyTrees(radius);
		return trees.length > 0;
	}

	private boolean edgeWithinRadius(float radius) throws GameActionException {
		MapLocation loc = rc.getLocation();
		float threshold = (float) Math.ceil(radius / 2);
		return !rc.onTheMap(loc.add(Direction.getNorth(), threshold)) || !rc.onTheMap(loc.add(Direction.getEast(), threshold))
				|| !rc.onTheMap(loc.add(Direction.getWest(), threshold)) || !rc.onTheMap(loc.add(Direction.getSouth(), threshold));
	}

	private boolean isGoodLocation() {
		try {
			// Check for free space of certain radius - gives space to spawn trees
			return !isCircleOccupiedByTrees(MIN_FREE_SPACE_REQUIREMENT) && !edgeWithinRadius(MIN_FREE_SPACE_REQUIREMENT)
					&& rc.onTheMap(rc.getLocation().add(0, (float) .01), MIN_FREE_SPACE_REQUIREMENT);
		} catch (GameActionException e) {
			return false;
		}
	}
}
