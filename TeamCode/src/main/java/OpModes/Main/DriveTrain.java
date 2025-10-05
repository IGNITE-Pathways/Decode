package OpModes.Main;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import ProgrammingBoard.ProgrammingBoardDrive;
import Constants.Drive;
@TeleOp(name = "Drive", group = "Main")
public class DriveTrain extends OpMode {

    ProgrammingBoardDrive board = new ProgrammingBoardDrive();
    Drive constants = new Drive();
    double leftFrontPower = 0;
    double leftBackPower = 0;
    double rightFrontPower = 0;
    double rightBackPower = 0;

    private double robotSpeed = constants.robotSpeed;


    @Override
    public void init() {
        board.initializeComponents(hardwareMap);

    }

    @Override
    public void loop() {

        double max;
        double axial = -gamepad1.left_stick_y;
        double lateral = gamepad1.left_stick_x;
        double yaw = gamepad1.right_stick_x;

        leftFrontPower = axial + lateral + yaw;
        rightFrontPower = axial - lateral - yaw;
        leftBackPower = axial - lateral + yaw;
        rightBackPower = axial + lateral - yaw;

        max = Math.max(Math.abs(leftFrontPower), Math.abs(rightFrontPower));
        max = Math.max(max, Math.abs(leftBackPower));
        max = Math.max(max, Math.abs(rightBackPower));

        if (max > 1.0) {
            leftFrontPower /= max;
            rightFrontPower /= max;
            leftBackPower /= max;
            rightBackPower /= max;
        }

        board.leftFrontDrive.setPower(leftFrontPower * robotSpeed);
        board.rightFrontDrive.setPower(rightFrontPower * robotSpeed);
        board.leftBackDrive.setPower(leftBackPower * robotSpeed);
        board.rightBackDrive.setPower(rightBackPower * robotSpeed);
    }
}
