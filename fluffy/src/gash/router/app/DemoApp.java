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
package gash.router.app;

import com.google.protobuf.ByteString;
import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import gash.router.server.utilities.SupportMessageGenerator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe;
import routing.Pipe.CommandMessage;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DemoApp extends Thread implements CommListener {
    private MessageClient mc;
    private Thread t1;
    private Thread t2;
    private CommandMessage msg;
    private boolean firstHalfValue = false;
    private boolean secondHalfValue = false;
    private ArrayList<ByteString> firstHalfChunkList = new ArrayList<ByteString>();
    private ArrayList<ByteString> secondHalfChunkList = new ArrayList<ByteString>();
    private Map<String, ArrayList<CommandMessage>> fileBlocksList = new HashMap<String, ArrayList<CommandMessage>>();
    private DemoApp da;
    protected static Logger logger = LoggerFactory.getLogger(DemoApp.class);


    public DemoApp(MessageClient mc) {
        init(mc);
    }

    private void init(MessageClient mc) {
        this.mc = mc;
        this.mc.addListener(this);
    }

    private void ping(int N) {
        // test round-trip overhead (note overhead for initial connection)
        final int maxN = 10;
        long[] dt = new long[N];
        long st = System.currentTimeMillis(), ft = 0;
        for (int n = 0; n < N; n++) {
            mc.ping();
            ft = System.currentTimeMillis();
            dt[n] = ft - st;
            st = ft;
        }

        System.out.println("Round-trip ping times (msec)");
        for (int n = 0; n < N; n++)
            System.out.print(dt[n] + " ");
        System.out.println("");
    }

    private ArrayList<ByteString> divideFileChunks(File file) throws IOException {
        ArrayList<ByteString> chunkedFile = new ArrayList<ByteString>();
        int sizeOfFiles = 1024 * 1024; // equivalent to 1 Megabyte
        byte[] buffer = new byte[sizeOfFiles];

        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            int tmp = 0;
            while ((tmp = bis.read(buffer)) > 0) {
                ByteString byteString = ByteString.copyFrom(buffer, 0, tmp);
                chunkedFile.add(byteString);
            }
            return chunkedFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getListenerID() {
        return "demo";
    }

    @Override
    public void onMessage(CommandMessage msg) {

        System.out.println("The file has been arrived in bytes");
        this.msg = msg;

        logger.info("Printing msg from server" + msg.getDuty().getBlockNo());

        if (msg.hasDuty()) {
            if (!fileBlocksList.containsKey(msg.getDuty().getFilename())) {
                fileBlocksList.put(msg.getDuty().getFilename(), new ArrayList<Pipe.CommandMessage>());
                System.out.println("Chunk list created ");
            }
            fileBlocksList.get(msg.getDuty().getFilename()).add(msg);

            if (fileBlocksList.get(msg.getDuty().getFilename()).size() == msg.getDuty().getNumOfBlocks()) {
                try {

                    File file = new File(msg.getDuty().getFilename());
                    file.createNewFile();
                    List<ByteString> byteString = new ArrayList<ByteString>();
                    FileOutputStream outputStream = new FileOutputStream(file);
                    int i = 1;
                    while (i <= msg.getDuty().getNumOfBlocks()) {
                        for (int j = 0; j < fileBlocksList.get(msg.getDuty().getFilename()).size(); j++) {
                            System.out.println("Inside the for loop ");
                            if (fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty().getBlockNo() == i) {
                                System.out.println("Added chunk to file " + i);
                                byteString.add(fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty().getBlockData());
                                System.out.println(fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty().getBlockData().size());
                                i++;
                                break;
                            }
                        }
                    }
                    ByteString bs = ByteString.copyFrom(byteString);
                    outputStream.write(bs.toByteArray());
                    outputStream.flush();
                    outputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            System.out.flush();
        }
        if (msg.hasLeaderStatus()) {
            System.out.println("Server is not a leader");
        }
        if (msg.hasMessage()) {
            System.out.println(msg.getMessage());
        }

    }

    public void readFileFromBlocks(Map<String, ArrayList<CommandMessage>> fileBlocksList, CommandMessage msg,
                                   int startBlockNo, int endBlockNo, String threadName) {
        if (fileBlocksList.get(msg.getDuty().getFilename()).size() == msg.getDuty().getNumOfBlocks()) {

            try {
                File file = new File(msg.getDuty().getFilename());
                file.createNewFile();
                ArrayList<ByteString> byteString = new ArrayList<ByteString>();
                HashMap<String, ArrayList<ByteString>> partitionedMap = new HashMap<String, ArrayList<ByteString>>();
                FileOutputStream fOutputStream = new FileOutputStream(file);
                int i = startBlockNo;
                while (i <= endBlockNo) {
                    for (int j = 0; j < fileBlocksList.get(msg.getDuty().getFilename()).size(); j++) {

                        if (fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty().getBlockNo() == i) {
                            System.out.println("Written 1 MB chunk to file " + i);
                            byteString.add(
                                    fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty().getBlockData());
                            System.out.println(fileBlocksList.get(msg.getDuty().getFilename()).get(j).getDuty()
                                    .getBlockData().size());
                            // outputStream.write(fileBlocksList.get(msg.getTask().getFilename()).get(j).getTask().getChunk().toByteArray());

                            i++;
                            break;
                        }
                    }

                }

                partitionedMap.put(threadName, byteString);
                rearrangeFileBlocks(partitionedMap, file);

                ByteString bs = ByteString.copyFrom(byteString);
                fOutputStream.write(bs.toByteArray());
                fOutputStream.flush();
                fOutputStream.close();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    /***
     * Two parts from two different concurrent threads we receive a file blocks
     * This constitues a parallel approach for file read
     */

    public void rearrangeFileBlocks(HashMap<String, ArrayList<ByteString>> partitionedMap, File file) {
        try {

            FileOutputStream fOutputStream = new FileOutputStream(file);

            while (firstHalfValue == true && secondHalfValue == true) {
                for (String key : partitionedMap.keySet()) {
                    if (key == "firstHalf") {
                        this.firstHalfValue = true;
                        firstHalfChunkList = partitionedMap.get("firstHalf");
                    } else {
                        this.secondHalfValue = true;
                        partitionedMap.get("secondHalf");
                        secondHalfChunkList = partitionedMap.get("secondHalf");
                    }
                }
                ;
            }

            ArrayList<ByteString> completeChunkList = new ArrayList<ByteString>();
            completeChunkList.addAll(firstHalfChunkList);
            completeChunkList.addAll(secondHalfChunkList);
            ByteString bs = ByteString.copyFrom(completeChunkList);

            // File has been created after ordering the two seperate halves/

            fOutputStream.write(bs.toByteArray());
            fOutputStream.flush();
            fOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.flush();

    }

    /**
     * A holder for threads created from main method invocation firstHalf and
     * secondHalf resp...
     ***/
    public void threadHolder() {
        t1.start();
        t2.start();
    }


    /**
     * Get the context of the thread from which it is called
     ***/
    public void run() {
        Thread currentThread = Thread.currentThread();
        int startBlockNo;
        int endBlockNo;

        /***
         * Split the file read into two halves to be processed by two different
         * concurrent processes
         */
        if (currentThread.getName() == "firstHalf") {
            startBlockNo = 0;
            endBlockNo = (msg.getDuty().getNumOfBlocks() / 2) - 1;
            System.out.println("Thread: " + currentThread.getName() + "has blocks starting:" + startBlockNo + "ending blocking as :" + endBlockNo);

            readFileFromBlocks(fileBlocksList, msg, startBlockNo, endBlockNo, currentThread.getName());
        } else {
            startBlockNo = (msg.getDuty().getNumOfBlocks() / 2);
            endBlockNo = msg.getDuty().getNumOfBlocks() - 1;
            System.out.println("Thread: " + currentThread.getName() + "has blocks starting:" + startBlockNo + "ending blocking as :" + endBlockNo);


            readFileFromBlocks(fileBlocksList, msg, startBlockNo, endBlockNo, currentThread.getName());

        }

    }

    public void sendReadTasks(String filename) {
        try {
            mc.getFile(filename);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void sendDeleteTasks(String fileName) {
        try {
            mc.deleteFile(fileName);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * sample application (client) use of our messaging service
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("usage:  <ip address> <port no>");
            System.exit(1);
        }
        String ipAddress = args[0];
        int port = Integer.parseInt(args[1]);
        Scanner s = new Scanner(System.in);
        boolean isExit = false;
        try {
            MessageClient mc = new MessageClient(ipAddress, port);
            DemoApp da = new DemoApp(mc);
            int choice = 0;

            while (true) {
                System.out.println("Enter your option \n1. WRITE a file. \n2. READ a file. \n3. Update a File. \n4. Delete a File\n 5 Ping(Global)\n 6 Exit");
                choice = s.nextInt();
                switch (choice) {
                    case 1: {
                        System.out.println("Enter the full pathname of the file to be written ");
                        String currFileName = s.next();
                        File file = new File(currFileName);
                        if (file.exists()) {
                            ArrayList<ByteString> chunkedFile = da.divideFileChunks(file);
                            String name = file.getName();
                            int i = 0;
                            String requestId = SupportMessageGenerator.generateRequestID();
                            for (ByteString string : chunkedFile) {
                                mc.saveFile(name, string, chunkedFile.size(), i++, requestId);
                            }
                        } else {
                            throw new FileNotFoundException("File does not exist in this path ");
                        }
                    }
                    break;
                    case 2: {
                        System.out.println("Enter the file name to be read : ");
                        String currFileName = s.next();
                        da.sendReadTasks(currFileName);
                        //Thread.sleep(1000 * 100);
                    }
                    break;
                    case 3: {
                        System.out.println("Enter the full pathname of the file to be updated");
                        String currFileName = s.next();
                        File file = new File(currFileName);
                        if (file.exists()) {
                            ArrayList<ByteString> chunkedFile = da.divideFileChunks(file);
                            String name = file.getName();
                            int i = 0;
                            String requestId = SupportMessageGenerator.generateRequestID();
                            for (ByteString string : chunkedFile) {
                                mc.updateFile(name, string, chunkedFile.size(), i++, requestId);
                            }
                            //Thread.sleep(10 * 1000);
                        } else {
                            throw new FileNotFoundException("File does not exist in this path ");
                        }
                    }
                    break;

                    case 4:
                        System.out.println("Enter the file name to be deleted : ");
                        String currFileName = s.next();
                        mc.deleteFile(currFileName);
                        //Thread.sleep(1000 * 100);
                        break;
                    case 5:
                        da.ping(1);
                        break;
                    case 6:
                        isExit = true;
                        break;
                    default:
                        break;
                }
                if (isExit)
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CommConnection.getInstance().release();
            if (s != null)
                s.close();
        }
    }

}