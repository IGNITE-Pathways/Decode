package OpModes;

import static android.os.SystemClock.sleep;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import ProgrammingBoard.ProgrammingBoard;


public class DriveTrain extends OpMode {

    ProgrammingBoard board = new ProgrammingBoard();

    double leftFrontPower = 0;
    double leftBackPower = 0;
    double rightFrontPower = 0;
    double rightBackPower = 0;

    private double robotSpeed = 1.0;


    @Override
    public void init() {
        board.initializeComponents(hardwareMap);

    }

    @Override
    public void loop() {

        double max;

        // POV Mode uses left joystick to go forward & strafe, and right joystick to rotate.
        double axial = -gamepad1.left_stick_y;  // Note: pushing stick forward gives negative value
        double lateral = gamepad1.left_stick_x;
        double yaw = gamepad1.right_stick_x;

        // Combine the joystick requests for each axis-motion to determine each wheel's power.
        // Set up a variable for each drive wheel to save the power level for telemetry.
        leftFrontPower = axial + lateral + yaw;
        rightFrontPower = axial - lateral - yaw;
        leftBackPower = axial - lateral + yaw;
        rightBackPower = axial + lateral - yaw;

        // Normalize the values so no wheel power exceeds 100%
        // This ensures that the robot maintains the desired motion.
        max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
        max = Math.max(max, Math.abs(leftBackPower));
        max = Math.max(max, Math.abs(rightBackPower));

        if (max > 1.0) {
            leftFrontPower /= max;
            rightFrontPower /= max;
            leftBackPower /= max;
            rightBackPower /= max;
        }

        // Send calculated power to wheels
        board.leftFrontDrive.setPower(leftFrontPower * robotSpeed);
        board.rightFrontDrive.setPower(rightFrontPower * robotSpeed);
        board.leftBackDrive.setPower(leftBackPower * robotSpeed);
        board.rightBackDrive.setPower(rightBackPower * robotSpeed);
    }
}
