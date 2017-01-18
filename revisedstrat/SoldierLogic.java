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
					MapLocation help = BroadcastManager.getRecentLocation(rc, LocationInfoType.GARDENER_HELP);
					if (help != null) {
						handleHelp(help, LocationInfoType.GARDENER_HELP);
					} else {
						help = BroadcastManager.getRecentLocation(rc, LocationInfoType.ARCHON_HELP);
						if (help != null) {
							handleHelp(help, LocationInfoType.ARCHON_HELP);
						} else {
							handleRecon();
						}
					}
				}
				econWinIfPossible();
				tryAndShakeATree();
				drawBullshitLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private void handleHelp(MapLocation help, LocationInfoType type) throws GameActionException {
		System.out.println("dealing with a help call.");
		if(this.closeToLocationAndNoEnemies(rc, help)){
			BroadcastManager.invalidateLocation(rc, type);
		}
		else{
			Direction toMove = moveTowards(help);
			if(rc.canMove(help)){
				move(help);
			}
		}
	}

	private void handleAttack(RobotInfo[] nearbyFoes) throws GameActionException {
		System.out.println("Handling attack");
		BulletInfo[] bullets = rc.senseNearbyBullets();
		BulletInfo toDodge = getTargetingBullet(bullets);
		if (toDodge != null) {
			System.out.println("We need to dodge");
			dodge(bullets);
		}
		RobotInfo target = getHighestPriorityTarget(nearbyFoes);
		if (target != null) {
			System.out.println("We have a target");
			Direction toMove = moveTowards(target.location);
			if (toMove != null) {
				if (rc.canMove(toMove)) {
					move(toMove);
				}
			} else {
				this.moveWithRandomBounce(Utils.randomDirection());
			}
			if (rc.canFirePentadShot()) {
				rc.firePentadShot(rc.getLocation().directionTo(target.location));
			}
		} else{
			target = (RobotInfo) this.getClosestBody(nearbyFoes);
			Direction toMove = moveTowards(target.location);
			if(toMove!=null){
				if(rc.canMove(toMove)){
					move(toMove);
				}
			} else{
				this.moveWithRandomBounce(Utils.randomDirection());
			}
		}
	}

	private void handleRecon() throws GameActionException {
		System.out.println("Handling recon");

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
				} else {
					Direction move = moveTowards(getRandomEnemyInitialArchonLocation());
					if (move != null) {
						move(move);
					}
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

}
