package com.github.bewaremypower;

import com.github.bewaremypower.config.Config;
import com.github.bewaremypower.util.ExceptionUtil;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.LedgerEntry;
import org.apache.bookkeeper.client.LedgerHandle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadRead {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            System.err.println("Arguments must be: LedgerId [ThreadNum=8]");
            return;
        }

        Config config = Config.newConfig();
        final long ledgerId = Long.parseLong(args[0]);
        if (ledgerId < 0) {
            System.err.println("LedgerId (" + ledgerId + ") must be non-negative");
            return;
        }
        final int threadNum = (args.length > 1) ? Integer.parseInt(args[1]) : 8;
        if (threadNum <= 0) {
            System.err.println("ThreadNum (" + threadNum + ") must be positive");
            return;
        }
        System.out.println("LedgerId: " + ledgerId + " ThreadNum: " + threadNum);

        try (BookKeeper bookKeeper = config.newBookKeeper()) {
            final ExecutorService threadPool = Executors.newFixedThreadPool(threadNum);
            final CountDownLatch threadsDone = new CountDownLatch(threadNum);

            for (int i = 0; i < threadNum; i++) {
                final String threadName = "Thread " + i;
                threadPool.execute(() -> {
                    try (LedgerHandle ledgerHandle = config.openLedger(bookKeeper, ledgerId)) {
                        Enumeration<LedgerEntry> entries = ledgerHandle.readEntries(0, ledgerHandle.getLastAddConfirmed());
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        while (entries.hasMoreElements()) {
                            LedgerEntry entry = entries.nextElement();
                            try {
                                stream.write(entry.getEntry());
                            } catch (IOException e) {
                                ExceptionUtil.handle(e, "[WARN] " + threadName);
                            }
                        }
                        System.out.println(threadName + ": " + Arrays.toString(stream.toByteArray()));
                    } catch (BKException | InterruptedException e) {
                        ExceptionUtil.handle(e, threadName);
                    } finally {
                        threadsDone.countDown();
                    }
                });
            }

            threadsDone.await();
            threadPool.shutdown();
        } catch (IOException | BKException e) {
            ExceptionUtil.handle(e);
        }
    }
}
