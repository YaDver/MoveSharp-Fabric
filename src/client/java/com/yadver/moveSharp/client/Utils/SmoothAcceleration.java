package com.yadver.moveSharp.client.Utils;

public class SmoothAcceleration {

    private double initialSpeed;
    private double currentSpeed;
    private double targetSpeed;
    private double accelerationRate;
    private boolean accelerating;

    public SmoothAcceleration(double initialSpeed, double targetSpeed, double accelerationRate) {
        this.initialSpeed = initialSpeed;
        this.currentSpeed = initialSpeed;
        this.targetSpeed = targetSpeed;
        this.accelerationRate = accelerationRate;
        this.accelerating = true;
    }

    public void restore() {
        this.currentSpeed = this.initialSpeed;
    }

    public double update() {
        if (accelerating) {
            double delta = targetSpeed - currentSpeed;
            if (Math.abs(delta) < 0.01) {
                currentSpeed = targetSpeed;
                accelerating = false;
                return currentSpeed;
            }

            currentSpeed += accelerationRate * delta;
        }
        return currentSpeed;
    }
}

