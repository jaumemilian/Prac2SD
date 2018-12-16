/*
* Copyright (c) Joan-Manuel Marques 2013. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This file is part of the practical assignment of Distributed Systems course.
*
* This code is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this code.  If not, see <http://www.gnu.org/licenses/>.
*/

package recipes_service.tsae.data_structures;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

//LSim logging system imports sgeag@2017
import edu.uoc.dpcs.lsim.LSimFactory;
import lsim.worker.LSimWorker;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class TimestampMatrix implements Serializable{
	// Needed for the logging system sgeag@2017
	private transient LSimWorker lsim = LSimFactory.getWorkerInstance();
	
	private static final long serialVersionUID = 3331148113387926667L;
	ConcurrentHashMap<String, TimestampVector> timestampMatrix = new ConcurrentHashMap<String, TimestampVector>();
	
	public TimestampMatrix(List<String> participants){
		// create and empty TimestampMatrix
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			timestampMatrix.put(it.next(), new TimestampVector(participants));
		}
	}
	
	// Constructor without parameters for clone purposes
    private TimestampMatrix()
    {
    	
    }
	
	/**
	 * Not private for testing purposes.
	 * @param node
	 * @return the timestamp vector of node in this timestamp matrix
	 */
    synchronized TimestampVector getTimestampVector(String node){
		
		return this.timestampMatrix.get(node);
	}
	
	/**
	 * Merges two timestamp matrix taking the elementwise maximum
	 * @param tsMatrix
	 */
	public synchronized void updateMax(TimestampMatrix tsMatrix)
	{
		if (tsMatrix != null)
		{
			// For each node in current timestampMatrix, check if there is vector 
			// in the matrix passed as a parameter, and execute the upddateMax for them
			for(Enumeration<String> node = this.timestampMatrix.keys(); node.hasMoreElements();){
				
				// Get the nodeId
				String nodeId = node.nextElement();
				
				// Get the TimesampVector value of the same nodeId for the other TimestampMatrix
				TimestampVector tsMatrixNodeVector = tsMatrix.getTimestampVector(nodeId);
				
				if (tsMatrixNodeVector != null)
				{
					// Get Current TimestampVector
					TimestampVector currentVector = this.getTimestampVector(nodeId);
					
					// Call the updateMax for the TimestampVector
					currentVector.updateMax(tsMatrixNodeVector);
				}
			}		
		}
	}
	
	/**
	 * substitutes current timestamp vector of node for tsVector
	 * @param node
	 * @param tsVector
	 */
	public synchronized void update(String node, TimestampVector tsVector)
	{
		if (tsVector != null)
		{
			this.timestampMatrix.put(node, tsVector);
		}
	}
	
	/**
	 * 
	 * @return a timestamp vector containing, for each node, 
	 * the timestamp known by all participants
	 */
	public synchronized TimestampVector minTimestampVector(){
		
		TimestampVector minTimestampVector = null;		
		
		// For each node in current timestampMatrix, get the minimum value 
		// and store in the minTimestampVector
		for(Enumeration<String> node = this.timestampMatrix.keys(); node.hasMoreElements();){
			
			// Get the nodeId
			String nodeId = node.nextElement();
			
			// Get the TimesampVector value
			TimestampVector currentVector = this.getTimestampVector(nodeId);
			
			// If it is the first time, get all the values from the current vector
			if (minTimestampVector == null)
			{
				minTimestampVector = currentVector.clone();
			}
			else
			{
				minTimestampVector.mergeMin(currentVector);
			}
		}				
		
		return minTimestampVector;
	}
	
	/**
	 * clone
	 */
	public synchronized TimestampMatrix clone(){
		
		TimestampMatrix timestampMatrixCloned = new TimestampMatrix();
		
		for(Enumeration<String> node = this.timestampMatrix.keys(); node.hasMoreElements();)
		{
			// Get the nodeId
			String nodeId = node.nextElement();
			
			// Get a clone of the TimestampVector of the current node
			TimestampVector vectorCloned = this.getTimestampVector(nodeId).clone();					
						
			// Add the cloned vector to the MatrixCloned instance
			timestampMatrixCloned.timestampMatrix.put(nodeId, vectorCloned);
		}
		
		return timestampMatrixCloned;
	}
	
	/**
	 * equals
	 */	
	@Override
	public synchronized boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestampMatrix other = (TimestampMatrix) obj;
		if (this.timestampMatrix == null) {
			if (other.timestampMatrix != null)
				return false;
		} else if (!timestampMatrix.equals(other.timestampMatrix))
			return false;
		return true;
	}

	
	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String all="";
		if(timestampMatrix==null){
			return all;
		}
		for(Enumeration<String> en=timestampMatrix.keys(); en.hasMoreElements();){
			String name=en.nextElement();
			if(timestampMatrix.get(name)!=null)
				all+=name+":   "+timestampMatrix.get(name)+"\n";
		}
		return all;
	}

	
}