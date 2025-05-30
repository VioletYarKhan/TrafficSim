package trafficsim;

import org.junit.jupiter.api.Test;

import trafficsim.Constants.ConstantCars;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class TrafficTest {

    @Test
    void fastCarShouldStayBehindSlowCarInSameLane() {
        Car slowCar = ConstantCars.slowCar;

        Car fastCar = ConstantCars.fastCar;

        List<Car> cars = List.of(fastCar, slowCar);
        Traffic t = Traffic.startSim(1, cars, Constants.dt, Constants.simTime, false);

        double slowCarPosition = t.getIndexCarDist(1);
        double fastCarPosition = t.getIndexCarDist(0);

        System.out.println("Slow: " + slowCarPosition + ", Fast: " + fastCarPosition);

        // Assert that the slow car is still ahead
        assertTrue(slowCarPosition > fastCarPosition,
            "Fast car should not pass slow car in the same lane.");
    }
}
