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
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

//LSim logging system imports sgeag@2017
import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;
import lsim.worker.LSimWorker;
import recipes_service.data.Operation;

/**
 * @author Joan-Manuel Marques, Daniel Lázaro Iglesias
 * December 2012
 *
 */
public class Log implements Serializable{
	// Needed for the logging system sgeag@2017
	private transient LSimWorker lsim = LSimFactory.getWorkerInstance();

	private static final long serialVersionUID = -4864990265268259700L;
	/**
	 * This class implements a log, that stores the operations
	 * received  by a client.
	 * They are stored in a ConcurrentHashMap (a hash table),
	 * that stores a list of operations for each member of 
	 * the group.
	 */
	private ConcurrentHashMap<String, List<Operation>> log= new ConcurrentHashMap<String, List<Operation>>();  

	public Log(List<String> participants){
		// create an empty log
		for (Iterator<String> it = participants.iterator(); it.hasNext(); ){
			log.put(it.next(), new Vector<Operation>());
		}
	}

	/**
	 * inserts an operation into the log. Operations are 
	 * inserted in order. If the last operation for 
	 * the user is not the previous operation than the one 
	 * being inserted, the insertion will fail.
	 * 
	 * @param op
	 * @return true if op is inserted, false otherwise.
	 */
	public boolean add(Operation op){
		//lsim.log(Level.TRACE, "Inserting into Log the operation: "+op);
		
		// Get the currentHostId from the operation timestamp
		String currentHostId = op.getTimestamp().getHostid();
				
		// Get the list of operations for the current HostId
		List<Operation> operationList = log.get(currentHostId);
		
		// Initialize lastTimestamp as it was no timestamp
		Timestamp lastTimestamp = new Timestamp(currentHostId, -1);
		
		if (operationList != null & operationList.size() > 0) {
			// Get the last timestamp from the last operation
			lastTimestamp = operationList.get(operationList.size()-1).getTimestamp();
		}		
		
		// Check if the current operation is next for the last stored in the log 
		if (op.getTimestamp().compare(lastTimestamp) == 1)
		{
			// Add the operation to the operation list
			if (operationList.add(op))
			{
				// Update the log
				log.put(currentHostId, operationList);			
				return true;
			}

			//lsim.log(Level.TRACE, "Operation failed when being added: "+op);
			return false;
		}
		else
		{
			//lsim.log(Level.TRACE, "Operation can not be inserted because there are some operations pending to be logged: "+op);
			return false;
		}		
	}
	
	/**
	 * Checks the received summary (sum) and determines the operations
	 * contained in the log that have not been seen by
	 * the proprietary of the summary.
	 * Returns them in an ordered list.
	 * @param sum
	 * @return list of operations
	 */
	public List<Operation> listNewer(TimestampVector sum){
		
		List<Operation> listOfNewOperations = new Vector<Operation>();
		
		// Foreach node in the current log, check if there are operations that are missing
		// in the sum vector passed, to be returned in the list		
		for(Enumeration<String> node = this.log.keys(); node.hasMoreElements();)
		{
			// Get the nodeId
			String nodeId = node.nextElement();
			
			Timestamp timestampFromSum = sum.getLast(nodeId);
			
			// Get the operations of the nodeId from current log
			List<Operation> currentNodeOperations = this.log.get(nodeId);
			
			// Loop all the operations to check the ones that are missing in the log
			for(Operation currentOperation : currentNodeOperations)
			{
				// Get the timestamp of the current operation
				Timestamp currentOperationTimestamp = currentOperation.getTimestamp();
				
				// Check (against the sum vector) if the operation is missing in the log
				if (currentOperationTimestamp.compare(timestampFromSum) > 0)
				{
					listOfNewOperations.add(currentOperation);
				}
			}			
		}		
		
		return listOfNewOperations;
	}
	
	/**
	 * Removes from the log the operations that have
	 * been acknowledged by all the members
	 * of the group, according to the provided
	 * ackSummary. 
	 * @param ack: ackSummary.
	 */
	public void purgeLog(TimestampMatrix ack){
	}

	/**
	 * equals
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Log other = (Log) obj;
		if (log == null) {
			if (other.log != null)
				return false;
		} else if (!log.equals(other.log))
			return false;
		return true;
	}

	/**
	 * toString
	 */
	@Override
	public synchronized String toString() {
		String name="";
		for(Enumeration<List<Operation>> en=log.elements();
		en.hasMoreElements(); ){
		List<Operation> sublog=en.nextElement();
		for(ListIterator<Operation> en2=sublog.listIterator(); en2.hasNext();){
			name+=en2.next().toString()+"\n";
		}
	}
		
		return name;
	}
}