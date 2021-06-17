import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

class WebFrame extends JFrame {

    private final JButton single, concurrent, stop;
    private final JLabel completed, elapsed, running;

    private JTable table;
    private DefaultTableModel tableModel;

    private final JTextField numThreads;
    private final JProgressBar progress;

    private long beginTime;

    private Starter start = null;
    private static final String LINKS = "C:\\Users\\Admin\\Desktop\\oop\\assignment 4\\links.txt";

    public void tableInit(){
        String[] sections = new String[]{"url", "status"};
        tableModel = new DefaultTableModel(sections, 0);
        try {
            BufferedReader br = new BufferedReader(new FileReader(LINKS));
            while (true) {
                String str = br.readLine();
                if (str == null) break;
                tableModel.addRow(new String[]{str, ""});
            }
            table = new JTable(tableModel);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WebFrame() {
        super("WebLoader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel;

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        tableInit();

        running = new JLabel("Running:");
        completed = new JLabel("Completed:");
        elapsed = new JLabel("Elapsed:");

        stop = new JButton("Stop");
        single = new JButton("Single Thread Fetch");
        concurrent = new JButton("Concurrent Fetch");

        numThreads = new JTextField("4", 4);
        numThreads.setMaximumSize(numThreads.getPreferredSize());

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(500, 250));

        progress = new JProgressBar(0, tableModel.getRowCount());
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        addActionListeners();
        panel.add(scrollPane);
        panel.add(single);
        panel.add(concurrent);
        panel.add(numThreads);
        panel.add(running);
        panel.add(completed);
        panel.add(elapsed);
        panel.add(progress);
        panel.add(stop);
        add(panel);
        pack();
        setVisible(true);
    }

    private void changeButtons(){
        SwingUtilities.invokeLater(() -> {
            single.setEnabled(false);
            concurrent.setEnabled(false);
            stop.setEnabled(true);
            progress.setValue(0);
        });
    }

    private void addActionListeners() {
        single.addActionListener(e -> {
            if (start != null){
                start.interrupt();
            }
            start(1);
            beginTime = System.currentTimeMillis();
            changeButtons();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt("", i, 1);
            }
        });

        concurrent.addActionListener(e -> {
            if (start != null){
                start.interrupt();
            }
            beginTime = System.currentTimeMillis();
            start(Integer.parseInt(numThreads.getText()));

            changeButtons();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt("", i, 1);
            }
        });

        stop.addActionListener(e -> {
            if (start != null){
                start.interrupt();
            }
            start = null;
            SwingUtilities.invokeLater(() -> {
                single.setEnabled(true);
                concurrent.setEnabled(true);
                stop.setEnabled(false);
            });
        });
    }

    private void start(int threadsNumber) {
        start = new Starter(threadsNumber, tableModel.getRowCount(), this);
        start.start();
    }

    public String getValue(int row) {
        return (String) tableModel.getValueAt(row, 0);
    }

    public void setResult(final String data, final int row) {
        tableModel.setValueAt(data, row, 1);
    }

    private AtomicInteger runners, completes;
    private Semaphore counter;

    public void displayStatus(String result, int row) {
        setResult(result, row);
        completes.incrementAndGet();
        runners.decrementAndGet();
        counter.release();
        double elapsedTime = (System.currentTimeMillis() - beginTime)/1000.0;

        SwingUtilities.invokeLater(() -> {
            progress.setValue(completes.get());
            running.setText("Running:" + runners);
            completed.setText("Complete:" + completes);
            elapsed.setText("Elapsed:" + elapsedTime);
        });
    }


    public class Starter extends Thread {

        private final int count;

        private final WebFrame frame;

        private final HashSet<WebWorker> threads = new HashSet<>();

        public Starter(int numOfThreads, int count, WebFrame frame) {
            this.count = count;
            this.frame = frame;

            counter = new Semaphore(numOfThreads);
            runners = new AtomicInteger(0);
            completes = new AtomicInteger(0);
        }

        public void run() {
            try {
                for (int i = 0; i < count; i++) {
                    WebWorker worker = new WebWorker(i,getValue(i), frame);
                    threads.add(worker);
                }
                for (WebWorker w : threads) {
                    counter.acquire();
                    runners.incrementAndGet();
                    w.start();
                }

                for(WebWorker w: threads){
                    w.join();
                }
            } catch (InterruptedException e) {
                for (WebWorker w : threads){
                    w.interrupt();
                }
            }

            SwingUtilities.invokeLater(() -> {
                single.setEnabled(true);
                concurrent.setEnabled(true);
                stop.setEnabled(false);
            });

        }
    }

    public static void main(String[] args){
        new WebFrame();
    }

}

public class WebWorker extends Thread {
    private final String link;
    private String status;
    private long begin;
    private final int row;
    private final WebFrame frame;

    public WebWorker(int row, String link, WebFrame frame){
        this.frame = frame;
        this.link = link;
        this.row = row;
    }

    @Override
    public void run(){
        begin = System.currentTimeMillis();
        download();
        frame.displayStatus(status, row);
    }
    //  This is the core web/download i/o code...
    public void download() {
        int bytes = 0;
        InputStream input = null;
        StringBuilder contents = null;
        try {
            URL url = new URL(link);
            URLConnection connection = url.openConnection();

            // Set connect() to throw an IOException
            // if connection does not succeed in this many msecs.
            connection.setConnectTimeout(5000);

            connection.connect();
            input = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            char[] array = new char[1000];
            int len;
            contents = new StringBuilder(1000);
            while ((len = reader.read(array, 0, array.length)) > 0) {
                if (isInterrupted()){
                    throw new InterruptedException();
                }
                bytes += len;
                contents.append(array, 0, len);
                Thread.sleep(100);
            }
            long end = System.currentTimeMillis();
            // Successful download if we get here
            String date = new SimpleDateFormat("HH:mm:ss").format(new Date(begin));
            status = date + "   " + (end - begin) + "ms " + bytes + " bytes";
        }
        // Otherwise control jumps to a catch...
        catch (MalformedURLException ignored) {
            status = "err";
        } catch (InterruptedException exception) {
            status = "interrupted";
        } catch (IOException ignored) {
            status = "err";
        }
        // "finally" clause, to close the input stream
        // in any case
        finally {
            try {
                if (input != null) input.close();
            } catch (IOException ignored) {
            }
        }
    }
}
