package example;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;

public class Reporter implements Runnable {

    final LinkedList<Record> records = new LinkedList<Record>();
    final StringBuffer buff = (EchoClient.OUT_FILE == null ? null : new StringBuffer());
    final long startTime = System.currentTimeMillis();

    public void run() {
        if (this.buff != null) {
            this.buff.append(",# ").append(EchoClient.TITLE).append(" #,")
                    .append(EchoClient.THREADS).append(" clients ").append(EchoClient.TIME_LEN / 60 / 1000).append(" minutes\n")
                    .append("second, speed (k/s), ")
                    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime())).append("\n");
        }
        long lastMoment = this.startTime, lastRecv = 0, totalRecv, now;
        for (boolean running = true; running; lastRecv = totalRecv, lastMoment = now) {
            try { Thread.sleep(1000); }
            catch (final InterruptedException ignored) {}
            int toGo = EchoClient.THREADS;
            for (final EchoClient client : EchoClient.clients) {
                final int idx = client.i;
                if (client.exit) {
                    toGo--;
                }
                else if (client.lastCheckIdx == idx) {
                    client.checkedCount++;
                }
                else {
                    client.lastCheckIdx = idx;
                    client.checkedCount = 0;
                }
                if (client.checkedCount >= 10) {
                    System.out.println("!!! Test-Client-" + client.threadId + " seems to be stuck at [round: " + client.i + ", pos: "
                            + client.pos + ", size: " + client.size + "].");
                }
            }
            running = toGo > 0;
            now = System.currentTimeMillis();
            final long timeLen = now - this.startTime;
            final double goingP = ((double) timeLen) / EchoClient.TIME_LEN * 100;
            totalRecv = EchoClient.recvCount.get();
            final long second = timeLen / 1000, period = now - lastMoment;
            final double speed = (totalRecv-lastRecv) / 1024d / period * 1000;
            final String speedStr = String.format("%8.2f", speed);
            if (goingP < 15.0d || goingP > 90.0d || second % 4 == 0) {
                System.out.println(String.format("%6d", toGo) + " clients to go: [" + String.format("%5.1f", goingP) + "%, "
                        + String.format("%4d", second) + "s, " + speedStr + "k/s ]");
            }
            this.records.add(new Record(second, speed));
            if (this.buff != null) {
                this.buff.append(second).append(", ").append(speedStr).append("\n");
            }
        }
        this.doSummery();
    }

    void doSummery() {
        final double avgSpeed = EchoClient.recvCount.get() / 1024d / ((System.currentTimeMillis() - this.startTime) / 1000d);
        double diffSquareSum = 0, sumSpeed = 0;
        int size = 0;
        for (final Record r : this.records) {
            sumSpeed += r.speed;
            size++;
        }
        int i = 0;
        double[] sorted = new double[size];
        final double avg = sumSpeed / size;
        for (final Record r : this.records) {
            final double diff = r.speed - avg;
            diffSquareSum += diff * diff;
            sorted[i++] = r.speed;
        }
        this.records.clear();
        Arrays.sort(sorted);
        final double stdDev = Math.sqrt(diffSquareSum / size);
        final String summery = "STD: " + String.format("%4.2f", stdDev)
                + "(" + String.format("%.3f", stdDev / avg) + ")"
                + ", AVG: " + String.format("%8.2f", avgSpeed) + "k/s, 90th: "
                + String.format("%8.2f", sorted[Math.max((int)(size*0.1)-1, 0)]) + "k/s\n\n";
        System.out.print(summery);
        if (this.buff != null) {
            FileWriter fileOut = null;
            try {
                fileOut = new FileWriter(EchoClient.OUT_FILE, true);
                fileOut.append(this.buff);
                fileOut.append(summery);
                fileOut.flush();
            }
            catch (final IOException ex) {
                ex.printStackTrace();
            }
            finally {
                if (fileOut != null) {
                    try { fileOut.close(); }
                    catch (final IOException ignored) {}
                }
            }
        }
    }

}

class Record {
    final long second;
    final double speed;
    Record(final long second, final double speed) {
        this.second = second;
        this.speed = speed;
    }
}