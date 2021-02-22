package com.example.banknoteclassifer;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Classifier {
    private Interpreter interpreter; // the agent of the model file and applciation
    private List<String> labelList; // return list
    private int inputSize;  // width of the model
    private int pixelSize = 3;
    private int imageMean = 0;
    private float imageStd = 255.0f;
    private float maxResult = 6; // label size
    private float threshold = 0.4f;

    Classifier(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException{
        this.inputSize = inputSize;
        // create a interpreter to assist Tensorflow lite running
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(7);
        options.setUseNNAPI(true);
        interpreter = new Interpreter(loadModelFile(assetManager, modelPath), options);
        labelList = loadLabelList(assetManager, labelPath);
    }

    class Recognition{
        String id = "";
        String title = "";
        float confidence = 0f;

        public Recognition(String i, String s, float confidence){
            id = i;
            title = s;
            this.confidence = confidence;
        }

        @NonNull
        @Override
        public String toString() {
            return "Title = " + title + ", confidence = " + confidence;
        }
    }
    // a huge file processor (code from tensorflow )
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String path) throws IOException{
        try {
            AssetFileDescriptor fileDescriptor = assetManager.openFd(path);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        }catch (Exception e){
            Log.i("clicked", "fail load model");
            return null;
        }

    }

    private List<String> loadLabelList(AssetManager assetManager, String path) throws IOException{
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(path)));
        String line;
        while((line = reader.readLine())!=null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    List<Recognition> recognizeImage(Bitmap bitmap){
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaleBitmap);
        float[][] result = new float[1][labelList.size()];
        interpreter.run(byteBuffer, result);
        return getSortedResultFloat(result);
    }
    // converter -- bitmap will be convert to byte format and output to  btyeBuffer
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap){
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * pixelSize);

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        /*
        for one pixel
        if val = 16
        val >> 1
        = 8
        val >> 2
        = 4
        val << 1
        32
        <<2
        = 64
        16 >> 1 = 16 /2 = /2 = 1 times
        16 >> 2 = 16/2/2 = /2 = 2times
        16 << 1 = 16 * 2 = 1 times
        16 << 2 = 16 *2*2 = 2times
         */
        for (int i = 0; i<inputSize; i++){
            for (int j = 0; j<inputSize; j++){
                final int val = intValues[pixel++];

                byteBuffer.putFloat((((val >> 16) & 0xFF) - imageMean) / imageStd);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - imageMean) / imageStd);
                byteBuffer.putFloat((((val) & 0xFF) - imageMean) / imageStd);

            }
        }
        return byteBuffer;
    }

    private List<Recognition> getSortedResultFloat(float[][] labelProArray){

        PriorityQueue<Recognition> pq = new PriorityQueue<>(
                (int) maxResult,
                new Comparator<Recognition>() {
                    @Override
                    public int compare(Recognition o1, Recognition o2) {
                        return Float.compare(o2.confidence, o1.confidence);
                    }
                }
        );

        for (int i = 0; i < labelList.size(); ++i){
            float confidence = labelProArray[0][i];
            if (confidence > threshold){
                pq.add(new Recognition("" + i, labelList.size() > 1? labelList.get(i) : "unknown", confidence));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = (int) Math.min(pq.size(), maxResult);
        for (int i = 0; i<recognitionsSize; i++){
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
