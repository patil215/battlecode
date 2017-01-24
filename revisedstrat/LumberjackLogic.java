package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

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

				// Attack enemy if there are no trees to cut
				boolean foundEnemyToTarget = false;
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, getEnemyTeam());
				for (RobotInfo enemy : enemyRobots) {
					if (enemy.type != RobotType.SOLDIER && enemy.type != RobotType.TANK) {
						foundEnemyToTarget = true;
					}
				}
				if (foundEnemyToTarget) {
					attackEnemy(enemyRobots);
					endTurn();
					continue;
				}

				// Tree cutting mode
				TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, getEnemyTeam());
				if (enemyTrees.length > 0) {
					moveTowardsAndChop(enemyTrees);
					endTurn();
					continue;
				}

				if (getDestination() != null) {
					moveTowardsAndChop(getDestination());
					if (rc.getLocation().distanceTo(getDestination()) < DISTANCE_TO_CLEAR_DESTINATION) {
						setDestination(null);
					}
					endTurn();
					continue;
				} else {
					MapLocation destination = BroadcastManager.getRecentLocation(rc, LocationInfoType.LUMBERJACK_GET_HELP);
					this.setDestination(destination);
					if(Math.random()>.3){
						BroadcastManager.invalidateLocation(rc, LocationInfoType.LUMBERJACK_GET_HELP);
					}
				}

				// Neutral tree cutting mode
				TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				if (neutralTrees.length > 0) {
					moveTowardsAndChop(neutralTrees);
					endTurn();
					continue;
				}

				// Move randomly
				Direction towardsEnemy = rc.getLocation().directionTo(getRandomEnemyInitialArchonLocation());
				towardsEnemy = moveTowards(towardsEnemy);
				if (towardsEnemy != null) {
					rc.move(towardsEnemy);
				} else {
					this.moveDir = this.moveWithRandomBounce(moveDir);
				}
				endTurn();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void moveTowardsAndChop(TreeInfo[] trees) throws GameActionException {
		TreeInfo toChop = getTargetTree(trees);
		System.out.println("Found a tree");
		if (toChop != null) {
			Direction toTree = rc.getLocation().directionTo(toChop.location);
			rc.setIndicatorLine(rc.getLocation(), toChop.location, 80, 80, 0);
			TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees);
			boolean chopped = false;
			if (rc.canChop(toChop.getID())) {
				rc.chop(toChop.getID());
				chopped = true;
			} else if (treeInFront != null && rc.canChop(treeInFront.ID)) {
				rc.chop(treeInFront.ID);
				chopped = true;
			}
			if (!chopped) {
				Direction toMove = moveTowards(toChop.getLocation());
				if (toMove != null) {
					move(toMove);
				}
			}
		}
	}

	private void moveTowardsAndChop(MapLocation destination) throws GameActionException {
		Direction toTree = rc.getLocation().directionTo(destination);
		rc.setIndicatorLine(rc.getLocation(), destination, 80, 80, 0);
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees);
		boolean chopped = false;
		if (treeInFront != null && rc.canChop(treeInFront.ID)) {
			rc.chop(treeInFront.ID);
			chopped = true;
		}
		if (!chopped) {
			Direction toMove = moveTowards(destination);
			if (toMove != null) {
				move(toMove);
			}
		}
	}

	private TreeInfo getClosestTreeThatCanBeChopped(TreeInfo[] trees) {
		TreeInfo toChop = null;
		float closestDistance = Float.MAX_VALUE;
		for (TreeInfo tree : trees) {
			float distance = rc.getLocation().distanceTo(tree.location);
			if (tree.team != rc.getTeam() && distance < closestDistance) {
				closestDistance = distance;
				toChop = tree;
			}
		}
		return toChop;
	}

	private TreeInfo getTargetTree(TreeInfo[] trees) {
		boolean foundTreeWithGoodies = false;
		float distanceToClosestTree = Float.MAX_VALUE;
		TreeInfo target = null;
		for (TreeInfo tree : trees) {
			float distance = rc.getLocation().distanceTo(tree.location);
			if (foundTreeWithGoodies && tree.containedRobot == null) {
				System.out.println(1);
				continue;
			} else if (foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
				System.out.println(2);
			} else if (!foundTreeWithGoodies && tree.containedRobot != null) {
				target = tree;
				distanceToClosestTree = distance;
				foundTreeWithGoodies = true;
				System.out.println(3);
			} else if (!foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
				System.out.println(4);
			}
		}
		return target;
	}

	private void attackEnemy(RobotInfo[] enemyRobots) throws GameActionException {
		RobotInfo target = getTarget(enemyRobots);
		Direction toMove = moveTowards(target.location);
		if (toMove != null) {
			move(toMove);
		}
		if ((rc.getLocation().distanceTo(target.location) < GameConstants.LUMBERJACK_STRIKE_RADIUS
				+ target.getType().bodyRadius) && rc.canStrike()) {
			rc.strike();
		} else {
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			TreeInfo toChop = (TreeInfo) getClosestBody(nearbyTrees);
			if (rc.canChop(toChop.ID)) {
				rc.chop(toChop.ID);
			}
		}
	}

	private RobotInfo getTarget(RobotInfo[] enemyRobots) {
		RobotInfo closestEnemy = null;
		float closestDistance = Float.MAX_VALUE;
		for (RobotInfo enemy : enemyRobots) {
			float distance = rc.getLocation().distanceTo(enemy.location);
			if (distance < closestDistance && rc.getType() != RobotType.SOLDIER && rc.getType() != RobotType.TANK) {
				closestDistance = distance;
				closestEnemy = enemy;
			}
		}
		return closestEnemy;
	}

}
