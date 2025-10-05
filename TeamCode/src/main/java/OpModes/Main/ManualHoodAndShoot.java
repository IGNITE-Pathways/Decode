package OpModes.Main;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

import ProgrammingBoard.ProgrammingBoardShooter;

@TeleOp(name="Flywheel + Hood Control", group="TeleOp")
public class ManualHoodAndShoot extends OpMode {

    private Servo hoodServo;
    private double servoPosition = 0.0; // start fully down
    private final double SERVO_INCREMENT = 0.005; // smoother servo steps

    private ProgrammingBoardShooter board = new ProgrammingBoardShooter();
    private double flywheelPower = 0.1; // starting power
    private boolean spinning = false;   // flywheel state

    @Override
    public void init() {
        // Initialize hardware
        board.initializeComponents(hardwareMap);

        hoodServo = hardwareMap.get(Servo.class, "hoodservo");
        hoodServo.setPosition(servoPosition);

        telemetry.addData("Status", "Initialized. Servo pos: %.2f | Flywheel power: %.2f",
                servoPosition, flywheelPower);
        telemetry.update();
    }

    @Override
    public void loop() {
        // ----- Servo Control (Triggers) -----
        if (gamepad1.right_trigger > 0.1) { // RT pressed
            servoPosition += SERVO_INCREMENT;
        }
        if (gamepad1.left_trigger > 0.1) {  // LT pressed
            servoPosition -= SERVO_INCREMENT;
        }

        // Clamp servo position
        servoPosition = Math.max(0.0, Math.min(1.0, servoPosition));
        hoodServo.setPosition(servoPosition);

        // ----- Flywheel Control -----
        // Increase power with Triangle (Y)
        if (gamepad1.triangle) {
            flywheelPower += 0.01;
        }

        // Decrease power with X
        if (gamepad1.cross) {
            flywheelPower -= 0.01;
        }

        // Clamp power between 0 and 1
        flywheelPower = Math.max(0.0, Math.min(1.0, flywheelPower));

        // Start spinning with Square
        if (gamepad1.square) {
            spinning = true;
        }

        // Stop spinning with Circle
        if (gamepad1.circle) {
            spinning = false;
        }

        // Apply power to flywheel
        board.flyWheelMotor.setPower(spinning ? flywheelPower : 0);

        // ----- Telemetry -----
        telemetry.addData("Hood Servo Pos", "%.2f", servoPosition);
        telemetry.addData("Flywheel Power", "%.2f", flywheelPower);
        telemetry.addData("Flywheel Status", spinning ? "Shooting" : "Stopped");
        telemetry.update();
    }
}
