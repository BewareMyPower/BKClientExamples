package com.github.bewaremypower.config;

import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.LedgerHandle;
import org.apache.bookkeeper.conf.ClientConfiguration;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Config of BookKeeper/Ledger
 */
public class Config {
    public static final String DEFAULT_PROPERTIES_FILE = "bkclientexample.properties";
    public static final String DEFAULT_ZK_SERVERS = "localhost:2181";
    public static final String DEFAULT_ZK_TIMEOUT_MS = "3000";
    public static final String DEFAULT_ZK_LEDGERS_ROOT_PATH = "/ledgers";
    public static final String DEFAULT_ENS_SIZE = "1";
    public static final String DEFAULT_WRITE_QUORUM_SIZE = "1";
    public static final String DEFAULT_ACK_QUORUM_SIZE = "1";

    private final String zkServers;
    private final int zkTimeoutMs;
    private final String zkLedgersRootPath;
    private final int ensSize;
    private final int writeQuorumSize;
    private final int ackQuorumSize;

    // default digest type when a ledger's created or opened
    public final BookKeeper.DigestType DIGEST_TYPE = BookKeeper.DigestType.CRC32;

    // password when a ledger's created or opened
    public final byte[] PASSWORD = "".getBytes();

    private Config(Properties properties) {
        zkServers = properties.getProperty("zkServers", DEFAULT_ZK_SERVERS);
        zkTimeoutMs = Integer.parseInt(properties.getProperty("zkTimeoutMs", DEFAULT_ZK_TIMEOUT_MS));
        zkLedgersRootPath = properties.getProperty("zkLedgersRootPath", DEFAULT_ZK_LEDGERS_ROOT_PATH);
        ensSize = Integer.parseInt(properties.getProperty("ensSize", DEFAULT_ENS_SIZE));
        writeQuorumSize = Integer.parseInt(properties.getProperty("writeQuorumSize", DEFAULT_WRITE_QUORUM_SIZE));
        ackQuorumSize = Integer.parseInt(properties.getProperty("ackQuorumSize", DEFAULT_ACK_QUORUM_SIZE));
        System.out.println("-------------------- Config --------------------");
        System.out.println("zkServers:         " + zkServers);
        System.out.println("zkTimeoutMs:       " + zkTimeoutMs);
        System.out.println("zkLedgersRootPath: " + zkLedgersRootPath);
        System.out.println("ensSize:           " + ensSize);
        System.out.println("writeQuorumSize:   " + writeQuorumSize);
        System.out.println("ackQuorumSize:     " + ackQuorumSize);
    }

    /**
     * @return a `Config` initialized from a property file
     */
    public static Config newConfig() throws InvalidConfigError {
        String configPath = System.getProperty("bkClientExamplesConfig");
        Properties properties = new Properties();
        try {
            if (configPath != null) {
                properties.load(new FileReader(configPath));
                System.out.println("Load config from " + configPath);
            } else {
                properties.load(Config.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE));
                System.out.println("Load config from " + DEFAULT_PROPERTIES_FILE + " in classpath");
            }
        } catch (IOException e) {
            throw new InvalidConfigError(e.getMessage());
        }
        return new Config(properties);
    }

    /**
     * Creates a `BookKeeper` with the default ZooKeeper servers and timeout
     *
     * @return the new `BookKeeper`
     * @throws BKException
     * @throws InterruptedException
     * @throws IOException
     */
    public BookKeeper newBookKeeper() throws BKException, InterruptedException, IOException {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .setZkTimeout(zkTimeoutMs)
                .setMetadataServiceUri("zk+null://" + zkServers + zkLedgersRootPath);
        return new BookKeeper(clientConfiguration);
    }

    /**
     * Creates a ledger, with the default digest type and password
     *
     * @param bookKeeper the `BookKeeper` of ledgers
     * @return a handle to the newly ledger
     * @throws BKException
     * @throws InterruptedException
     */
    public LedgerHandle createLedger(BookKeeper bookKeeper) throws BKException, InterruptedException {
        return bookKeeper.createLedger(ensSize, writeQuorumSize, ackQuorumSize, DIGEST_TYPE, PASSWORD);
    }

    /**
     * Opens a ledger
     *
     * @param bookKeeper the `BookKeeper` of ledgers
     * @param ledgerId   id of the ledger to open
     * @return a handle to the newly ledger
     * @throws BKException
     * @throws InterruptedException
     */
    public LedgerHandle openLedger(BookKeeper bookKeeper, long ledgerId) throws BKException, InterruptedException {
        return bookKeeper.openLedger(ledgerId, DIGEST_TYPE, PASSWORD);
    }
}
