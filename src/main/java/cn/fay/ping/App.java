package cn.fay.ping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author fay  fay9395@gmail.com
 * @date 2018/9/30 下午6:41.
 */
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger("ping");

    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(App.class.getClassLoader().getResourceAsStream("url.txt")));
        ExecutorService executorService = Executors.newCachedThreadPool();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final String data = line;
                executorService.submit(new Runnable() {
                    public void run() {
                        String url = data.split(":")[0];
                        String name = url.split("\\.")[0];
                        name = name.contains("-") ? name.split("-")[0] : name;
                        int port = Integer.parseInt(data.split(":")[1]);
                        while (true) {
                            try {
                                long start = System.currentTimeMillis();
                                new Socket(url, port).close();
                                LOGGER.info("name:{} url:{} cast:{}", name, url, System.currentTimeMillis() - start);
                            } catch (IOException e) {
                                if (e instanceof ConnectException) {
                                    LOGGER.info("name:{} url:{} cast:{} msg:{}", name, url, 999, e.getMessage());
                                } else if (e instanceof UnknownHostException) {
                                    LOGGER.info("name:{} url:{} cast:{} msg:{}", name, url, 999, "unknown host");
                                } else {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                Thread.sleep(1000); // 1s
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } finally {
            reader.close();
        }
    }
}
