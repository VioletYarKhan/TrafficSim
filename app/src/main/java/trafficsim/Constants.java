package trafficsim;

public class Constants {
    public static int lanes = 2;
    public static int cars = 100;
    public static double dt = 0.02;
    public static double simTime = 120;
    public static double carLengthFt = 15;
    public static double rightLaneEnd = -7000;


    public static class ConstantCars{
        public static final Car fastCar = new Car(0, 70, 10, 10, 0.7, 0.03);
        public static final Car mediumCar = new Car(0, 50, 8, 10, 0.6, 0.025);
        public static final Car slowCar = new Car(0, 30, 6, 10, 0.5, 0.02);
    }
}
