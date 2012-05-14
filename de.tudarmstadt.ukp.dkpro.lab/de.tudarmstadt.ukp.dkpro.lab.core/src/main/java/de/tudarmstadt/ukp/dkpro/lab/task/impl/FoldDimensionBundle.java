/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.lab.task.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;

public class FoldDimensionBundle<T> extends DimensionBundle<Collection<T>> implements DynamicDimension
{
	private Dimension<T> foldedDimension;
	private List<T>[] buckets;
	private int validationBucket = -1;
	private int folds;
	
	@SuppressWarnings("unchecked")
	public FoldDimensionBundle(String aName, Dimension<T> aFoldedDimension, int aFolds)
	{
		super(aName);
		foldedDimension = aFoldedDimension;
		folds = aFolds;
	}
	
	private void init()
	{
		buckets = new List[folds];
		
		// Capture all data from the dimension into buckets, one per fold
		foldedDimension.rewind();
		int i = 0;
		while (foldedDimension.hasNext()) {
			int bucket = i % folds;
			
			if (buckets[bucket] == null) {
				buckets[bucket] = new ArrayList<T>();
			}
			
			buckets[bucket].add(foldedDimension.next());
			i++;
		}
		
		if (i < folds) {
			throw new IllegalStateException("Requested [" + folds + "] folds, but only got [" + i
					+ "] values. There must be at least as many values as folds.");
		}
	}

	@Override
	public boolean hasNext()
	{
		return validationBucket < buckets.length-1;
	}

	@Override
	public void rewind()
	{
		init();
		validationBucket = -1;
	}

	@Override
	public Map<String, Collection<T>> next()
	{
		validationBucket++;
		return current();
	}

	@Override
	public Map<String, Collection<T>> current()
	{
		List<T> trainingData = new ArrayList<T>();
		for (int i = 0; i < buckets.length; i++) {
			if (i != validationBucket) {
				trainingData.addAll(buckets[i]);
			}
		}
		
		Map<String, Collection<T>> data = new HashMap<String, Collection<T>>();
		data.put(getName()+"_training", trainingData);
		data.put(getName()+"_validation", buckets[validationBucket]);
		
		return data;
	}

	@Override
	public void setConfiguration(Map<String, Object> aConfig)
	{
		if (foldedDimension instanceof DynamicDimension) {
			((DynamicDimension) foldedDimension).setConfiguration(aConfig);
		}
	}
}
