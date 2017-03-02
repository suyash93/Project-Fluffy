/**
 * Copyright 2016 Gash.
 * <p>
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.client;

import com.google.protobuf.ByteString;
import gash.router.message.command.PingMessage;
import gash.router.server.utilities.SupportMessageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.common.Common.Duty;
import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * front-end (proxy) to our service - functional-based
 *
 * @author gash
 */
public class MessageClient {
	private static final Logger logger = LoggerFactory.getLogger(MessageClient.class);
	// track requests
	private long curID = 0;

	public MessageClient(String host, int port) {
		init(host, port);
	}

	private void init(String host, int port) {
		CommConnection.initConnection(host, port);
	}

	public void addListener(CommListener listener) {
		CommConnection.getInstance().addListener(listener);
	}

	// Ping Server
	public void ping() {
		// construct the message to send
		PingMessage pingMessage = new PingMessage(999);
		try {
			// using queue
			CommConnection.getInstance().enqueue(pingMessage.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Save File to server
	public String saveFile(String filename, ByteString bs, int no_of_blocks, int block_no, String requestId) {
		
		logger.info("Printing byte size"+bs.size());
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(999);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		Common.Duty.Builder db = Duty.newBuilder();
		// Prepare the Duty structure
		db.setNumOfBlocks(no_of_blocks); // Number of blocks the file is
		// comprised of
		db.setBlockNo(block_no); // Id of each block;
		db.setDutyType(Common.Duty.DutyType.SAVEFILE); // operation to be
														// performed
		try {
			db.setSender(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		db.setFilename(filename);
		db.setBlockData(bs);
		db.setRequestId(requestId);

		CommandMessage.Builder cb = CommandMessage.newBuilder();

		// Prepare the CommandMessage structure
		cb.setHeader(hb.build());
		cb.setMessage(filename);
		cb.setDuty(db.build());

		// Initiate connection to the server and prepare to save file
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Problem connecting to the system");
		}

		return requestId;

	}

	// Client requests for a file in our storage system
	public void getFile(String filename) throws UnknownHostException {

		Header.Builder hb = Header.newBuilder();
		// prepare the Header Structure
		hb.setNodeId(999);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		Duty.Builder db = Duty.newBuilder();
		// Prepare the Duty structure

		System.out.println("Inside client printing filename" + filename);

		db.setFilename(filename);
		db.setDutyType(Duty.DutyType.GETFILE);
		db.setSender(InetAddress.getLocalHost().getHostAddress());
		db.setRequestId(SupportMessageGenerator.generateRequestID());
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		// Prepare the CommandMessage structure
		cb.setHeader(hb.build());
		cb.setMessage(filename);
		cb.setDuty(db.build());

		// Initiate connection to the server and prepare to read and save file
		try {

			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Update File to server
	public String updateFile(String filename, ByteString bs, int no_of_blocks, int block_no, String requestId) {
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(999);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		Common.Duty.Builder db = Duty.newBuilder();
		// Prepare the Duty structure
		db.setNumOfBlocks(no_of_blocks); // Number of blocks the file is
		// comprised of
		db.setBlockNo(block_no); // Id of each block;
		db.setDutyType(Duty.DutyType.UPDATEFILE); // operation to be performed
		try {
			db.setSender(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		db.setFilename(filename);
		db.setBlockData(bs);
		db.setRequestId(requestId);

		CommandMessage.Builder cb = CommandMessage.newBuilder();

		// Prepare the CommandMessage structure
		cb.setHeader(hb.build());
		cb.setMessage(filename);
		cb.setDuty(db.build());

		// Initiate connection to the server and prepare to save file
		try {
			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Problem connecting to the system");
		}

		return requestId;

	}

	// Client requests to delete a file in our storage system
	public void deleteFile(String filename) throws UnknownHostException {
		Header.Builder hb = Header.newBuilder();
		// prepare the Header Structure
		hb.setNodeId(999);
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		Duty.Builder db = Duty.newBuilder();
		// Prepare the Duty structure

		System.out.println("Inside client printing filename" + filename);

		db.setFilename(filename);
		db.setDutyType(Duty.DutyType.DELETEFILE);
		db.setSender(InetAddress.getLocalHost().getHostAddress());
		db.setRequestId(SupportMessageGenerator.generateRequestID());
		CommandMessage.Builder cb = CommandMessage.newBuilder();
		// Prepare the CommandMessage structure
		cb.setHeader(hb.build());
		cb.setMessage(filename);
		cb.setDuty(db.build());

		// Initiate connection to the server and prepare to read and save file
		try {

			CommConnection.getInstance().enqueue(cb.build());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void release() {
		CommConnection.getInstance().release();
	}

	/**
	 * Since the service/server is asychronous we need a unique ID to associate
	 * our requests with the server's reply
	 *
	 * @return
	 */
	private synchronized long nextId() {
		return ++curID;
	}
}
