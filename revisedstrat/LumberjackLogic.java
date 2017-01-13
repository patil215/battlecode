package revisedstrat;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

/**
 * Created by patil215 on 1/12/17.
 */
public class LumberjackLogic extends RobotLogic {

	public LumberjackLogic(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while (true) {
			try {
				RobotInfo[] enemies = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (enemies.length > 0) {
					handleAttack(enemies);
				} else {
					TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, getEnemyTeam());
					if (enemyTrees.length > 0) {
						handleTrees(enemyTrees);
					} else {
						TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
						if (neutralTrees.length > 0) {
							handleTrees(neutralTrees);
						} else {
							handleRecon();
						}
					}
				}
				tryAndShakeATree();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private void handleRecon() throws GameActionException {
		Direction move = Utils.randomDirection();
		moveWithRandomBounce(move);
	}

	private void handleTrees(TreeInfo[] neutralTrees) throws GameActionException {
		TreeInfo closest = findClosestTree(neutralTrees);
		Direction toMove = moveTowards(closest.location);
		if (toMove != null && rc.canMove(toMove)) {
			rc.move(toMove);
		}
		if (rc.canChop(closest.ID)) {
			rc.chop(closest.ID);
		}
	}

	private TreeInfo findClosestTree(TreeInfo[] trees) {
		TreeInfo closestTree = trees[0];
		float distance = rc.getLocation().distanceTo(trees[0].location);
		for (int index = 1; index < trees.length; index++) {
			float distanceToTree = rc.getLocation().distanceTo(trees[index].location);
			if (distance > distanceToTree) {
				closestTree = trees[index];
				distance = distanceToTree;
			}
		}
		return closestTree;
	}

	private void handleAttack(RobotInfo[] enemies) {
		// TODO Auto-generated method stub

	}
}
