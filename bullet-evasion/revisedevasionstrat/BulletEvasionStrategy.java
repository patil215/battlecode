package revisedevasionstrat;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;

/**
 * Created by dhruv on 1/14/17.
 */
interface BulletEvasionStrategy {
    void move();
    void danger();
}

class EvadeClosest {
    static void move() {
    }
}
