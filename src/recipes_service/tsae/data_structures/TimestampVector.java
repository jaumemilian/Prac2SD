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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//LSim logging system imports sgeag@2017
import lsim.worker.LSimWorker;
import recipes_service.data.Operation;
import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */
public class TimestampVector implements Serializable{
	// Needed for the logging system sgeag@2017
	private transient LSimWorker lsim = LSimFactory.getWorkerInstance();

	private static final long serialVersionUID = -765026247959198886L;
	/**
	 * This class stores a summary of the timestamps seen by a node.
	 * For each node, stores the timestamp of the last received operation.
	 */
	
	private ConcurrentHashMap<String, Timestamp> timestampVector= new ConcurrentHashMap<String, Timestamp>();
	
	public TimestampVector (List<String> participants){
		// create and empty TimestampVector
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			String id = it.next();
			// when sequence number of timestamp < 0 it means that the timestamp is the null timestamp
			timestampVector.put(id, new Timestamp(id, Timestamp.NULL_TIMESTAMP_SEQ_NUMBER));
		}
	}
	
	/** 
	 * Constructor that creates a new instance of TimestampVector from a ConcurrentHashMap<String, Timestamp>
	 * @param timestampVector
	 */
	public TimestampVector(ConcurrentHashMap<String, Timestamp> timestampVector)
	{
		this.timestampVector = new ConcurrentHashMap<String, Timestamp>(timestampVector);
	}

	/**
	 * Updates the timestamp vector with a new timestamp. 
	 * @param timestamp
	 */
	public synchronized void updateTimestamp(Timestamp timestamp){
		//lsim.log(Level.TRACE, "Updating the TimestampVectorInserting with the timestamp: "+timestamp);
				
		if (timestamp != null)
		{
			// Get the currentHostId from the operation timestamp
			String currentHostId = timestamp.getHostid();
			
			timestampVector.replace(currentHostId, timestamp);
		}		
	}
	
	/**
	 * merge in another vector, taking the elementwise maximum
	 * @param tsVector (a timestamp vector)
	 */
	public synchronized void updateMax(TimestampVector tsVector){
		if (tsVector != null)
		{
			// For each node in current timestampvector, check if there is value 
			// in the passed as a parameter vector, and get the max value from both
			for(Enumeration<String> node = this.timestampVector.keys(); node.hasMoreElements();){
				
				// Get the nodeId
				String nodeId = node.nextElement();
				
				// Get the value of the same nodeId for the other TimestampVector
				Timestamp tsVectorNodeValue = tsVector.getLast(nodeId);
				
				if (tsVectorNodeValue != null)
				{
					// Compare if the Timestamp of the node passed by parameter is greater than the current one
					if (this.getLast(nodeId).compare(tsVectorNodeValue) < 0) 
					{
						this.timestampVector.replace(nodeId, tsVectorNodeValue);
					}
				}
			}			
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return the last timestamp issued by node that has been
	 * received.
	 */
	public synchronized Timestamp getLast(String node){
		
		return this.timestampVector.get(node);
	}
	
	/**
	 * merges local timestamp vector with tsVector timestamp vector taking
	 * the smallest timestamp for each node.
	 * After merging, local node will have the smallest timestamp for each node.
	 *  @param tsVector (timestamp vector)
	 */
	public synchronized void mergeMin(TimestampVector tsVector){
		if (tsVector != null)
		{
			// For each node in current timestampvector, check if there is value 
			// in the passed as a parameter vector, and get the min value from both
			for(Enumeration<String> node = this.timestampVector.keys(); node.hasMoreElements();){
				
				// Get the nodeId
				String nodeId = node.nextElement();
				
				// Get the value of the same nodeId for the other TimestampVector
				Timestamp tsVectorNodeValue = tsVector.getLast(nodeId);
				
				if (tsVectorNodeValue != null)
				{
					// Compare if the Timestamp of the node passed by parameter is greater than the current one
					if (this.getLast(nodeId).compare(tsVectorNodeValue) > 0) 
					{
						this.timestampVector.replace(nodeId, tsVectorNodeValue);
					}
				}
			}			
		}
	}
	
	/**
	 * clone
	 */
	public synchronized TimestampVector clone(){
		
		// Return a clone of the current TimestampVector object
		return new TimestampVector(this.timestampVector);
	}
	
	/**
	 * equals
	 */
	public synchronized boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimestampVector other = (TimestampVector) obj;
		if (timestampVector == null) {
			if (other.timestampVector != null)
				return false;
		} else if (!timestampVector.equals(other.timestampVector))
			return false;
		return true;
	}

	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String all="";
		if(timestampVector==null){
			return all;
		}
		for(Enumeration<String> en=timestampVector.keys(); en.hasMoreElements();){
			String name=en.nextElement();
			if(timestampVector.get(name)!=null)
				all+=timestampVector.get(name)+"\n";
		}
		return all;
	}
}