package com.ccc.sendalyzeit.textanalytics.algorithms.datasets.fetchers;

import java.util.List;

import org.jblas.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ccc.sendalyzeit.deeplearning.berkeley.Pair;
import com.ccc.sendalyzeit.textanalytics.algorithms.datasets.iterator.DataSetFetcher;
import com.ccc.sendalyzeit.textanalytics.util.MatrixUtil;

public abstract class BaseDataFetcher implements DataSetFetcher {
	
	protected int cursor = 0;
	protected int numOutcomes = -1;
	protected int inputColumns = -1;
	protected Pair<DoubleMatrix,DoubleMatrix> curr;
	protected int totalExamples;
	protected static Logger log = LoggerFactory.getLogger(BaseDataFetcher.class);
	
	
	protected DoubleMatrix createInputMatrix(int numRows) {
		return new DoubleMatrix(numRows,inputColumns);
	}
	
	protected DoubleMatrix createOutputVector(int outcomeLabel) {
		return MatrixUtil.toOutcomeVector(outcomeLabel, numOutcomes);
	}
	
	protected DoubleMatrix createOutputMatrix(int numRows) {
		return new DoubleMatrix(numRows,numOutcomes);
	}
	
	protected void initializeCurrFromList(List<Pair<DoubleMatrix,DoubleMatrix>> examples) {
		
		if(examples.isEmpty())
			log.warn("Warning: empty dataset from the fetcher");
		
		DoubleMatrix inputs = createInputMatrix(examples.size());
		DoubleMatrix labels = createOutputMatrix(examples.size());
		for(int i = 0; i < examples.size(); i++) {
			inputs.putRow(i, examples.get(i).getFirst());
			labels.putRow(i,examples.get(i).getSecond());
		}
		curr = new Pair<DoubleMatrix,DoubleMatrix>(inputs,labels);

	}
	
	@Override
	public boolean hasMore() {
		return cursor < totalExamples;
	}

	@Override
	public Pair<DoubleMatrix, DoubleMatrix> next() {
		return curr;
	}

	@Override
	public int totalOutcomes() {
		return numOutcomes;
	}

	@Override
	public int inputColumns() {
		return inputColumns;
	}

	@Override
	public int totalExamples() {
		return totalExamples;
	}

	@Override
	public void reset() {
		cursor = 0;
	}

	@Override
	public int cursor() {
		return cursor;
	}
	
	

	
}
