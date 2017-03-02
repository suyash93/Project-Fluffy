package gash.router.inmemory;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class RiakClientHandler {

    private static final Logger logger = LoggerFactory.getLogger(RiakClientHandler.class);
    private RiakCluster cluster;
    protected static AtomicReference<RiakClientHandler> instance = new AtomicReference<RiakClientHandler>();

    public static RiakClientHandler init() {
        instance.compareAndSet(null, new RiakClientHandler());
        return instance.get();
    }

    public static RiakClientHandler getInstance() throws NullPointerException {
        return instance.get();
    }

    private RiakClientHandler() {
        RiakNode node = new RiakNode.Builder()
                .withRemoteAddress("127.0.0.1")
                .withRemotePort(8087)
                .build();
        // This cluster object takes our one node as an argument
        cluster = new RiakCluster.Builder(node)
                .build();

        // The cluster must be started to work, otherwise you will see errors
        cluster.start();
    }


    private static class RiakFile {

        public String filename;
        public byte[] byteData;
    }

    public void saveFile(String filename, byte[] byteData) {
        try {
            System.out.println("Printing file name in riak " + filename);
            System.out.println("Inside Riak handler");
            RiakClient client = new RiakClient(cluster);
            RiakFile newFile = createRiakFile(filename, byteData);
            System.out.println("Riak file created");
            Namespace fileBucket = new Namespace("files");
            Location fileLocation = new Location(fileBucket, filename);
            StoreValue storeFile = new StoreValue.Builder(newFile).withLocation(fileLocation).build();
            client.execute(storeFile);
            System.out.println("File saved to riak ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isFilePresent(String fileName) {
        try {
            RiakClient client = new RiakClient(cluster);
            Namespace fileBucket = new Namespace("files");
            Location fileLocation = new Location(fileBucket, fileName);
            FetchValue fetchFile = new FetchValue.Builder(fileLocation).build();
            client.execute(fetchFile).getValue(RiakFile.class);
            return true;
        } catch (Exception e) {
            logger.info("File not found in memory");
            return false;
        }
    }

    public byte[] getFile(String fileName) throws Exception {
        System.out.println("Fetching file " + fileName);
        RiakClient client = new RiakClient(cluster);
        Namespace fileBucket = new Namespace("files");
        Location fileLocation = new Location(fileBucket, fileName);
        FetchValue fetchFile = new FetchValue.Builder(fileLocation).build();
        RiakFile fetchedFile = client.execute(fetchFile).getValue(RiakFile.class);
        System.out.println(fetchedFile.filename);
        System.out.println("Printing file data in bytes" + fetchedFile.byteData);
        return fetchedFile.byteData;
    }

    private RiakFile createRiakFile(String filename, byte[] byteData) {
        RiakFile file = new RiakFile();
        file.filename = filename;
        file.byteData = byteData;
        return file;

    }

    public void deleteFile(String fileName) throws Exception {
        logger.info("Deleting an existing file " + fileName);
        RiakClient client = new RiakClient(cluster);
        Namespace fileBucket = new Namespace("files");
        Location fileLocation = new Location(fileBucket, fileName);
        DeleteValue delete = new DeleteValue.Builder(fileLocation).build();
        client.execute(delete);
    }

}
