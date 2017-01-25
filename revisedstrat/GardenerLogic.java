package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

	private final int NUM_ROUNDS_BEFORE_UNIT_SPAWNER_ELIGIBLE = 200;
	private final int NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE = 400;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER = 50;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE = 25;
	private final float MIN_FREE_SPACE_REQUIREMENT = 5;

	private Direction moveDir;
	private final boolean UNIT_SPAWNER_ELIGIBLE;
	private final boolean DEGENERATE_ELIGIBLE;
	private final boolean SHOULD_SPAWN_TANKS;

	public GardenerLogic(RobotController rc) {
		super(rc);
		moveDir = Utils.diagonalDirection();

		double TANK_SPAWNER_CHANCE = rc.getRoundNum() * 2.0 / rc.getRoundLimit();
		SHOULD_SPAWN_TANKS = Math.random() < TANK_SPAWNER_CHANCE
				&& !(rc.getRobotCount() - 1 == allyArchonLocations.length);
		UNIT_SPAWNER_ELIGIBLE = rc.getRoundNum() > NUM_ROUNDS_BEFORE_UNIT_SPAWNER_ELIGIBLE;
		DEGENERATE_ELIGIBLE = rc.getRoundNum() < NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE;
	}

	// TODO: make gardener only send help broadcast every 50 rounds

	@Override
	public void run() {

		try {

			buildInitialRoundsUnits();

			int numRoundsSettling = 0;
			boolean settled = false;

			while (true) {

				if (!settled && !(DEGENERATE_ELIGIBLE
						&& numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE)) {
					if (numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER
							&& UNIT_SPAWNER_ELIGIBLE) {
						settled = moveTowardsGoodSpot();
						System.out.println("spawning unit");
						spawnUnit(Utils.randomDirection());
					} else {
						numRoundsSettling++;
						if (rc.getBuildCooldownTurns() == 0 && rc.getRobotCount() - 2 <= allyArchonLocations.length) {
							tryToBuildUnit(determineUnitToSpawn(Utils.randomDirection()));
						}
						settled = moveTowardsGoodSpot();
					}
				} else {
					settled = true;
					createTreeRingAndSpawnUnits();
					detectTreesAndAskLumberjacksForHelp();
				}

				sendHelpBroadcastIfNeeded();
				waterLowestHealthTree();

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
			BroadcastManager.saveLocation(rc, closestTree.location, LocationInfoType.LUMBERJACK_GET_HELP);
		}
	}

	private void createTreeRingAndSpawnUnits() throws GameActionException {

		Direction archonOppositeLocation = rc.getLocation().directionTo(allyArchonLocations[0]).opposite();
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

	private float getNearbyTreeSpread(TreeInfo[] nearbyTrees) {
		float totalDifferenceX = 0;
		float totalDifferenceY = 0;
		for (int count = 0; count < 10; count++) {
			MapLocation randomTree = nearbyTrees[(int) (Math.random() * nearbyTrees.length)].location;
			totalDifferenceX += rc.getLocation().x - randomTree.x;
			totalDifferenceY += rc.getLocation().y - randomTree.y;
		}
		return (totalDifferenceY + totalDifferenceX);
	}

	private void buildInitialRoundsUnits() throws GameActionException {
		if (rc.getRobotCount() - 1 == allyArchonLocations.length) {
			// Build first unit depending on tree density
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			if (nearbyTrees.length != 0) {
				float treeSpread = getNearbyTreeSpread(nearbyTrees);
				if (Math.abs(treeSpread) < 20) {
					tryToBuildUnit(RobotType.LUMBERJACK);
				} else {
					tryToBuildUnit(RobotType.SOLDIER);
				}
			} else {
				tryToBuildUnit(RobotType.SOLDIER);
			}

			// Wait until we can build second unit
			while (rc.getBuildCooldownTurns() != 0) {
				endTurn();
			}

			// Build second unit depending on how far archons are from each
			// other
			float closestEnemyArchonDistance = Float.MAX_VALUE;
			MapLocation[] enemyArchons = enemyArchonLocations;
			for (MapLocation startLocation : enemyArchons) {
				float distance = rc.getLocation().distanceTo(startLocation);
				if (distance < closestEnemyArchonDistance) {
					closestEnemyArchonDistance = distance;
				}
			}
			if (closestEnemyArchonDistance < 50) {
				tryToBuildUnit(RobotType.SOLDIER);
			} else {
				tryToBuildUnit(RobotType.LUMBERJACK);
			}
		}
	}

	private void tryToBuildUnit(RobotType toBuild) throws GameActionException {
		System.out.println("trying to build unit");
		Direction test = Direction.getNorth();
		for (int deltaDegree = (int) (Math.random() * 360), count = 0; count < 36; deltaDegree+=10, deltaDegree %= 360, count++) {
			if (rc.canBuildRobot(toBuild, test.rotateLeftDegrees(deltaDegree))) {
				rc.buildRobot(toBuild, test.rotateLeftDegrees(deltaDegree));
				return;
			}
		}
	}

	private void spawnUnit(Direction direction) throws GameActionException {
		if (rc.getBuildCooldownTurns() == 0 && rc.getTeamBullets() >= 100) {
			RobotType typeToBuild = determineUnitToSpawn(direction);
			if (rc.canBuildRobot(typeToBuild, direction)) {
				rc.buildRobot(typeToBuild, direction);
			}
		}
	}

	private void waterLowestHealthTree() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(-1, allyTeam);
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
		if (rc.canBuildRobot(RobotType.TANK, intendedDirection)) {
			return RobotType.TANK;
		}
		double chanceToSpawnLumberjack = getLumberjackSpawnChance();
		if (Math.random() < chanceToSpawnLumberjack) {
			return RobotType.LUMBERJACK;
		} else {
			return RobotType.SOLDIER;
		}
	}

	private double getLumberjackSpawnChance() {
		// TODO: Fix logical error with trees that are only partially in the
		// sense radius.
		double senseArea = Math.pow(rc.getType().sensorRadius, 2);
		double treeArea = 0;
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		for (TreeInfo t : trees) {
			treeArea += Math.pow(t.radius, 2);
		}
		return (treeArea / senseArea) + 0.1;
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

	private void sendHelpBroadcastIfNeeded() throws GameActionException {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, enemyTeam);

		if (foes.length > 0) {
			BroadcastManager.saveLocation(rc, foes[0].location, BroadcastManager.LocationInfoType.GARDENER_HELP);
		}
	}

	private boolean isCircleOccupiedByTrees(float radius) {
		TreeInfo[] trees = rc.senseNearbyTrees(radius);
		return trees.length > 0;
	}

	private boolean isGoodLocation() {
		try {
			// Check for free space of certain radius - gives space to spawn
			// trees
			return !isCircleOccupiedByTrees(MIN_FREE_SPACE_REQUIREMENT) && !edgeWithinRadius(MIN_FREE_SPACE_REQUIREMENT)
					&& rc.onTheMap(rc.getLocation().add(0, (float) .01), MIN_FREE_SPACE_REQUIREMENT);
		} catch (GameActionException e) {
			return false;
		}
	}
}
