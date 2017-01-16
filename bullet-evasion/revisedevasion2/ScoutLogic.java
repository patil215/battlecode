package revisedstrat;

import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;
import revisedstrat.BroadcastManager.LocationInfoType;

import java.util.Arrays;

/**
 * Created by patil215 on 1/12/17.
 */
public class ScoutLogic extends RobotLogic {
	public ScoutLogic(RobotController rc) {
		super(rc);
	}

	private MapLocation destination;

	@Override
	public void run() {
		while (true) {
			try {
				// Check if we have company
				RobotInfo[] foes = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (foes.length != 0) {
					boolean isAttackUnit = false;
					for (RobotInfo enemy : foes) {
						if (enemy.type != RobotType.ARCHON && enemy.type != RobotType.GARDENER) {
							isAttackUnit = true;
							break;
						}
					}
					if (isAttackUnit) {
						// We need to deal with attack units, if they exist.
						handleAttack(foes);
					} else {
						// Otherwise, we can harass the enemy.
						handleHarass(foes);
					}
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
		if (destination == null) {
			MapLocation recentEnemyLocation = BroadcastManager.getRecentLocation(rc, LocationInfoType.ENEMY);
			if (recentEnemyLocation != null) {
				destination = recentEnemyLocation;
			} else {
				MapLocation[] broadcastLocations = rc.senseBroadcastingRobotLocations();
				if (broadcastLocations.length != 0) {
					int broadcastIndex = (int) Math.random() * broadcastLocations.length;
					destination = broadcastLocations[broadcastIndex];
				} else {
					Direction move = moveTowards(getRandomEnemyInitialArchonLocation());
					if (move != null) {
						rc.move(move);
					}
				}
			}
		}

		if (destination != null) {
			Direction toMove = moveTowards(destination);
			if (toMove != null) {
				rc.move(toMove);
			}
			if(rc.canSenseLocation(destination)&&rc.senseNearbyRobots(-1, getEnemyTeam()).length==0){
				destination = null;
			}
		}
	}

	// TODO: Actually allow shooting at a distance against archons. Use attack
	// code for this?
	private void handleHarass(RobotInfo[] foes) throws GameActionException {
		RobotInfo target = getPriorityEconTarget(foes);
		if (target != null) {
			BroadcastManager.saveLocation(rc, target.location, LocationInfoType.ENEMY);
			Direction toMove = moveTowards(target.location);
			if (toMove != null) {
				rc.move(toMove);
			}
			Direction towards = rc.getLocation().directionTo(target.getLocation());

			// TODO: clean up or replace with actual line of sight check.
			RobotInfo potentialTarget = rc.senseRobotAtLocation(rc.getLocation().add(towards,
					rc.getType().bodyRadius + GameConstants.BULLET_SPAWN_OFFSET + rc.getType().bulletSpeed));
			if (potentialTarget != null && potentialTarget.getTeam() == getEnemyTeam() && rc.canFireSingleShot()) {
				rc.fireSingleShot(towards);
			}
		}
	}

	private boolean isImminentThreat(BulletInfo bullet, float angleThreshold, float distanceThreshold) {
		Direction bulletToTarget = bullet.getLocation().directionTo(rc.getLocation());
		Direction bulletDir = bullet.getDir();

		// if angle and distance are within the threshold
		return Math.abs(bulletDir.degreesBetween(bulletToTarget)) < angleThreshold
				&& bullet.getLocation().distanceTo(rc.getLocation()) < distanceThreshold;
	}

	private void handleAttack(RobotInfo[] foes) throws GameActionException {
		BulletInfo[] bullets = Arrays.stream(rc.senseNearbyBullets(5)).filter(b -> isImminentThreat(b, 30, 5)).toArray(BulletInfo[]::new);

		// Dodge any dangerous bullets
		BulletInfo toDodge = getTargetingBullet(bullets);
		RobotInfo threat = (RobotInfo) getClosestBody(foes);
//		int dodged = dodgeBullets(bullets);
		if (toDodge != null) {
			dodge(toDodge);
		} else {
//		if (dodged != 0) {
			if (rc.getLocation().distanceTo(threat.location) <= 7) {
				// We are too close to the enemy. They can see us.
				Direction toMove = moveTowards(rc.getLocation().directionTo(threat.location).opposite());
				if (toMove != null) {
					rc.move(toMove);
				}
			}
		}

		// Shoot at a viable target
		RobotInfo target = getHighestPriorityTarget(foes);
		if (target != null && rc.canFireSingleShot()) {
			System.out.println("SHOOTING SCOUT");
			rc.fireSingleShot(rc.getLocation().directionTo(target.location));
		}
		if (!rc.hasAttacked() && !rc.hasMoved()) {
			Direction towardsThreat = rc.getLocation().directionTo(threat.location);
			Direction toMove = moveTowards(towardsThreat.rotateLeftDegrees(90));
			if (toMove != null && rc.canMove(toMove)) {
				rc.move(toMove);
			} else {
				toMove = moveTowards(towardsThreat.rotateRightDegrees(90));
				if (toMove != null && rc.canMove(toMove)) {
					rc.move(toMove);
				}
			}
		}
	}

	private RobotInfo getPriorityEconTarget(RobotInfo[] foes) {
		return (RobotInfo) getClosestBody(foes);
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


	/**
	 * This method dodges a list of bullets, and returns an integer status code.
	 *
	 * 0  - bullets were successfully dodged
	 * -1 - no call to rc.move() was made, and dodging either failed, or was unnecessary.
	 * @param bullets
	 * @return
	 * @throws GameActionException
	 */
	private int dodgeBullets (BulletInfo[] bullets) throws GameActionException {
		MapLocation currentLocation = rc.getLocation();

		BulletInfo[] dangerousBullets = getAllTargetingBullets(bullets, currentLocation);

		if (dangerousBullets.length == 0) {
			System.out.println("NO BULLETS TO DODGE");
			return -1;
		}

		BulletInfo toDodge = dangerousBullets[0];
		Direction toFirstBullet = rc.getLocation().directionTo(toDodge.location);
		Direction toMove;
		int leastDangerous = 90;
		int danger = Integer.MAX_VALUE;
		for (int angle = 90; angle <= 270; angle += 10) {
			toMove = toFirstBullet.rotateLeftDegrees(angle);

			int currDanger = getAllTargetingBullets(bullets, currentLocation.add(toMove, (float) 2.5)).length;
			if (currDanger < danger && rc.canMove(toMove)) {
				danger = currDanger;
				leastDangerous = angle;
			}

//			if (rc.canMove(toMove)) {
//				rc.move(toMove);
//				return 0;
//			}

//			toMove = toFirstBullet.rotateRightDegrees(angle);
//			if (rc.canMove(toMove)) {
//				rc.move(toMove);
//				return 0;
//			}
//			currDanger = getAllTargetingBullets(bullets, currentLocation.add(toMove, (float) 2.5)).length;
//			if (currDanger < danger && rc.canMove(toMove)) {
//				danger = currDanger;
//				leastDangerous = angle;
//			}
		}

		System.out.println("DANGER: " +  danger);

		if (danger != Integer.MAX_VALUE) rc.move(toFirstBullet.rotateLeftDegrees(leastDangerous));

		return 0;
	}
}
