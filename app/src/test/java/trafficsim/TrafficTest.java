package trafficsim;

import org.junit.jupiter.api.Test;

import trafficsim.Constants.ConstantCars;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class TrafficTest {

    @Test
    void fastCarShouldStayBehindSlowCarInSameLane() {
        Car slowCar = ConstantCars.slowCar;
        Car fastCar = ConstantCars.fastCar;

        slowCar.setDistanceFromStart(100);
        fastCar.setDistanceFromStart(50);

        List<Car> cars = List.of(slowCar, fastCar);
        Traffic t = Traffic.createSim(1, cars, Constants.dt, Constants.simTime, false);

        t.startSim();

        double slowCarPosition = slowCar.getDistanceFromStart();
        double fastCarPosition = fastCar.getDistanceFromStart();

        System.out.println("Slow: " + slowCarPosition + ", Fast: " + fastCarPosition);

        // Assert that the slow car is still ahead
        assertTrue(slowCarPosition > fastCarPosition,
            "Fast car should not pass slow car in the same lane.");
    }

    @Test
    void carShouldMaintainDesiredDistance() {
        Car lead = ConstantCars.slowCar;
        Car follow = ConstantCars.fastCar;

        lead.setDistanceFromStart(100);
        follow.setDistanceFromStart(60);

        Traffic t = Traffic.createSim(1, List.of(follow, lead), Constants.dt, 10, false);
        t.startSim();

        double gap = lead.getDistanceFromStart() - follow.getDistanceFromStart();
        assertTrue(gap >= follow.getDesiredDistance(),
            "Following car should maintain its desired distance.");
    }

    @Test
    void averageSpeedShouldIncreaseIfUnblocked() {
        Car c = new Car(1, 80, 5.0, 20, 0.5, 0.1);
        c.setDistanceFromStart(0);
        c.setSpeed(55);

        Traffic t = Traffic.createSim(1, List.of(c), Constants.dt, 10, false);
        t.startSim();

        double avgSpeed = t.getAverageSpeed();
        assertTrue(avgSpeed > 60, "Unblocked car should accelerate toward max speed.");
    }

    @Test
    void simulationWithZeroCarsShouldNotCrash() {
        Traffic t = Traffic.createSim(1, new ArrayList<>(), Constants.dt, 10, false);
        t.startSim();

        assertEquals(Double.NaN, t.getAverageSpeed(), "Simulation should not crash with 0 cars");
    }

    @Test
    void veryShortSimTimeShouldOnlyTakeOneStep() {
        Car c = new Car(1, 80, 5.0, 20, 0.5, 0.1);
        c.setDistanceFromStart(0);

        Traffic t = Traffic.createSim(1, List.of(c), Constants.dt, Constants.dt, false);
        t.startSim();

        // It should've moved a little
        assertTrue(c.getDistanceFromStart() > 0, ()->"Simulation should always take at least 1 step");
    }
}
