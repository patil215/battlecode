package revisedstrat;

import battlecode.common.*;

/**
 * Created by patil215 on 1/18/17.
 */
public class LumberjackLogic extends RobotLogic {

	Direction moveDir;

	public LumberjackLogic(RobotController rc) {
		super(rc);
		moveDir = Utils.randomDirection();
	}

	@Override
	public void run() {
		while (true) {
			try {

				/*
				 * // Combat mode RobotInfo[] enemyRobots =
				 * rc.senseNearbyRobots(-1, getEnemyTeam()); if
				 * (enemyRobots.length > 0) { executeCombat(enemyRobots);
				 * Clock.yield(); continue; }
				 */

				// Tree cutting mode
				TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, getEnemyTeam());
				if (enemyTrees.length > 0) {
					moveTowardsAndChop(enemyTrees);
					endTurn();
					continue;
				}

				// Neutral tree cutting mode
				TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				if (neutralTrees.length > 0) {
					moveTowardsAndChop(neutralTrees);
					endTurn();
					continue;
				}

				// Attack enemy if there are no trees to cut
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (enemyRobots.length > 0) {
					attackEnemy(enemyRobots);
					endTurn();
					continue;
				}

				// Move randomly
				moveDir = moveWithRandomBounce(moveDir);
				endTurn();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void moveTowardsAndChop(TreeInfo[] trees) throws GameActionException {
		TreeInfo nearestTree = (TreeInfo) getClosestBody(trees);
		if (nearestTree != null) {
			boolean chopped = false;
			if (rc.canChop(nearestTree.getID())) {
				rc.chop(nearestTree.getID());
				chopped = true;
			}
			if (!chopped) {
				move(moveTowards(nearestTree.getLocation()));
			}
		}
	}

	private void attackEnemy(RobotInfo[] enemyRobots) throws GameActionException {
		RobotInfo target = (RobotInfo) getClosestBody(enemyRobots);
		Direction toMove = moveTowards(target.location);
		if (toMove != null) {
			move(toMove);
		}
		if ((rc.getLocation().distanceTo(target.location) < GameConstants.LUMBERJACK_STRIKE_RADIUS
				+ target.getType().bodyRadius) && rc.canStrike()) {
			rc.strike();
		}
	}

}
