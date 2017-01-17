package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;

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
				if (!rc.hasMoved() && !rc.hasAttacked()) {
					moveWithRandomBounce(Utils.randomDirection());
				}
				tryAndShakeATree();
				econWinIfPossible();
			} catch (

			Exception e) {
				e.printStackTrace();
			}
			drawBullshitLine();
			Clock.yield();
		}
	}

	private void handleHelp(MapLocation helpNeeded, LocationInfoType info) throws GameActionException {
		Direction toMove = moveTowards(helpNeeded);
		if (toMove != null && rc.canMove(toMove)) {
			move(toMove);
		} else {
			handleTreesInWayToDesiredLocation(rc.getLocation().directionTo(helpNeeded));
		}
	}

	private void handleRecon() throws GameActionException {
		Direction move = Utils.randomDirection();
		moveWithRandomBounce(move);
	}

	private void handleTrees(TreeInfo[] neutralTrees) throws GameActionException {
		TreeInfo closest = (TreeInfo) getClosestBody(neutralTrees);
		Direction toMove = moveTowards(closest.getLocation());
		if (toMove != null && rc.canMove(toMove)) {
			move(toMove);
		}
		if (rc.canChop(closest.ID)) {
			rc.chop(closest.ID);
		}
	}

	private void handleAttack(RobotInfo[] enemies) throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets(5);
		BulletInfo toDodge = getTargetingBullet(bullets);
		RobotInfo target = (RobotInfo) getClosestBody(enemies);
		Direction toMove = moveTowards(target.location);
		if (toDodge != null) {
			dodge(bullets);
		} else {
			if (toMove != null && rc.canMove(toMove)) {
				move(toMove);
			} else {
				Direction desired = rc.getLocation().directionTo(target.location);
				handleTreesInWayToDesiredLocation(desired);
			}
		}
		// TODO: replace with actual evaluation about striking.
		if (rc.canStrike() && rc.getLocation().distanceTo(target.location) < GameConstants.LUMBERJACK_STRIKE_RADIUS
				+ RobotType.LUMBERJACK.bodyRadius) {
			rc.strike();
		}
	}

	// TODO: refactor
	private void handleTreesInWayToDesiredLocation(Direction desired) throws GameActionException {
		TreeInfo inWay = rc.senseTreeAtLocation(
				rc.getLocation().add(desired, rc.getType().bodyRadius + GameConstants.NEUTRAL_TREE_MIN_RADIUS));
		if (inWay != null && rc.canChop(inWay.ID) && inWay.team != rc.getTeam()) {
			rc.chop(inWay.ID);
		} else {
			desired = desired.rotateLeftDegrees(15);
			inWay = rc.senseTreeAtLocation(
					rc.getLocation().add(desired, rc.getType().bodyRadius + GameConstants.NEUTRAL_TREE_MIN_RADIUS));
			if (inWay != null && rc.canChop(inWay.ID) && inWay.team != rc.getTeam()) {
				rc.chop(inWay.ID);
			} else {
				desired = desired.rotateRightDegrees(30);
				inWay = rc.senseTreeAtLocation(
						rc.getLocation().add(desired, rc.getType().bodyRadius + GameConstants.NEUTRAL_TREE_MIN_RADIUS));
				if (inWay != null && rc.canChop(inWay.ID) && inWay.team != rc.getTeam()) {
					rc.chop(inWay.ID);
				}
			}
		}
	}
}
