    package trafficsim;

    public class Car {
        private int lane;
        private double maxSpeedMPH;
        private double maxAccelMPHSquared;
        private double desiredDistanceFromCarAhead;
        private double distanceFromStart;
        private double currentSpeedMPH;
        private double kP;
        private double kD;
        private double lengthFt;
        private Traffic t;

        public static final double laneChangeGap = 10.0; 

        public Car(int lane, double maxSpeedMPH, double maxAccelMPHSquared, double desiredDistanceFromCarAhead, double kP, double kD) {
            this.lane = lane;
            this.maxSpeedMPH = maxSpeedMPH;
            this.maxAccelMPHSquared = maxAccelMPHSquared;
            this.desiredDistanceFromCarAhead = desiredDistanceFromCarAhead;
            this.distanceFromStart = 0;
            this.currentSpeedMPH = maxSpeedMPH/2;
            this.kP = kP;
            this.kD = kD;
            this.lengthFt = Constants.carLengthFt;
        }

        public void assignTraffic(Traffic t){
            this.t = t;
        }

        public int getLane() {
            return lane;
        }

        public void setLane(int lane){
            this.lane = lane;
        }

        public double getDistanceFromStart() {
            return distanceFromStart;
        }

        public double getDesiredDistance(){
            return desiredDistanceFromCarAhead;
        }

        public double getLength() {
            return lengthFt;
        }

        public double getSpeed(){
            return currentSpeedMPH;
        }

        public double getMaxSpeed(){
            return maxSpeedMPH;
        }

        public void setDistanceFromStart(double distance) {
            this.distanceFromStart = distance;
        }

        public void tryLaneChange(Car[] cars) {
            Car frontCar = findFrontCar(cars, lane);
            if ((frontCar == null || frontCar.getDistanceFromStart() - this.distanceFromStart > desiredDistanceFromCarAhead * 2.5) && !(Constants.rightLaneEnd - getDistanceFromStart() < 600 && Constants.rightLaneEnd > 0 && this.lane == t.getNumLanes())) {
                return; // No need to change lane, enough space ahead
            }

            for (int dir = -1; dir <= 1; dir += 2) { // Check left (-1) and right (+1)
                boolean requiredChange = false;
                int newLane = lane + dir;
                boolean rightLaneAvailable = Constants.rightLaneEnd < 0 || getDistanceFromStart() < Constants.rightLaneEnd;
                if (newLane < 1 || newLane > (rightLaneAvailable ? t.getNumLanes() : t.getNumLanes() - 1)) {
                    continue;
                } else{
                    new String("new lane detected");
                }

                Car frontInNewLane = findFrontCar(cars, newLane);
                Car frontInCurrentLane = findFrontCar(cars, this.lane);

                boolean laneClear = true;
                double minGapBehind = desiredDistanceFromCarAhead/2;
                double minGapAhead = desiredDistanceFromCarAhead/2;


                for (Car other : cars) {
                    if (other == this || other.lane != newLane){
                        continue;
                    }

                    double gap = other.getDistanceFromStart() - this.distanceFromStart;

                    if (gap > 0) {
                        // Car is ahead in the new lane
                        double distAhead = (other.getDistanceFromStart() - other.getLength() / 2) - (this.distanceFromStart + this.getLength() / 2);
                        if (distAhead < minGapAhead) {
                            laneClear = false;
                            break;
                        }
                    } else {
                        // Car is behind in the new lane
                        double distBehind = (this.distanceFromStart - this.getLength() / 2) - (other.getDistanceFromStart() + other.getLength() / 2);
                        if (distBehind < minGapBehind) {
                            laneClear = false;
                            break;
                        }
                    }
                }
                if (Constants.rightLaneEnd - getDistanceFromStart() < 600 && Constants.rightLaneEnd > 0 && newLane == t.getNumLanes()){
                    laneClear = false;
                }

                boolean wouldBeFaster = false;
                if (laneClear) {
                    if (frontInCurrentLane == null) {
                        wouldBeFaster = false; // No car ahead in current lane, so no benefit
                    } else if (frontInNewLane == null) {
                        wouldBeFaster = true; // No car ahead in new lane
                    } else {
                        double gapCurrent = frontInCurrentLane.getDistanceFromStart() - this.getDistanceFromStart();
                        double gapNew = frontInNewLane.getDistanceFromStart() - this.getDistanceFromStart();

                        double speedCurrent = frontInCurrentLane.getSpeed();
                        double speedNew = frontInNewLane.getSpeed();

                        // Prefer lane if more space or if front car is faster
                        wouldBeFaster = (gapNew > gapCurrent) || (speedNew > speedCurrent);
                    }
                }

                if (this.lane == t.getNumLanes() && Constants.rightLaneEnd > 0 &&
                    this.distanceFromStart > Constants.rightLaneEnd - 500) {
                    requiredChange = true;
                }

                if (laneClear && (wouldBeFaster || requiredChange)) {
                    // System.out.println("Lane Change! " + lane + " to " + newLane);
                    this.lane = newLane;
                    break;
                }
            }
        }

        public void update(double dt, Car carAhead, Car[] allCars) {
            tryLaneChange(allCars);
            double targetSpeed = maxSpeedMPH;

            double gap = Double.MAX_VALUE;
            double relativeSpeed = 0;

            if (carAhead != null && carAhead.lane == this.lane) {
                gap = (carAhead.getDistanceFromStart()-(carAhead.getLength()/2)) - (this.distanceFromStart+(this.getLength()/2));
                relativeSpeed = carAhead.currentSpeedMPH - this.currentSpeedMPH;

                // Adjust target speed using PD control
                double error = gap - desiredDistanceFromCarAhead;
                double derivative = relativeSpeed;

                double speedAdjustment = kP * error + kD * derivative;

                targetSpeed = currentSpeedMPH + speedAdjustment;

                targetSpeed = Math.min(maxSpeedMPH, Math.max(0, targetSpeed));
            }

            if (this.lane == t.getNumLanes()) {
                double distanceToEnd = Constants.rightLaneEnd - (this.distanceFromStart + this.getLength() / 2);
                
                if (distanceToEnd < 700 && Constants.rightLaneEnd > 0) {
                    boolean canChange = false;
                    int newLane = this.lane - 1;

                    for (Car other : allCars) {
                        if (other != this && other.lane == newLane) {
                            double dist = Math.abs(other.getDistanceFromStart() - this.distanceFromStart);
                            if (dist < this.desiredDistanceFromCarAhead) {
                                currentSpeedMPH /= 2;
                                targetSpeed /= 2;
                                canChange = false;
                                break;
                            }
                            canChange = true;
                        }
                    }

                    if (!canChange) {
                        // Slow down to avoid driving off the road
                        targetSpeed = Math.min(targetSpeed, (distanceToEnd / 10)); // braking logic
                    }
                }

                // Actually stop if we hit the end and couldn't merge
                if (distanceToEnd <= 30 && Constants.rightLaneEnd > 0) {
                    targetSpeed = 0;
                    currentSpeedMPH /= 2;
                }
            }

            // Apply acceleration limits
            double speedDiff = targetSpeed - currentSpeedMPH;
            double maxDelta = maxAccelMPHSquared * dt;

            if (Math.abs(speedDiff) > maxDelta) {
                speedDiff = Math.copySign(maxDelta, speedDiff);
            }

            currentSpeedMPH += speedDiff;

            // Update position based on new speed
            double feetPerHour = currentSpeedMPH * 5280;
            double feetPerSecond = feetPerHour / 3600;
            double movementFeet = feetPerSecond * dt;

            double proposedFront = distanceFromStart + movementFeet + (getLength() / 2);
            double proposedBack = distanceFromStart + movementFeet - (getLength() / 2);

            for (Car other : allCars) {
                if (other == this || other.lane != this.lane) continue;

                double otherFront = other.getDistanceFromStart() + (other.getLength() / 2);
                double otherBack = other.getDistanceFromStart() - (other.getLength() / 2);

                boolean overlaps = !(proposedBack >= otherFront || proposedFront <= otherBack);
                if (overlaps) {
                    // Collision would happen, cancel movement
                    movementFeet = Math.max(0, otherBack - (this.getLength() / 2) - distanceFromStart - 1);
                    currentSpeedMPH = Math.max(0, currentSpeedMPH - 10 * dt);
                    break;
                }
            }
            distanceFromStart += movementFeet;
        }

        private Car findFrontCar(Car[] cars, int laneToCheck) {
            Car closest = null;
            double closestDist = Double.POSITIVE_INFINITY;

            for (Car other : cars) {
                if (other == this){
                    continue;
                }
                if (other.lane != laneToCheck){
                    continue;
                }
                double gap = other.distanceFromStart - this.distanceFromStart;
                if (gap > 0 && gap < closestDist) {
                    closestDist = gap;
                    closest = other;
                }
            }

            return closest;
        }

        @Override
        public String toString() {
            return String.format("Lane %d | Speed: %.1f mph | Pos: %.1f ft | DesiredGap: %.1f ft", 
                    lane, currentSpeedMPH, distanceFromStart, desiredDistanceFromCarAhead);
        }
    }
