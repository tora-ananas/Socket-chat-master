# Socket-chat-master
#### The main goal of the project is to create an android app to control mini-robot
#### There is a SocketServer for android app: [HelloRobot control app](https://github.com/tora-ananas/HelloRobot_mobile)
#### Start `Server` and then connect from the android client via socket
#### To make the wheels turn we use `SerialPort` and transfer bytes to open port.
#### If you need the wheels to spin forward use `public static byte[] packet_byte(int steer, int speed){...}` where `int steer`=right wheel, `int speed`=left wheel
#### If you need the wheels to spin backwards use `public static byte[] packet_minus(int steer, int speed){...}` ...
#### In func `public byte[] takePic(Webcam webcam){...}` we use `ByteBuffer bytes = webcam.getImageBytes();` to capture image from webcam and convert it in bytes and then transfer them in `DataOutputStream`
