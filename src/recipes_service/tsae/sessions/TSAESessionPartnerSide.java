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

package recipes_service.tsae.sessions;


import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import communication.ObjectInputStream_DS;
import communication.ObjectOutputStream_DS;
import recipes_service.ServerData;
import recipes_service.communication.Message;
import recipes_service.communication.MessageAErequest;
import recipes_service.communication.MessageEndTSAE;
import recipes_service.communication.MessageOperation;
import recipes_service.communication.MsgType;
import recipes_service.data.AddOperation;
import recipes_service.data.Operation;
import recipes_service.data.OperationType;
import recipes_service.tsae.data_structures.TimestampMatrix;
import recipes_service.tsae.data_structures.TimestampVector;

//LSim logging system imports sgeag@2017
import lsim.worker.LSimWorker;
import edu.uoc.dpcs.lsim.LSimFactory;
import edu.uoc.dpcs.lsim.logger.LoggerManager.Level;

/**
 * @author Joan-Manuel Marques
 * December 2012
 *
 */
public class TSAESessionPartnerSide extends Thread{
	// Needed for the logging system sgeag@2017
	private LSimWorker lsim = LSimFactory.getWorkerInstance();
	
	private Socket socket = null;
	private ServerData serverData = null;
	
	public TSAESessionPartnerSide(Socket socket, ServerData serverData) {
		super("TSAEPartnerSideThread");
		this.socket = socket;
		this.serverData = serverData;
	}

	public void run() {

		Message msg = null;

		int current_session_number = -1;
		try {
			ObjectOutputStream_DS out = new ObjectOutputStream_DS(socket.getOutputStream());
			ObjectInputStream_DS in = new ObjectInputStream_DS(socket.getInputStream());

			// receive originator's summary and ack
			msg = (Message) in.readObject();
			current_session_number = msg.getSessionNumber();
			lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] TSAE session");
			lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
			if (msg.type() == MsgType.AE_REQUEST){
				
				// Cast the message to a MessageAERequest as it is just validated it is from this type
				MessageAErequest msgAeRequest = (MessageAErequest) msg;
				
				TimestampVector localSummary = null;
				TimestampMatrix localAck = null;
				
				// Get the current values for localSummary and localAck
				// It is synchronized in order to prevent the localSummary change meanwhile is used for the ack
				synchronized (serverData)
				{
					localSummary = serverData.getSummary().clone();
					
					//Update the localAck with the currentSummary
					serverData.getAck().update(serverData.getId(), localSummary);
					
					localAck = serverData.getAck().clone();
				}				
								
				// Get the list of operations that are not in the parter summary
				List<Operation> operationsToSend = serverData.getLog().listNewer(msgAeRequest.getSummary());
				
	            // For each operation that is not in the originator, send it
				for (Operation operation: operationsToSend)
				{
					out.writeObject(new MessageOperation(operation));
				}			
				
				// send to originator: local's summary and ack
				msg = new MessageAErequest(localSummary, localAck);
				msg.setSessionNumber(current_session_number);
	 	        out.writeObject(msg);
				lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] sent message: "+ msg);
								

				// Read the operations received
				List<MessageOperation> operationsFromOriginator = new Vector<MessageOperation>();
				msg = (Message) in.readObject();
				lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
				while (msg.type() == MsgType.OPERATION){
					// Add the operation read to a memory list
					operationsFromOriginator.add((MessageOperation)msg);
					
					msg = (Message) in.readObject();
					lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] received message: "+ msg);
				}
				
				// receive message to inform about the ending of the TSAE session
				if (msg.type() == MsgType.END_TSAE){
					// send and "end of TSAE session" message
					msg = new MessageEndTSAE();
					msg.setSessionNumber(current_session_number);
		            out.writeObject(msg);					
					lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] sent message: "+ msg);
					
					// At this point the TSAE session has ended properly
					// Process the operations from the Partner
					// It is synchronized in order to prevent the localSummary change meanwhile 
					// is used for the ack in a distributed system
					synchronized (serverData)
					{
						for (MessageOperation operation: operationsFromOriginator)
						{
							if (operation.getOperation().getType()== OperationType.ADD)
							{
								serverData.processOperation((AddOperation)operation.getOperation());
							}							
						}
						
						// Update the Summary and Ack After processing the operations 
						this.serverData.getSummary().updateMax(msgAeRequest.getSummary());
						this.serverData.getAck().updateMax(msgAeRequest.getAck());
						
						// Purge the log with the operations processed by all nodes
						this.serverData.getLog().purgeLog(serverData.getAck());
					}
				}
				
			}
			socket.close();		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			lsim.log(Level.FATAL, "[TSAESessionPartnerSide] [session: "+current_session_number+"]" + e.getMessage());
			e.printStackTrace();
            System.exit(1);
		}catch (IOException e) {
	    }
		
		lsim.log(Level.TRACE, "[TSAESessionPartnerSide] [session: "+current_session_number+"] End TSAE session");
	}
}