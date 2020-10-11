package com.github.bewaremypower;

import com.github.bewaremypower.config.Config;
import com.github.bewaremypower.util.ExceptionUtil;
import org.apache.bookkeeper.client.BKException;
import org.apache.bookkeeper.client.BookKeeper;
import org.apache.bookkeeper.client.LedgerEntry;
import org.apache.bookkeeper.client.LedgerHandle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;

public class WriteAndRead {
    public static void main(String[] args) throws InterruptedException {
        Config config = Config.newConfig();
        try (BookKeeper bookKeeper = config.newBookKeeper()) {
            try (LedgerHandle ledgerForWrite = config.createLedger(bookKeeper)) {
                System.out.println("Create ledger: " + ledgerForWrite.getId());
                // Read lines from stdin, each line is an entry written to the ledger
                BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Press Ctrl+D to stop input");
                try {
                    while (true) {
                        System.out.print("> ");
                        String line = stdin.readLine();
                        if (line == null) {
                            System.out.println("EOF reached");
                            break;
                        }
                        if (line.isEmpty()) // skip empty entries
                            continue;
                        ledgerForWrite.addEntry(line.getBytes());
                    }
                } catch (IOException e) {
                    ExceptionUtil.handle(e, "Input stopped");
                }

                // Print all confirmed entries
                try (LedgerHandle ledgerForRead = config.openLedger(bookKeeper, ledgerForWrite.getId())) {
                    long lastAddConfirmed = ledgerForRead.getLastAddConfirmed();
                    if (lastAddConfirmed >= 0) {
                        System.out.println("Last add confirmed: " + lastAddConfirmed);
                        System.out.println("Entries of " + ledgerForWrite.getId() + ":");
                        Enumeration<LedgerEntry> entries = ledgerForRead.readEntries(0, lastAddConfirmed);
                        while (entries.hasMoreElements()) {
                            LedgerEntry entry = entries.nextElement();
                            System.out.println("[" + entry.getEntryId() + "]: " + Arrays.toString(entry.getEntry()));
                        }
                    } else {
                        System.out.println("Ledger " + ledgerForRead.getId() + " contains no entries");
                    }
                }
            }
        } catch (IOException | BKException e) {
            e.printStackTrace();
        }
    }
}
