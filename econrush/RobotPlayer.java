package econrush;

import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	/**
	 * run() is the method that is called when a robot is instantiated in the
	 * Battlecode world. If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
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
		case LUMBERJACK:
			runJack();
			break;
		}
	}

	private static void runJack() {
		// TODO Auto-generated method stub

	}

	private static void runGardener() throws GameActionException {
		try {
			Direction move = randomDirection();
			int turnCount = 20;
			while (true) {
				if (move != null) {
					System.out.println(rc.getLocation());
					if (turnCount == 0
							|| rc.isCircleOccupiedExceptByThisRobot(rc.getLocation().add(0, (float) .001), 3)) {
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
				if (rc.getTeamBullets() > 9999) {
					rc.donate(10000);
				}
				Clock.yield();
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

	}

	private static void runArchon() throws GameActionException {
		Direction move = randomDirection();
		while (true) {
			// We always want at least five gardeners
			if (rc.getRobotCount() < 5 + rc.getInitialArchonLocations(rc.getTeam()).length
					&& rc.canHireGardener(move.opposite())) {
				rc.hireGardener(move.opposite());
			} else if (rc.canHireGardener(move.opposite())) {
				// Maths to find out if we should hire a gardener
				float currentBullets = rc.getTeamBullets();
				int trees = rc.getTreeCount();
				float neededBullets = 10000 - currentBullets;
				float treesPerGardener = trees
						/ (rc.getRobotCount() - rc.getInitialArchonLocations(rc.getTeam()).length);
				int turnsToVictory = (int) Math.ceil(neededBullets / trees);
				int turnsForGardenerToBeProfit = (int) Math
						.ceil(100 + (treesPerGardener * 50 + 100) / treesPerGardener);
				if (turnsForGardenerToBeProfit < turnsToVictory) {
					rc.hireGardener(move.opposite());
				}
			}
			move = RobotPlayer.moveWithRandomBounce(move);
			Clock.yield();
		}
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

	static Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

}
