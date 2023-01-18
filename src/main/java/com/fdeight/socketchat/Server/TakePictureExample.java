package com.fdeight.socketchat.Server;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


import com.github.sarxos.webcam.Webcam;
import org.bytedeco.javacv.CanvasFrame;

import javax.imageio.ImageIO;


/**
 * Example of how to take single picture.
 *
 * @author Bartosz Firyn (SarXos)
 */
public class TakePictureExample {

    public static void main(String[] args) throws IOException, InterruptedException {

        // get default webcam and open it
        Webcam webcam = Webcam.getDefault();
        System.out.println(webcam.getViewSize());
        webcam.open();
        CanvasFrame canvas = new CanvasFrame(" ");

        // get image
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //ImageIO.write(image, )
        BufferedImage image = webcam.getImage();
        ImageIO.write(image, "png", baos);
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos.close();
        System.out.println(bytes.length);

        ByteBuffer bytes1 = webcam.getImageBytes();
        byte[] arr = new byte[bytes1.remaining()];
        bytes1.get(arr);
        System.out.println(arr.length);
        /*System.out.println(image.getHeight());
        System.out.println(image.getWidth());
        ByteBuffer bytes = webcam.getImageBytes();
        byte[] arr = new byte[bytes.remaining()];
        bytes.get(arr);
        System.out.println(arr.length);*/



    }
}