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
	}

	@Override
	public void run() {

		try {

			if (rc.getRobotCount() - 1 == rc.getInitialArchonLocations(rc.getTeam()).length) {
				tryAndBuildUnit(RobotType.SCOUT);
				while(rc.getBuildCooldownTurns()!=0){
					Clock.yield();
				}
				tryAndBuildUnit(RobotType.SOLDIER);
			}

			while (!settled && numRoundsSettling < 20) {
				moveTowardsGoodSpot();
				numRoundsSettling++;
				Clock.yield();
			}

			Direction base = Utils.randomDirection();

			for (int count = 0; count < 3; count++) {
				while (rc.getBuildCooldownTurns() != 0 || rc.getTeamBullets() < 50) {
					Clock.yield();
				}
				if (rc.canPlantTree(base.rotateLeftDegrees(90))) {
					System.out.println("Planting left tree");
					rc.plantTree(base.rotateLeftDegrees(90));
				}
				while (rc.getBuildCooldownTurns() != 0 || rc.getTeamBullets() < 50) {
					Clock.yield();
				}
				if (rc.canPlantTree(base.rotateRightDegrees(90))) {
					rc.plantTree(base.rotateRightDegrees(90));
					System.out.println("Planting right tree");
				}
				if (count < 2) {
					if (rc.canMove(base.rotateLeftDegrees(180))) {
						rc.move(base.rotateLeftDegrees(180));
						System.out.println("Moving down");
						Clock.yield();
					}
					if (rc.canMove(base.rotateLeftDegrees(180))) {
						rc.move(base.rotateLeftDegrees(180));
						System.out.println("Moving down");
						Clock.yield();
					}
					if (rc.canMove(base.rotateLeftDegrees(180), .1f)) {
						rc.move(base.rotateLeftDegrees(180), .1f);
						System.out.println("Moving down");
						Clock.yield();
					}
				} else {
					if (rc.canMove(base)) {
						rc.move(base);
						System.out.println("Moving down");
						Clock.yield();
					}
					if (rc.canMove(base)) {
						rc.move(base);
						System.out.println("Moving down");
						Clock.yield();
					}
				}
			}

			while (rc.getBuildCooldownTurns() != 0 || rc.getTeamBullets() < 50) {
				Clock.yield();
			}
			if (rc.canPlantTree(base.rotateLeftDegrees(180))) {
				rc.plantTree(base.rotateLeftDegrees(180));
			}
			
			MapLocation center = rc.getLocation();

			while (true) {

				moveBackAndForth(center, base);
				
				spawnUnit(base);

				RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());

				if (foes.length > 0) {
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

	private void moveBackAndForth(MapLocation center, Direction base) throws GameActionException {
		if(towardsCenter){
			if(rc.canMove(base.opposite())){
				rc.move(base.opposite());
			}
			if(rc.getLocation().distanceTo(center)<.25f){
				towardsCenter = false;
			}
		} else{
			if(rc.canMove(base)){
				rc.move(base);
			} else{
				towardsCenter = true;
			}
			if(rc.getLocation().distanceTo(center)>2){
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
		} else

		{
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
