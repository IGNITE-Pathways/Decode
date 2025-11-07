package ProgrammingBoard;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.CRServo;

public class ProgrammingBoardShooter {
    public DcMotor flyWheelMotor = null;
    public DcMotor flyWheelMotor2 = null;
    public Servo hoodServo;
    public CRServo BallLauncherServo;
    public void initializeComponents(HardwareMap hwMap) {
        flyWheelMotor = hwMap.get(DcMotor.class, "flywheelmotor");
        flyWheelMotor2 = hwMap.get(DcMotor.class, "flywheelmotor2");
//        flyWheelMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        hoodServo = hwMap.get(Servo.class, "hoodservo");

        BallLauncherServo = hwMap.get(CRServo.class, "balllauncher");

    }

}
