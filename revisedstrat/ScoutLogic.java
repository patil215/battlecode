package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager;
import revisedstrat.BroadcastManager.LocationInfoType;
import revisedstrat.BroadcastManager.UnitCountInfoType;
import revisedstrat.RobotLogic;
import revisedstrat.Utils;

/**
 * Created by patil215 on 1/12/17.
 */
public class ScoutLogic extends RobotLogic {

	public ScoutLogic(RobotController rc) {
		super(rc);
		hasVisitedEnemyArchon = false;
		randBounceDir = Utils.randomDirection();
	}

	private MapLocation destination;
	private boolean hasVisitedEnemyArchon;
	private Direction randBounceDir;

	@Override
	public void run() {
		boolean isDead = false;
		int birthRound = rc.getRoundNum();
		while (true) {
			try {

				invalidateVisitedLocation();

				if (rc.getRoundNum() - birthRound == 20) {
					BroadcastManager.incrementUnitCount(rc, UnitCountInfoType.ALLY_SCOUT);
				}
				if (rc.getHealth() < RobotType.SCOUT.maxHealth / 5 && rc.getRoundNum() - birthRound >= 20 && !isDead) {
					BroadcastManager.decrementUnitCount(rc, UnitCountInfoType.ALLY_SCOUT);
					isDead = true;
				}
				// Check if we have company
				RobotInfo[] foes = rc.senseNearbyRobots(-1, enemyTeam);
				boolean foundEconUnit = false;
				for (RobotInfo info : foes) {
					if (info.type == RobotType.GARDENER || (info.type == RobotType.ARCHON && rc.getRoundNum() > 200)) {
						foundEconUnit = true;
					}
				}
				if (foundEconUnit) {
					handleHarass(foes);
				} else if (foes.length > 0) {
					handleAttack(foes);
				} else {
					handleRecon();
				}

				endTurn();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private void invalidateVisitedLocation() throws GameActionException {
		MapLocation recentEnemyLoc = BroadcastManager.getRecentLocation(rc, LocationInfoType.ENEMY_NOT_ARCHON);
		if (recentEnemyLoc != null && closeToLocationAndJustArchons(rc, recentEnemyLoc)) {
			BroadcastManager.invalidateLocation(rc, LocationInfoType.ENEMY_NOT_ARCHON);
			destination = null;
		}
	}

	// TODO: First handle broadcasted information. Also, find something to do if
	// initial archon locations are abandoned.
	private void handleRecon() throws GameActionException {
		if (destination == null) {
			MapLocation recentEnemyLocation = BroadcastManager.getRecentLocation(rc, LocationInfoType.ENEMY_NOT_ARCHON);
			if (recentEnemyLocation != null) {
				destination = recentEnemyLocation;
			} else if (!hasVisitedEnemyArchon) {
				destination = getRandomEnemyInitialArchonLocation();
				this.hasVisitedEnemyArchon = true;
			} else {
				randBounceDir = this.moveWithRandomBounce(randBounceDir);
			}
			/*else {
				MapLocation[] broadcastLocations = rc.senseBroadcastingRobotLocations();
				if (broadcastLocations.length != 0) {
					int broadcastIndex = (int) (Math.random() * broadcastLocations.length);
					destination = broadcastLocations[broadcastIndex];
				} else if (!hasVisitedEnemyArchon) {
					destination = getRandomEnemyInitialArchonLocation();
					this.hasVisitedEnemyArchon = true;
				} else {
					randBounceDir = this.moveWithRandomBounce(randBounceDir);
				}
			}*/
		} else {
			Direction toMove = moveTowards(destination);
			if (toMove != null) {
				move(toMove);
			}
			if (rc.canSenseLocation(destination)/* && rc.senseNearbyRobots(-1, enemyTeam).length == 0*/) {
				destination = null;
			}
		}
	}

	// TODO: Actually allow shooting at a distance against archons. Use attack
	// code for this?
	private void handleHarass(RobotInfo[] foes) throws GameActionException {

		System.out.println("harrassing");
		RobotInfo target = getPriorityEconTarget(foes);
		if (target != null) {

			BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY);
			if (target.getType() != RobotType.ARCHON) {
				BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY_NOT_ARCHON);
			}

			/*
			 * Direction toMove = moveTowards(target.location); if (toMove !=
			 * null) { move(toMove); }
			 */

			BulletInfo[] bullets = rc.senseNearbyBullets();
			Direction moveDir = moveTowards(target.getLocation());
			if(moveDir != null) {
				move(moveDir);
			} else {
				if(willGetHitByABullet(rc.getLocation(), bullets)) {
					MapLocation safeLocation = getBulletAvoidingLocation(rc);
					if(safeLocation != null) {
						move(safeLocation);
					}
				}
			}

			if (rc.canFireSingleShot()) {
				rc.fireSingleShot(rc.getLocation().directionTo(target.location));
			}
			/*
			 * BulletInfo toDodge = getTargetingBullet(bullets); if (toDodge !=
			 * null) { dodge(bullets); }
			 */

			/*
			 * RobotInfo potentialTarget =
			 * getHighestPriorityTarget(rc.senseNearbyRobots(-1,
			 * enemyTeam), false); if (potentialTarget != null &&
			 * rc.canFireSingleShot()) {
			 * rc.fireSingleShot(rc.getLocation().directionTo(potentialTarget.
			 * location)); }
			 */
		} else {
			handleRecon();
		}
	}

	private void handleAttack(RobotInfo[] foes) throws GameActionException {

		System.out.println("attacking");
		BulletInfo[] bullets = rc.senseNearbyBullets(5);
		RobotInfo threat = (RobotInfo) getClosestBody(foes);
		// The only enemy is an archon, and it is too early to deal with it
		if (threat != null) {
			BroadcastManager.saveLocation(rc, threat.location, LocationInfoType.ENEMY);
		}
		if (threat == null) {
			handleRecon();
			return;
		}

		//BulletInfo toDodge = getTargetingBullet(bullets);

		RobotInfo target = (RobotInfo) getClosestBody(foes);
		if (target != null && rc.canFireSingleShot()) {
			rc.fireSingleShot(rc.getLocation().directionTo(target.location));
		}
		// RobotInfo target = getHighestPriorityTarget(foes, false);
		boolean willBeHit = willGetHitByABullet(rc.getLocation(), bullets);
		MapLocation safeLocation = null;
		if(willBeHit) {
			System.out.println("WE GONNA GET HIT");
			safeLocation = getBulletAvoidingLocation(rc);
		}
		if (willBeHit && safeLocation != null) {
			System.out.println("DEPLOYING EVASIVE MANEUVERS");
			move(safeLocation);
		} else {
			if (rc.getLocation().distanceTo(threat.location) <= 7) {
				// We are too close to the enemy. They can see us.
				Direction toMove = moveTowards(rc.getLocation().directionTo(threat.location).opposite());
				if (toMove != null) {
					move(toMove);
				}
			}
		}
		/*if (!rc.hasAttacked() && !rc.hasMoved()) {
			Direction towardsThreat = rc.getLocation().directionTo(threat.location);
			Direction toMove = moveTowards(towardsThreat.rotateLeftDegrees(90));
			if (toMove != null && rc.canMove(toMove)) {
				move(toMove);
			} else {
				toMove = moveTowards(towardsThreat.rotateRightDegrees(90));
				if (toMove != null && rc.canMove(toMove)) {
					move(toMove);
				}
			}
		}*/
	}

	private RobotInfo getPriorityEconTarget(RobotInfo[] foes) {
		RobotInfo target = null;
		float leastDistance = Float.MAX_VALUE;
		for (RobotInfo info : foes) {
			if (info.type == RobotType.GARDENER || (rc.getType() == RobotType.ARCHON && rc.getRoundNum() > 200)) {
				float distance = rc.getLocation().distanceTo(info.location);
				if (distance < leastDistance) {
					leastDistance = distance;
					target = info;
				}
			}
		}
		return target;
	}
}
