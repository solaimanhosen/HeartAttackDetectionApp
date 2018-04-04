
int led1 = 13;
int led2 = 8;

const int analogInPin = A0;

float sensorValue;
float voltageValue;
 
//Char used for reading in Serial characters
char inbyte = 0;
 
void setup() {
  // initialise serial communications at 9600 bps:
  Serial.begin(9600);
  
  pinMode(led1, OUTPUT);
  pinMode(led2, OUTPUT);

  pinMode(10, INPUT); // Setup for leads off detection LO +
  pinMode(11, INPUT); // Setup for leads off detection LO -
  
  digitalWrite(led1, LOW);
  digitalWrite(led2, LOW);
}
 
void loop() {
  
  //when serial values have been received this will be true
  if (Serial.available() > 0)
  {
    inbyte = Serial.read();
    
    switch(inbyte){
      case 'S':
        //LED on
        digitalWrite(led1, HIGH);
        digitalWrite(led2, HIGH);
        start();
        break;
    }
  }
  delay(500);
}

void start(){
  while(1){
    //Serial.print('s');
    //Serial.print(floatMap(analogRead(sensorPin),0,1023,0,5),2);
    sensorValue = analogRead(analogInPin);
    voltageValue = floatMap(sensorValue, 0, 1023, 0, 5);
    //voltageValue = map(sensorValue, 0, 1023, 0, 5);
    //voltageValue = ((sensorValue/1023)*5);
    sendAndroidValues();
  
    if(Serial.available()>0){
      if (Serial.read()=='Q'){
        //LED off
        digitalWrite(led1, LOW);
        digitalWrite(led2, LOW);
        return;
      }
    }
  }
}

//sends the values from the sensor over serial to BT module
void sendAndroidValues()
 {
  //puts # before the values so our app knows what to do with the data
  Serial.print('#');
  Serial.print(voltageValue);
  Serial.print('+');
  
  Serial.print('~'); //used as an end of transmission character - used in app for string length
  Serial.println();
  delay(100);        //added a delay to eliminate missed transmissions
}

float floatMap(float x, float inMin, float inMax, float outMin, float outMax){
  return (x-inMin)*(outMax-outMin)/(inMax-inMin)+outMin;
}

