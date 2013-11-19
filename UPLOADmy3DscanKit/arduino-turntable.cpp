#include <Servo.h>

Servo myservo;
int buttonPin = 3;
int buttonState = 0;
void setup()
{
  pinMode(buttonPin, INPUT);
   Serial.begin(115200);
  //myservo.attach(9);
 // myservo.writeMicroseconds(1500);  // set servo to mid-point
}

void loop() {

moveServo();
buttonState = digitalRead(buttonPin);
  while(buttonState!=HIGH){
    //infinite loop untill buton press to restart cycle
  }

}//end loop

void sendSerial(){
    Serial.print("AAA");//tell android to take picture
    delay(2000);//guesstimate time for picture taken
}//end sendSerial

void moveServo(){
  for(int i = 0; i < 31; i ++){
    myservo.attach(9);
    scheduleStop();
    delay(10000);//let camera complete picture taking
    sendSerial();
  }//end for
}//end moveServo

void scheduleStop(){
  myservo.writeMicroseconds(1000);//run
  delay(100);//100msec is about 15deg
  myservo.detach();//stop
}//end scheduleStop
