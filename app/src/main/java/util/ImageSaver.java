package util;

import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ImageSaver implements Runnable {

    private final Image mImage;
    private File file;

    public ImageSaver(Image image, File f) {
        mImage = image;
        file = f;
    }

    @Override
    public void run() {
// Create a byte buffer to contain the byte data returned to from the camera surface
        ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
        // Set up an array of bytes to pass the data into
        byte[] bytes = new byte[byteBuffer.remaining()];
        // Copy the data from the byte buffer into the byte array
        byteBuffer.get(bytes);
        OutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            // mImage.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
