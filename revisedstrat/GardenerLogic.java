package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

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

			if (rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length) {
				tryAndBuildUnit(RobotType.SCOUT);
			}

			while (true) {
				if (!settled && numRoundsSettling < 20) {
					moveTowardsGoodSpot();
					numRoundsSettling++;
				} else if (!settled) {
					settled = true;
				}

				if (settled) {
					// Look for a place to spawn a tree

					if (rc.getBuildCooldownTurns() == 0) {
						Direction spawnDir = rc.getLocation()
								.directionTo(rc.getInitialArchonLocations(getEnemyTeam())[0]).opposite()
								.rotateRightDegrees(60);
						System.out.println(spawnDir.getAngleDegrees());
						int degreesRotated = 0;
						while (degreesRotated < 360
								&& !rc.canPlantTree(spawnDir.rotateLeftDegrees(degreesRotated))) {
							degreesRotated += 10;
						}

						spawnDir = spawnDir.rotateLeftDegrees(degreesRotated);
						System.out.println(degreesRotated);
						System.out.println(spawnDir.getAngleDegrees());
						// If can spawn tree, spawn tree
						if (degreesRotated <= 240 && rc.canPlantTree(spawnDir)) {
							rc.plantTree(spawnDir);
						} else if (rc.canPlantTree(spawnDir)) {
							spawnUnit(spawnDir);
						}
					}
				}
				
                RobotInfo[] foes = rc.senseNearbyRobots(-1,getEnemyTeam());
                
                if(foes.length>0){
                	BroadcastManager.saveLocation(rc, foes[0].location, LocationInfoType.GARDENER_HELP);
                }

				waterLowestHealthTree();
				tryAndShakeATree();
				econWinIfPossible();
				Clock.yield();
			}

		} catch (GameActionException e) {
			e.printStackTrace();
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
		if (rc.canBuildRobot(RobotType.LUMBERJACK, direction)) {
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
