package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager;
import revisedstrat.BroadcastManager.LocationInfoType;
import revisedstrat.RobotLogic;
import revisedstrat.Utils;

/**
 * Created by patil215 on 1/18/17.
 */
public class LumberjackLogic extends RobotLogic {

	private Direction moveDir;
	private boolean respondToBroadcast;
	private final int TREE_TOO_FAR_AWAY_DISTANCE = 15;

	public LumberjackLogic(RobotController rc) {
		super(rc);
		moveDir = Utils.randomDirection();
		// TODO: Replace with actual logic
		respondToBroadcast = Math.random() > .5;
	}

	@Override
	public void run() {
		while (true) {
			try {

				// First priority: attack enemy
				boolean foundEnemyToTarget = false;
				RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, enemyTeam);
				for (RobotInfo enemy : enemyRobots) {
					if (/* enemy.type != RobotType.SOLDIER && */enemy.type != RobotType.TANK) {
						foundEnemyToTarget = true;
						break;
					}
				}
				if (foundEnemyToTarget) {
					attackEnemy(enemyRobots);
					endTurn();
					continue;
				}

				// Tree cutting mode
				TreeInfo[] enemyTrees = rc.senseNearbyTrees(-1, enemyTeam);
				if (enemyTrees.length > 0) {
					moveTowardsAndChop(enemyTrees);
					endTurn();
					continue;
				}

				TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				TreeInfo toChop = getTargetTree(neutralTrees);

				if (respondToBroadcast) {
					if (toChop != null && toChop.containedRobot != null
							&& (this.getDestination() == null || !getDestination().equals(toChop.getLocation()))) {
						this.setDestination(toChop.location);
					}

					if (getDestination() != null
							&& rc.getLocation().distanceTo(getDestination()) < TREE_TOO_FAR_AWAY_DISTANCE) {
						moveTowardsAndChop(getDestination());
						if (rc.getLocation().distanceTo(getDestination()) < DISTANCE_TO_CLEAR_DESTINATION) {
							setDestination(null);
						}
						endTurn();
						continue;
					} else {
						MapLocation destination = BroadcastManager.getRecentLocation(rc,
								LocationInfoType.LUMBERJACK_GET_HELP);
						setDestination(destination);
						BroadcastManager.invalidateLocation(rc, LocationInfoType.LUMBERJACK_GET_HELP);
					}
				}

				// Neutral tree cutting mode
				if (neutralTrees.length > 0) {
					moveTowardsAndChop(neutralTrees);
					endTurn();
					continue;
				}

				// Move randomly
				MapLocation archonLocation = getRandomEnemyInitialArchonLocation();
				Direction towardsEnemy = rc.getLocation().directionTo(archonLocation);
				towardsEnemy = moveTowards(towardsEnemy);
				if (towardsEnemy != null) {
					rc.move(towardsEnemy);
					if (rc.canSenseLocation(archonLocation)) {
						respondToBroadcast = true;
					}
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
		if (toChop != null) {
			rc.setIndicatorLine(rc.getLocation(), toChop.location, 80, 80, 0);
			TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees, null);
			if (rc.canChop(toChop.getID())) {
				System.out.println("sandwich");
				rc.chop(toChop.getID());
			} else if (treeInFront != null && rc.canChop(treeInFront.ID)) {
				System.out.println("read");
				rc.chop(treeInFront.ID);
			} else {
				Direction toMove = moveTowards(toChop.getLocation());
				if (toMove != null) {
					move(toMove);
				}
			}
		} else {
			System.out.println("***REMOVED***");
		}
	}

	private void moveTowardsAndChop(MapLocation destination) throws GameActionException {
		rc.setIndicatorLine(rc.getLocation(), destination, 80, 80, 0);
		TreeInfo[] trees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		TreeInfo treeInFront = getClosestTreeThatCanBeChopped(trees, rc.getLocation().directionTo(destination));
		if (treeInFront != null && rc.canChop(treeInFront.ID)) {
			System.out.println("Trying to chop");
			rc.chop(treeInFront.ID);
		} else {
			this.tryToMoveToDestination();
		}
	}

	/*
	 * Passed direction is the direction that you want to move.
	 */
	private TreeInfo getClosestTreeThatCanBeChopped(TreeInfo[] trees, Direction directionTo) {
		TreeInfo toChop = null;
		float closestDistance = Float.MAX_VALUE;
		for (TreeInfo tree : trees) {
			float distance = rc.getLocation().distanceTo(tree.location);
			if (directionTo != null) {
				if (Math.abs(rc.getLocation().directionTo(tree.location).degreesBetween(directionTo)) < 90
						&& tree.team != allyTeam && distance < closestDistance) {
					closestDistance = distance;
					toChop = tree;
				}
			} else if (tree.team != allyTeam && distance < closestDistance) {
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
			if (foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
			} else if (!foundTreeWithGoodies && tree.containedRobot != null) {
				target = tree;
				distanceToClosestTree = distance;
				foundTreeWithGoodies = true;
			} else if (!foundTreeWithGoodies && distance < distanceToClosestTree) {
				target = tree;
				distanceToClosestTree = distance;
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
