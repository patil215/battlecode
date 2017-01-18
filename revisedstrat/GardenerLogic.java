package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;
import revisedstrat.BroadcastManager.UnitCountInfoType;

/**
 * Created by patil215 on 1/12/17.
 */
public class GardenerLogic extends RobotLogic {

	private Direction moveDir;
	private int numRoundsSettling;
	private boolean settled;
	private boolean towardsCenter;

	public GardenerLogic(RobotController rc) {
		super(rc);
		numRoundsSettling = 0;
		moveDir = Utils.randomDirection();
		towardsCenter = false;
		settled = false;
	}

	@Override
	public void run() {

		try {

			if (rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length) {
				tryAndBuildUnit(RobotType.SCOUT);
				while (rc.getBuildCooldownTurns() != 0) {
					Clock.yield();
				}
				tryAndBuildUnit(RobotType.SOLDIER);
			}

			int roundCount = 0;
			Direction base = rc.getLocation().directionTo(Utils.getAvgArchonLocations(rc, getEnemyTeam()));

			boolean shouldSpawnTanks = Math.random() > .5;

			while (true) {
				if (!settled && roundCount > 30) {
					settled = true;
				} else if (!settled) {
					roundCount++;
					moveTowardsGoodSpot();
					System.out.println("Moving");
				} 
				if(settled){
					rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(base), 255, 255, 255);
					if (rc.getBuildCooldownTurns() == 0) {
						System.out.println("Should be able to spawn a unit");
						Direction startAngle;
						if (shouldSpawnTanks) {
							startAngle = base.rotateLeftDegrees(90);
						} else {
							startAngle = base.rotateLeftDegrees(60);
						}

						System.out.println("Starting to try angles");
						while (!rc.canPlantTree(startAngle) && Math.abs(base.degreesBetween(startAngle)) >= 50) {
							startAngle = startAngle.rotateLeftDegrees(10);
							System.out.println("Trying an angle of " + startAngle);
						}
						System.out.println("Done trying angles");
						if (rc.canPlantTree(startAngle)) {
							System.out.println("Trying to plant a tree");
							rc.plantTree(startAngle);
						} else {
							System.out.println("Trying to spawn unit");
							spawnUnit(base);
						}
					}
				}

				RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());

				if (foes.length > 0) {
					BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.GARDENER_HELP);
				}

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

	private void moveBackAndForth(MapLocation center, Direction base) throws GameActionException {
		if (towardsCenter) {
			if (rc.canMove(base.opposite())) {
				move(base.opposite());
			}
			if (rc.getLocation().distanceTo(center) < .25f) {
				towardsCenter = false;
			}
		} else {
			if (rc.canMove(base)) {
				move(base);
			} else {
				towardsCenter = true;
			}
			if (rc.getLocation().distanceTo(center) > 2) {
				towardsCenter = true;
			}
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

	private void spawnUnit(Direction direction) throws GameActionException {
		if (rc.canBuildRobot(RobotType.LUMBERJACK, direction) && rc.getBuildCooldownTurns() == 0
				&& rc.getTeamBullets() >= 100) {
			System.out.println("Can build");
			System.out.println(
					"We have around " + BroadcastManager.getUnitCount(rc, UnitCountInfoType.ALLY_SCOUT) + "Scouts");
			if (BroadcastManager.getUnitCount(rc, UnitCountInfoType.ALLY_SCOUT) < 3) {
				rc.buildRobot(RobotType.SCOUT, direction);
			} else if (rc.canBuildRobot(RobotType.TANK, direction)) {
				rc.buildRobot(RobotType.TANK, direction);
			} else if (Math.random() > .8) {
				rc.buildRobot(RobotType.LUMBERJACK, direction);
			} else {
				rc.buildRobot(RobotType.SOLDIER, direction);
			}
		} else {
			System.out.println("Can't build");
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
