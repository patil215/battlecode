package revisedstrat;

import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import revisedstrat.BroadcastManager.LocationInfoType;

public class SoldierLogic extends RobotLogic {
	
	private MapLocation destination;

	public SoldierLogic(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		while (true) {
			try {
				RobotInfo[] nearbyFoes = rc.senseNearbyRobots(-1, getEnemyTeam());
				if (nearbyFoes.length > 0) {
					handleAttack(nearbyFoes);
				} else {
					handleRecon();
				}
				econWinIfPossible();
				tryAndShakeATree();
				//This unit is less useful to us, as our strategy does not directly involve it.
				BroadcastManager.broadcastSpam(rc);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private void handleAttack(RobotInfo[] nearbyFoes) throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		BulletInfo toDodge = getTargetingBullet(bullets);
		if(toDodge != null){
			dodge(toDodge);
//			dodge(bullets);
		}
		RobotInfo target = getHighestPriorityTarget(nearbyFoes);
		if (target != null) {
			Direction toMove = moveTowards(target.location);
			if (!rc.hasMoved() && rc.canMove(toMove)) {
				rc.move(toMove);
			}
			if (rc.canFirePentadShot()) {
				rc.firePentadShot(rc.getLocation().directionTo(target.location));
			}
		}
	}

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

}
