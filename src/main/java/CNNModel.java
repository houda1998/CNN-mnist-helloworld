import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.Random;
public class CNNModel {
    public static void main(String[] args) throws IOException, InterruptedException {
        long seed = 1234;
        double learningRate = 0.001;
        long height = 28;
        long width = 28;
        long depth = 1;
        int batchSize = 54;
        int outputSize = 10;
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(new Adam(learningRate))
                .list()
                .setInputType(InputType.convolutionalFlat(height, width, depth))
                .layer(0, new ConvolutionLayer.Builder()
                        .nIn(depth)
                        .nOut(20)
                        .activation(Activation.RELU)
                        .kernelSize(5, 5)
                        .stride(1, 1)
                        .build())
                .layer(1, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .poolingType(SubsamplingLayer.PoolingType.MAX)
                        .build())
                .layer(2, new ConvolutionLayer.Builder()
                        .nOut(50)
                        .activation(Activation.RELU)
                        .kernelSize(5, 5)
                        .stride(1, 1)
                        .build())
                .layer(3, new SubsamplingLayer.Builder()
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .poolingType(SubsamplingLayer.PoolingType.MAX)
                        .build())
                .layer(4, new DenseLayer.Builder()
                        .nOut(500)
                        .activation(Activation.RELU)
                        .build())
                .layer(5, new OutputLayer.Builder()
                        .nOut(outputSize)
                        .activation(Activation.SOFTMAX)
                        .lossFunction(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .build())
                .build();
      //  System.out.println(configuration.toJson());
        MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        model.init();

        System.out.println("Model training");

        String path = System.getProperty("user.home")+"/mnist_png";
        File fileTrain = new File(path + "/training");
        FileSplit fileSplitTrain = new FileSplit(fileTrain, NativeImageLoader.ALLOWED_FORMATS,new Random(seed));
        RecordReader recordeReaderTrain = new ImageRecordReader(height, width, depth, new ParentPathLabelGenerator());
        recordeReaderTrain.initialize(fileSplitTrain);
        DataSetIterator dataSetIteratorTrain = new RecordReaderDataSetIterator(recordeReaderTrain, batchSize, 1, outputSize);
        DataNormalization scaler=new ImagePreProcessingScaler(0,1);
        dataSetIteratorTrain.setPreProcessor(scaler);

        UIServer uiServer=UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        model.setListeners(new StatsListener(statsStorage));

        int numEpoch=1;
        for(int i = 0;i <numEpoch; i++){
            model.fit(dataSetIteratorTrain);
        }
      /* while (dataSetIteratorTrain.hasNext()) {
            DataSet dataSet = dataSetIteratorTrain.next();
            INDArray features = dataSet.getFeatures();
            INDArray TargetLabels = dataSet.getLabels();
            System.out.println(features.shapeInfoToString());
            System.out.println(TargetLabels);
            System.out.println("------------------------------");
        }*/
        System.out.println("Model Evaluation");

        File fileTest = new File(path+"/testing");
        FileSplit fileSplitTest = new FileSplit(fileTest, NativeImageLoader.ALLOWED_FORMATS,new Random(seed));
        RecordReader recordeReaderTest = new ImageRecordReader(height, width, depth, new ParentPathLabelGenerator());
        recordeReaderTest.initialize(fileSplitTest);
        DataSetIterator dataSetIteratorTest = new RecordReaderDataSetIterator(recordeReaderTest, batchSize, 1, outputSize);
        DataNormalization scalerTest=new ImagePreProcessingScaler(0,1);
        dataSetIteratorTest.setPreProcessor(scalerTest);

        Evaluation evaluation=new Evaluation();
        while (dataSetIteratorTest.hasNext()){
            DataSet dataSet = dataSetIteratorTest.next();
            INDArray features=dataSet.getFeatures();
            INDArray TargetLabels=dataSet.getLabels();
            INDArray predictedLabels=model.output(features);
            evaluation.eval(predictedLabels,TargetLabels);
        }
        System.out.println(evaluation.stats());
    }
}
