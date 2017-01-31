package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

	private final int NUM_ROUNDS_BEFORE_UNIT_SPAWNER_ELIGIBLE = 0;
	private final int NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE = 2000;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER = 50;
	private final int NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE;
	private final float MIN_FREE_SPACE_REQUIREMENT = 5;

	private Direction moveDir;
	private final boolean UNIT_SPAWNER_ELIGIBLE;
	private final boolean DEGENERATE_ELIGIBLE;
	private Direction unitSpawnDir;

	private boolean builtInitialUnits;
	private boolean builtInitialUnit1;
	private boolean builtInitialUnit2;

	private static boolean INITIAL;

	public GardenerLogic(RobotController rc) {
		super(rc);
		moveDir = Utils.diagonalDirection();
		UNIT_SPAWNER_ELIGIBLE = rc.getRoundNum() > NUM_ROUNDS_BEFORE_UNIT_SPAWNER_ELIGIBLE;
		DEGENERATE_ELIGIBLE = rc.getRoundNum() < NUM_ROUNDS_BEFORE_NOT_DEGENERATE_ELIGIBLE;
		NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE = rc.getRoundNum() < 20 ? 20 : 50;
		INITIAL = rc.getRobotCount() - allyArchonLocations.length == 1;
		builtInitialUnits = false;
		builtInitialUnit1 = false;
		builtInitialUnit2 = false;
	}

	// TODO: make gardener only send help broadcast every 50 rounds

	@Override
	public void run() {


		int numRoundsSettling = 0;
		boolean settled = false;

		while (true) {
			try {

				if(INITIAL) {
					if (!builtInitialUnits) {
						buildInitialRoundsUnits();
						builtInitialUnits = builtInitialUnit1 && builtInitialUnit2;
					}
				}

				beginTurn();

				if (!settled) {
					settled = moveTowardsGoodSpot(numRoundsSettling);
					if (numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_A_UNIT_SPAWNER
							&& UNIT_SPAWNER_ELIGIBLE) {
						if (rc.getBuildCooldownTurns() == 0 && rc.getRoundNum() > 150) {
							tryToBuildUnit(determineUnitToSpawn(Utils.randomDirection()));
						}
					}
					numRoundsSettling++;
				} else {
					settled = true;
					if (inDanger()) {
						tryToBuildUnit(RobotType.SOLDIER);
					} else {
						createTreeRingAndSpawnUnits();
					}
					detectTreesAndAskLumberjacksForHelp();
				}

				sendHelpBroadcastIfNeeded();
				waterLowestHealthTree();

				endTurn();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
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

		if (unitSpawnDir == null) {
			unitSpawnDir = rc.getLocation().directionTo(allyArchonLocations[0]).opposite();
			for (int i = 0; i < 72; i++) {
				MapLocation proposedLoc = rc.getLocation().add(unitSpawnDir, (float) (2 * type.bodyRadius + 0.1));
				if (!rc.isCircleOccupied(proposedLoc, rc.getType().bodyRadius) && !notOnMapCircle(proposedLoc)) {
					break;
				}
				unitSpawnDir = unitSpawnDir.rotateLeftDegrees(5);
			}
		}
		if (rc.getBuildCooldownTurns() == 0) {
			Direction startAngle = unitSpawnDir;
			for (int i = 0; i < 5; i++) {
				startAngle = startAngle.rotateLeftDegrees(60);
				if (rc.canPlantTree(startAngle)) {
					rc.plantTree(startAngle);

//					if ((i == 3 && rc.getTeamBullets() > 175 )|| (rc.getRoundNum() < 500 && rc.getTeamBullets() > 125)) {
//						spawnUnit(unitSpawnDir);
//					}
					return;
				}

			}
			spawnUnit(unitSpawnDir);
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


		int numLumberjacks = BroadcastManager.getLumberjackInitialCount(rc);

		if (numLumberjacks == 0) {
			if(!builtInitialUnit1) {
				if (BroadcastManager.getScoutInitialCount(rc) > 0) {
					boolean result = tryToBuildUnit(RobotType.SCOUT);
					if(result) {
						builtInitialUnit1 = true;
						return;
					}
				} else {
					boolean result = tryToBuildUnit(RobotType.SOLDIER);
					if(result) {
						builtInitialUnit1 = true;
						return;
					}
				}
			}
			// Wait until we can build second unit
			/*while (rc.getBuildCooldownTurns() != 0) {
				endTurn();
			}*/
			if(!builtInitialUnit2) {
				boolean result = tryToBuildUnit(RobotType.SOLDIER);
				if(result) {
					builtInitialUnit2 = true;
					return;
				}
			}
		} else if (numLumberjacks == 1) {
			if(!builtInitialUnit1) {
				boolean result = tryToBuildUnit(RobotType.LUMBERJACK);
				if(result) {
					builtInitialUnit1 = true;
					return;
				}
			}
			// Wait until we can build second unit
			/*while (rc.getBuildCooldownTurns() != 0) {
				endTurn();
			}*/
			if(!builtInitialUnit2) {
				boolean result = tryToBuildUnit(RobotType.SOLDIER);
				if(result) {
					builtInitialUnit2 = true;
					return;
				}
			}
		} else {
			if(!builtInitialUnit1) {
				boolean result = tryToBuildUnit(RobotType.LUMBERJACK);
				if(result) {
					builtInitialUnit1 = true;
					return;
				}
			}
			/*// Wait until we can build second unit
			while (rc.getBuildCooldownTurns() != 0) {
				endTurn();
			}*/
			if(!builtInitialUnit2) {
				boolean result = tryToBuildUnit(RobotType.LUMBERJACK);
				if(result) {
					builtInitialUnit2 = true;
					return;
				}
			}
		}
		/*
		 * if (rc.getRobotCount() - 1 == allyArchonLocations.length) { // Build
		 * first unit depending on tree density TreeInfo[] nearbyTrees =
		 * rc.senseNearbyTrees(); if (nearbyTrees.length != 0) { float
		 * treeSpread = getNearbyTreeSpread(nearbyTrees); if
		 * (Math.abs(treeSpread) < 20) { tryToBuildUnit(RobotType.LUMBERJACK); }
		 * else { tryToBuildUnit(RobotType.SOLDIER); } } else {
		 * tryToBuildUnit(RobotType.SOLDIER); }
		 *
		 * // Wait until we can build second unit while
		 * (rc.getBuildCooldownTurns() != 0) { endTurn(); }
		 *
		 * // Build second unit depending on how far archons are from each //
		 * other float closestEnemyArchonDistance = Float.MAX_VALUE;
		 * MapLocation[] enemyArchons = enemyArchonLocations; for (MapLocation
		 * startLocation : enemyArchons) { float distance =
		 * rc.getLocation().distanceTo(startLocation); if (distance <
		 * closestEnemyArchonDistance) { closestEnemyArchonDistance = distance;
		 * } } if (closestEnemyArchonDistance < 50) {
		 * tryToBuildUnit(RobotType.SOLDIER); } else {
		 * tryToBuildUnit(RobotType.LUMBERJACK); } }
		 */
	}

	private boolean tryToBuildUnit(RobotType toBuild) throws GameActionException {
		Direction test = Direction.getNorth();
		for (int deltaDegree = (int) (Math.random()
				* 360), count = 0; count < 36; deltaDegree += 10, deltaDegree %= 360, count++) {
			if (rc.canBuildRobot(toBuild, test.rotateLeftDegrees(deltaDegree))) {
				rc.buildRobot(toBuild, test.rotateLeftDegrees(deltaDegree));
				return true;
			}
		}
		return false;
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
		if (Math.random() < chanceToSpawnLumberjack || Math.random() < .1) {
			return RobotType.LUMBERJACK;
		} else {
			return RobotType.SOLDIER;
		}
	}

	private double getLumberjackSpawnChance() throws GameActionException {
		// TODO: Fix logical error with trees that are only partially in the
		// sense radius.
		int locationsFoundOnMap = 0;
		int locationsFoundWithTrees = 0;
		for (int count = 0; count < 50; count++) {
			MapLocation toTest = rc.getLocation().add(Utils.randomDirection(),
					(float) (rc.getType().sensorRadius * Math.random()));
			if (rc.onTheMap(toTest)) {
				locationsFoundOnMap++;
				TreeInfo foundTree = rc.senseTreeAtLocation(toTest);
				if (foundTree != null && foundTree.team != rc.getTeam()) {
					locationsFoundWithTrees++;
				}
			}
		}
		return (((double) locationsFoundWithTrees) / locationsFoundOnMap) * 1.5;
	}

	/*
	 * Attempts to move to a good location. Returns true if a good location was
	 * found after the move.
	 */
	private boolean moveTowardsGoodSpot(int numRoundsSettling) throws GameActionException {
		// Try to find a free space to settle until 20 turns have elapsed
		if (!isGoodLocation(numRoundsSettling)) {
			/*
			 * TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam()); if
			 * (trees.length > 0) moveWithPathFinding(); else
			 */
			moveDir = moveWithDiagonalBounce(moveDir);
			return false;
		} else {
			return true;
		}
	}

	private boolean moveWithPathFinding() throws GameActionException {

		if (this.getDestination() == null || rc.getLocation().distanceTo(this.getDestination()) < 3) {
			/*
			 * MapLocation destination =
			 * rc.getLocation().add(birthLocation.directionTo(rc.getLocation()),
			 * rc.getType().sensorRadius); if (rc.canSenseLocation(destination)
			 * && rc.onTheMap(destination)) { setDestination(destination); }
			 * else { double rand = Math.random(); if (rand > .5) {
			 * setDestination(new MapLocation(rc.getLocation().x,
			 * this.getRandomEnemyInitialArchonLocation().y)); } else {
			 * setDestination(new MapLocation(rc.getLocation().y,
			 * this.getRandomEnemyInitialArchonLocation().x)); } }
			 */

			MapLocation goodSpot = BroadcastManager.getRecentLocation(rc, LocationInfoType.GOOD_SPOT);
			if (goodSpot != null) {
				setDestination(goodSpot);
			} else {
				double rand = Math.random();
				if (rand > .5) {
					setDestination(new MapLocation(rc.getLocation().x, this.getRandomEnemyInitialArchonLocation().y));
				} else {
					setDestination(new MapLocation(this.getRandomEnemyInitialArchonLocation().x, rc.getLocation().y));
				}
			}

		}

		return tryToMoveToDestinationTwo();
	}

	private void sendHelpBroadcastIfNeeded() throws GameActionException {
		RobotInfo[] foes = rc.senseNearbyRobots(-1, enemyTeam);

		if (foes.length > 0) {
			BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.GARDENER_HELP);
		}
	}

	private boolean isCircleOccupiedByTrees(float radius) {
		TreeInfo[] trees = rc.senseNearbyTrees(radius);
		return trees.length > 0;
	}

	private boolean notOnMapCircle(MapLocation proposedLocation) throws GameActionException {
		return !rc.onTheMap(proposedLocation.add(Direction.NORTH, (float) 1.05))
				|| !rc.onTheMap(proposedLocation.add(Direction.EAST, (float) 1.05))
				|| !rc.onTheMap(proposedLocation.add(Direction.WEST, (float) 1.05))
				|| !rc.onTheMap(proposedLocation.add(Direction.SOUTH, (float) 1.05));
	}

	private boolean isGoodLocation(int numRoundsSettling) {
		try {
			if (DEGENERATE_ELIGIBLE && numRoundsSettling > NUM_ROUNDS_BEFORE_GIVING_UP_TO_BECOME_DEGENERATE) {
				System.out.println("DEGENERATE ELIGIBLE");
				Direction start = rc.getLocation().directionTo(allyArchonLocations[0]).opposite();
				int settleSpots = 0;
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
				outer: for (int count = 0; count < 6; count++) {
					MapLocation proposedLocation = rc.getLocation().add(start.rotateLeftDegrees(count * 60),
							(float) 2.01);
					if (!notOnMapCircle(proposedLocation)) {
						if (!rc.isCircleOccupiedExceptByThisRobot(proposedLocation, 1)) {
							rc.setIndicatorDot(proposedLocation, 255, 0, 0);
							settleSpots++;
						} else {
							for (TreeInfo tree : nearbyTrees) {
								if (tree.location.distanceTo(proposedLocation) <= (tree.radius + 1)) {
									continue outer;
								}
							}
							rc.setIndicatorDot(proposedLocation, 0, 255, 0);
							settleSpots++;
						}
					}
				}
				if (settleSpots >= 3) {
					return true;
				}
				return false;
			} else {
				System.out.println("NOT DEGENERATE ELIGIBLE");
				// Check for free space of certain radius - gives space to spawn
				// trees
				return !isCircleOccupiedByTrees(MIN_FREE_SPACE_REQUIREMENT)
						&& !edgeWithinRadius(MIN_FREE_SPACE_REQUIREMENT)
						&& rc.onTheMap(rc.getLocation().add(0, (float) .01), MIN_FREE_SPACE_REQUIREMENT);
			}
		} catch (GameActionException e) {
			return false;
		}
	}
}