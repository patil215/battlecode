package revisedstrat;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

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
						System.out.println("Attack");
					} else {
						// Otherwise, we can harass the enemy.
						System.out.println("Harass");
						handleHarass(foes);
					}
				}

				// There are no enemies in sight
				else {
					handleRecon();
					System.out.println("Recon");
				}

				tryAndShakeATree();
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

		if (destination != null) {
			Direction toMove = moveTowards(destination);
			if (toMove != null) {
				rc.move(toMove);
			}
		}
	}

	// TODO: Actually allow shooting at a distance against archons. Use attack
	// code for this?
	private void handleHarass(RobotInfo[] foes) throws GameActionException {
		RobotInfo target = getPriorityEconTarget(foes);
		if (target != null) {
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

	private void handleAttack(RobotInfo[] foes) {
		// TODO Auto-generated method stub

	}

	private RobotInfo getPriorityEconTarget(RobotInfo[] foes) {
		return getClosestEconTarget(foes);
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

	private RobotInfo getClosestEconTarget(RobotInfo[] foes) {
		if (foes.length == 0) {
			return null;
		}

		RobotInfo closestEnemy = foes[0];
		float closestDistance = rc.getLocation().distanceTo(foes[0].location);
		for (RobotInfo enemy : foes) {
			float dist = rc.getLocation().distanceTo(enemy.location);
			if (dist < closestDistance) {
				closestEnemy = enemy;
				closestDistance = dist;
			}
		}

		return closestEnemy;
	}
}
