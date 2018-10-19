package cn.fay.ping;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author fay  fay9395@gmail.com
 * @date 2018/9/30 下午6:41.
 */
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger("ping");
    private static Connection connection = null;

    public static void main(String[] args) throws Exception {
        connection = getConnection();
        if (connection == null) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(App.class.getClassLoader().getResourceAsStream("url.txt")));
        ExecutorService executorService = Executors.newCachedThreadPool();
        String line;
        boolean needClean = false;
        try {
            while ((line = reader.readLine()) != null) {
                final String data = line;
                needClean = true;
                executorService.submit(new Runnable() {
                    public void run() {
                        String url = data.split(":")[0];
                        String name = url.split("\\.")[0];
                        name = name.contains("-") ? name.split("-")[0] : name;
                        int port = Integer.parseInt(data.split(":")[1]);
                        while (true) {
                            int delay = 999;
                            String msg = "";
                            try {
                                long start = System.currentTimeMillis();
                                new Socket(url, port).close();
                                delay = (int) (System.currentTimeMillis() - start);
                            } catch (IOException e) {
                                if (e instanceof ConnectException) {
                                    msg = e.getMessage();
                                } else if (e instanceof UnknownHostException) {
                                    msg = "unknown host";
                                } else {
                                    msg = e.getMessage();
                                }
                            }
                            putData(name, url, port, delay, msg);
                            LOGGER.info("name:{} url:{} cast:{} msg:{}", name, url, delay, msg);
                            try {
                                Thread.sleep(1000); // 1s
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            if (needClean) {
                final Connection connectionForDelete = getConnection();
                Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
                    public void run() {
                        String sql = "delete from delay where time < ? - interval 1 hour";
                        try {
                            PreparedStatement preparedStatement = connectionForDelete.prepareStatement(sql);
                            preparedStatement.setObject(1, new Date());
                            preparedStatement.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1L, 3600L, TimeUnit.SECONDS);
            }
        } finally {
            reader.close();
        }
    }

    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            return null;
        }
        return DriverManager.getConnection("jdbc:mysql://106.12.84.24:3306/vpn?autoReconnect=true", "root", null);
    }

    private static void putData(String name, String url, int port, int delay, String msg) {
        String sql = "insert into delay(time, name, url, port, delay, msg) values(?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = null;
        try {
            int i = 1;
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setObject(i++, new Date());
            preparedStatement.setString(i++, name);
            preparedStatement.setString(i++, url);
            preparedStatement.setInt(i++, port);
            preparedStatement.setInt(i++, delay);
            preparedStatement.setString(i++, msg);
            preparedStatement.execute();
        } catch (SQLException e) {
            if (e instanceof MySQLNonTransientConnectionException) {
                try {
                    connection = getConnection();
                    if (connection != null) {
                        putData(name, url, port, delay, msg);
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
