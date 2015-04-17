import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Gateway 
{
	public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException
	{
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 3, 0, TimeUnit.SECONDS, workQueue);

		ServerSocket serverSocket = new ServerSocket(12345);
		RequestProcessor.MediaParams params = new RequestProcessor.MediaParams("127.0.0.1", 8081);
		RequestProcessor.DBConGetter connectionGetter = new RequestProcessor.DBConGetter()
		{
			@Override
			public Connection getConnection() throws SQLException
			{
				return DriverManager.getConnection("jdbc:mysql://localhost:3306/test","root","root");
			}
		};
		
		while(true)
		{
			Socket soc = serverSocket.accept();
			System.out.println("Connection received");
			executor.execute(new RequestProcessor(soc,params,connectionGetter));
		}
	}
}
