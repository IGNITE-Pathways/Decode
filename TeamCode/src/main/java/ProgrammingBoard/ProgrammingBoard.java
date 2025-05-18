package ProgrammingBoard;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;


public class ProgrammingBoard {

    ;
    public DcMotor leftFrontDrive = null;
    public DcMotor leftBackDrive = null;
    public DcMotor rightFrontDrive = null;
    public DcMotor rightBackDrive = null;

    public void initializeComponents(HardwareMap hwMap) {
        leftFrontDrive = hwMap.get(DcMotor.class, "leftfrontmotor");
        leftFrontDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftBackDrive = hwMap.get(DcMotor.class, "leftbackmotor");
        leftBackDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        rightFrontDrive = hwMap.get(DcMotor.class, "rightfrontmotor");
        rightFrontDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        rightBackDrive = hwMap.get(DcMotor.class, "rightbackmotor");
        rightBackDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftFrontDrive.setDirection(DcMotor.Direction.REVERSE);
        leftBackDrive.setDirection(DcMotor.Direction.REVERSE);
        rightFrontDrive.setDirection(DcMotor.Direction.FORWARD);
        rightBackDrive.setDirection(DcMotor.Direction.FORWARD);

    }

}
