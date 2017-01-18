package revisedstrat;

import battlecode.common.*;
import revisedstrat.BroadcastManager.LocationInfoType;
import revisedstrat.BroadcastManager.UnitCountInfoType;

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
				if (rc.getRoundNum() - birthRound == 20) {
					BroadcastManager.incrementUnitCount(rc, UnitCountInfoType.ALLY_SCOUT);
				}
				if (rc.getHealth() < RobotType.SCOUT.maxHealth / 5 && rc.getRoundNum() - birthRound >= 20 && !isDead) {
					BroadcastManager.decrementUnitCount(rc, UnitCountInfoType.ALLY_SCOUT);
					isDead = true;
				}
				// Check if we have company
				RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());
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
				}

				// There are no enemies in sight
				else {
					handleRecon();
				}

				tryAndShakeATree();
				econWinIfPossible();
			} catch (Exception e) {
				e.printStackTrace();
			}

			Clock.yield();
		}
	}

	// TODO: First handle broadcasted information. Also, find something to do if
	// initial archon locations are abandoned.
	private void handleRecon() throws GameActionException {
		MapLocation recentEnemyLoc = BroadcastManager.getRecentLocation(rc, LocationInfoType.ENEMY);
		if (recentEnemyLoc != null && closeToLocationAndNoEnemies(rc, recentEnemyLoc)) {
			BroadcastManager.invalidateLocation(rc, LocationInfoType.ENEMY);
			destination = null;
		}

		if (destination == null) {
			MapLocation recentEnemyLocation = BroadcastManager.getRecentLocation(rc, LocationInfoType.ENEMY);
			if (recentEnemyLocation != null) {
				destination = recentEnemyLocation;
			} else {
				MapLocation[] broadcastLocations = rc.senseBroadcastingRobotLocations();
				if (broadcastLocations.length != 0) {
					int broadcastIndex = (int) (Math.random() * broadcastLocations.length);
					destination = broadcastLocations[broadcastIndex];
				} else if(!hasVisitedEnemyArchon){
					destination = getRandomEnemyInitialArchonLocation();
					this.hasVisitedEnemyArchon=true;
				} else{
					randBounceDir = this.moveWithRandomBounce(randBounceDir);
				}
			}
		}

		if (destination != null) {
			Direction toMove = moveTowards(destination);
			if (toMove != null) {
				move(toMove);
			}
			if (rc.canSenseLocation(destination) && rc.senseNearbyRobots(-1, getEnemyTeam()).length == 0) {
				destination = null;
			}
		}
	}

	// TODO: Actually allow shooting at a distance against archons. Use attack
	// code for this?
	private void handleHarass(RobotInfo[] foes) throws GameActionException {
		RobotInfo target = getPriorityEconTarget(foes);
		if (target != null) {
			int bytecode = Clock.getBytecodeNum();
			BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY);

			BulletInfo[] bullets = rc.senseNearbyBullets();

			bytecode = Clock.getBytecodeNum();
			BulletInfo toDodge = getTargetingBullet(bullets);

			if (toDodge != null) {
				bytecode = Clock.getBytecodeNum();
				dodge(bullets);
			} else {
				Direction toMove = moveTowards(target.location);
				if (toMove != null) {
					move(toMove);
				}
			}

			bytecode = Clock.getBytecodeNum();
			RobotInfo potentialTarget = getHighestPriorityTarget(rc.senseNearbyRobots(-1, getEnemyTeam()), false);
			if (potentialTarget != null && rc.canFireSingleShot()) {
				rc.fireSingleShot(rc.getLocation().directionTo(potentialTarget.location));
			}
		}
		// TODO: refactor?
		else {
			handleRecon();
		}
	}

	private void handleAttack(RobotInfo[] foes) throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets(5);
		BulletInfo toDodge = getTargetingBullet(bullets);
		RobotInfo threat = (RobotInfo) getClosestBody(foes);
		//The only enemy is an archon, and it is too early to deal with it
		if(threat == null){
			handleRecon();
			return;
		}
		if (toDodge != null) {
			dodge(toDodge);
		} else {
			if (rc.getLocation().distanceTo(threat.location) <= 7) {
				// We are too close to the enemy. They can see us.
				Direction toMove = moveTowards(rc.getLocation().directionTo(threat.location).opposite());
				if (toMove != null) {
					move(toMove);
				}
			}
		}
		RobotInfo target = getHighestPriorityTarget(foes, false);
		if (target != null && rc.canFireSingleShot()) {
			rc.fireSingleShot(rc.getLocation().directionTo(target.location));
		}
		if (!rc.hasAttacked() && !rc.hasMoved()) {
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
		}
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

	private RobotInfo getLowestHealthEconTarget(RobotInfo[] foes) {
		if (foes.length == 0) {
			return null;
		}

		RobotInfo lowestHealthEnemy = foes[0];
		for (RobotInfo enemy : foes) {
			if (enemy.health < lowestHealthEnemy.health) {
				lowestHealthEnemy = enemy;
			}
		}

		return lowestHealthEnemy;
	}
}
