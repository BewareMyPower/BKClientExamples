package com.github.bewaremypower;

import com.github.bewaremypower.config.Config;
import com.github.bewaremypower.util.ExceptionUtil;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class DeleteLedgers {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Arguments must be: MinLegerId MaxLedgerId");
            return;
        }

        Config config = Config.newConfig();
        final long minLedgerId = Long.parseLong(args[0]);
        final long maxLedgerId = Long.parseLong(args[1]);

        if (minLedgerId > maxLedgerId) {
            System.err.printf("minLedgerId (%d) can't be larger than maxLedgerId (%d)", minLedgerId, maxLedgerId);
            return;
        }

        CountDownLatch deleteCallbackDone = new CountDownLatch((int) (maxLedgerId - minLedgerId + 1));
        try (BookKeeper bookKeeper = config.newBookKeeper()) {
            for (long ledgerId = minLedgerId; ledgerId <= maxLedgerId; ledgerId++) {
                bookKeeper.asyncDeleteLedger(ledgerId, (i, o) -> {
                    long id = (long) o;
                    if (i < 0)
                        ExceptionUtil.handle(BKException.create(i), "failed to delete ledger");
                    else
                        System.out.println("success to delete ledger: " + id);
                    deleteCallbackDone.countDown();
                }, ledgerId);
            }
            deleteCallbackDone.await();
        } catch (InterruptedException | IOException | BKException e) {
            ExceptionUtil.handle(e);
        }
    }
}
