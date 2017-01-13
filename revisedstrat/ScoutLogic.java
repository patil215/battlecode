package revisedstrat;

import battlecode.common.Clock;
import battlecode.common.RobotController;

/**
 * Created by patil215 on 1/12/17.
 */
public class ScoutLogic extends RobotLogic {
    public ScoutLogic(RobotController rc) {
        super(rc);
    }

    @Override
    public void run() {
        while(true) {
            Clock.yield();
        }
    }
}
