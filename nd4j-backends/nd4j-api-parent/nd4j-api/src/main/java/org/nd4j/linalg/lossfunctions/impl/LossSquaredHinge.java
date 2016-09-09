package org.nd4j.linalg.lossfunctions.impl;
import lombok.EqualsAndHashCode;
import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.Max;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;

/**
 * Created by susaneraly on 9/9/16.
 */
@EqualsAndHashCode
public class LossSquaredHinge implements ILossFunction {

    public INDArray scoreArray(INDArray labels, INDArray preOutput, String activationFn, INDArray mask) {
        /* y_hat is -1 or 1
        squared hinge loss is {max(0,1-y_hat*y)}^2
         */
        INDArray postOutput = Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(activationFn, preOutput.dup()));

        INDArray oneMinusyhaty = labels.mul(postOutput);
        oneMinusyhaty.muli(-1);
        oneMinusyhaty.addi(1);

        INDArray scoreArr = oneMinusyhaty.dup();
        Nd4j.getExecutioner().exec(new Max(Nd4j.zeros(labels.shape()),oneMinusyhaty,scoreArr,oneMinusyhaty.length()));

        if (mask != null) scoreArr.muliColumnVector(mask);
        return scoreArr.muli(scoreArr);
    }

    @Override
    public double computeScore(INDArray labels, INDArray preOutput, String activationFn, INDArray mask, boolean average) {
        INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);

        double score = scoreArr.sumNumber().doubleValue();

        if(average) score /= scoreArr.size(0);

        return score;
    }

    @Override
    public INDArray computeScoreArray(INDArray labels, INDArray preOutput, String activationFn, INDArray mask) {
        INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
        return scoreArr.sum(1);
    }

    @Override
    public INDArray computeGradient(INDArray labels, INDArray preOutput, String activationFn, INDArray mask) {
        INDArray postOutDer = Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(activationFn,preOutput.dup()).derivative());
        /*
        The below describes the gradient of the hinge loss function:
        gradient is 0 if yhaty is >= 1
        else gradient is gradient of the loss function = (1-yhaty) wrt preOutput = -y*derivative_of_yhat wrt preout
        Gradient of the squared hinge loss functions:
        2*hinge_loss*gradient_of_hinge_loss
        */
        INDArray yhat = Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(activationFn, preOutput.dup()));
        INDArray oneMinusyyhat = labels.mul(yhat).mul(-1).add(1);

        INDArray score = scoreArray(labels,preOutput,activationFn,null);
        //Calc gradient of hinge loss
        INDArray scoreAsFilter = score.dup();
        scoreAsFilter = scoreAsFilter.div(oneMinusyyhat);//0s and 1s now
        INDArray gradients = scoreAsFilter.mul(labels);
        gradients.muli(-1);
        gradients.muli(postOutDer);

        //now calc gradient of squared hinge loss
        gradients.muli(2);
        gradients.muli(score);
        return gradients;
    }

    @Override
    public org.apache.commons.math3.util.Pair<Double, INDArray> computeGradientAndScore(INDArray labels, INDArray preOutput, String activationFn, INDArray mask, boolean average) {
        //TODO: probably a more efficient way to do this...
        //Yes - will implement in round two. Just want to get done now.

        return new Pair<>(
                computeScore(labels, preOutput, activationFn, mask, average),
                computeGradient(labels, preOutput, activationFn, mask));
    }

    @Override
    public String toString(){
        return "LossSquaredHinge()";
    }
}
