package trafficsim;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Traffic {
    private DecimalFormat df = new DecimalFormat("0.00");
    public int lanes;
    public Car[] cars;
    public double dt;
    public double simTime;
    public boolean visualize;

    private Traffic(int lanes, int carCount, double dt, boolean visualize) {
        this.lanes = lanes;
        this.cars = new Car[carCount];
        this.dt = dt;
        this.visualize = visualize;
        initializeCars(carCount);
    }

    private Traffic(int lanes, List<Car> carInput, double dt, boolean visualize) {
        this.lanes = lanes;
        this.cars = new Car[carInput.size()];
        this.dt = dt;
        this.visualize = visualize;
        initializeCars(carInput);
    }

    private void initializeCars(int carCount) {
        Map<Integer, Double> laneLastPosition = new HashMap<>();

        for (int i = 0; i < carCount; i++) {
            int lane = (int) (Math.random() * lanes + 1);
            double maxSpeedMPH = (40 + Math.random() * 40);
            double maxAccel = 6.7 + Math.random() * 3.5;
            double desiredDistance = 5 + Math.random() * 30;
            double kP = Math.random() / 2.0 + 0.3;
            double kD = Math.random() / 20.0;

            double position = laneLastPosition.getOrDefault(lane, 0.0) + Constants.carLengthFt/2;
            position += desiredDistance + 30 + Math.random() * 20;

            Car car = new Car(lane, maxSpeedMPH, maxAccel, desiredDistance, kP, kD);
            car.assignTraffic(this);
            car.setDistanceFromStart(position);

            cars[i] = car;
            laneLastPosition.put(lane, position);
        }
    }

    private void initializeCars(List<Car> carInput) {
        Map<Integer, Double> laneLastPosition = new HashMap<>();
        int i = 0;
        for (Car car : carInput) {
            car.assignTraffic(this);
            int lane = (int) (Math.random() * lanes + 1);
            car.setLane(lane);

            double position = laneLastPosition.getOrDefault(lane, 0.0) + Constants.carLengthFt/2;
            position += car.getDesiredDistance() + 30 + Math.random() * 20;

            car.setDistanceFromStart(position);

            cars[i] = car;
            laneLastPosition.put(lane, position);
            i++;
        }
    }

    public static Traffic startSim(int lanes, int cars, double dt, double simTime, boolean visualize) {
        Traffic t = new Traffic(lanes, cars, dt, visualize);
        t.runSim(simTime);
        return t;
    }

    public static Traffic startSim(int lanes, List<Car> cars, double dt, double simTime, boolean visualize) {
        Traffic t = new Traffic(lanes, cars, dt, visualize);
        t.runSim(simTime);
        return t;
    }

    private void runSim(double totalSeconds) {
        int steps = (int) (totalSeconds / dt);
        for (int step = 0; step <= steps; step++) {
            double currentTime = step * dt;

            for (int laneNum = 1; laneNum <= lanes; laneNum++) {
                ArrayList<Car> laneCars = new ArrayList<>();
                for (Car c : cars) {
                    if (c.getLane() == laneNum) laneCars.add(c);
                }
                laneCars.sort(Comparator.comparingDouble(Car::getDistanceFromStart).reversed());

                for (int i = 0; i < laneCars.size(); i++) {
                    Car current = laneCars.get(i);
                    Car ahead = (i > 0) ? laneCars.get(i - 1) : null;
                    current.update(dt, ahead, cars);
                }
            }

            if (visualize){
                System.out.print("\033[H");
                System.out.println("Time: " + df.format(currentTime));
                printVisualization();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public double getAverageSpeed(){
        double sum = 0;
        for (Car c : cars){
            sum += c.getSpeed();
        }
        return sum/cars.length;
    }

    public double getAverageDistance(){
        double sum = 0;
        for (Car c : cars){
            sum += c.getDistanceFromStart();
        }
        return sum/cars.length;
    }

    public int getNumLanes(){
        return lanes;
    }

    public double getMaxDistance(){
        Comparator<Car> dist = Comparator.comparingDouble(Car::getDistanceFromStart);
        List<Car> carList = List.of(cars);
        return Collections.max(carList, dist).getDistanceFromStart();
    }

    public int[] carsPerLane(){
        int[] lanes = new int[Constants.lanes];
        for (Car c : cars) {
            lanes[c.getLane() - 1] ++;
        }
        return lanes;
    }

    public double getIndexCarDist(int index){
        return cars[index].getDistanceFromStart();
    }


    private void printVisualization() {
        final int screenWidth = 92; // characters wide
        final double worldWidth = 15000; // feet represented across screen
        String[][] screen = new String[lanes][screenWidth];

        for (int i = 0; i < lanes; i++) {
            for (int j = 0; j < screenWidth; j++) {
                screen[i][j] = " ";
            }
        }

        for (Car car : cars) {
            int laneIndex = car.getLane() - 1;
            int pos = (int) ((car.getDistanceFromStart() / worldWidth) * screenWidth);
            pos = Math.min(screenWidth - 1, Math.max(0, pos));
            screen[laneIndex][pos] = colorSymbolFor(car);
        }

        int endCol = (int) ((Constants.rightLaneEnd / worldWidth) * screenWidth);
        if (endCol >= 0 && endCol < screenWidth) {
            screen[lanes - 1][endCol] = "|"; // show barrier on bottom-most lane
        }

        for (int i = 0; i < lanes; i++) {
            System.out.print("Lane " + (i + 1) + ": ");
            System.out.println(String.join("", screen[i]));
        }
        System.out.println();
    }

    /*
    private String colorSymbolFor(Car car) {
        if (car.getMaxSpeed() < 45) return "\u001B[31m>\u001B[0m";
        if (car.getMaxSpeed() > 75) return "\u001B[32m>\u001B[0m";
        return ">";
    }
    */

    private String colorSymbolFor(Car car) {
        double speed = car.getSpeed(); // current speed
        if (speed < 45) return "\u001B[31m>\u001B[0m"; // red for slow
        if (speed < 65) return "\u001B[33m>\u001B[0m"; // yellow for medium
        return "\u001B[32m>\u001B[0m"; // green for fast
    }
}
