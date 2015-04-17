import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ClientTest {
	public static void main(String[] args) throws UnknownHostException, IOException
	{
		//newToken();
		//oldToken();
		listOfArticles();
	}

	public static void newToken() throws IOException {
		Socket soc = new Socket("localhost", 12345);
		BufferedWriter write = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
		write.write("NEW_CONTENT\n");
		Date aLittleAfterNow = new Date(new Date().getTime()+3000); //THIS IS OBVIOUSLY INCORRECT
		SimpleDateFormat sdf = new SimpleDateFormat(RequestProcessor.OUR_DATE_FORMAT);

		write.write("TOKENalaska,"+sdf.format(aLittleAfterNow)+"\n");
		write.write("1\n");
		write.flush();

		BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		String line = "";
		while ((line =reader.readLine()) != null)
		{
			System.out.println(line);
		}
	}

	public static void oldToken() throws IOException {
		Socket soc = new Socket("localhost", 12345);
		BufferedWriter write = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
		write.write("PREVIOUS_TOKEN\n");
		Date aLittleAfterNow = new Date(new Date().getTime()+3000); //THIS IS OBVIOUSLY INCORRECT
		SimpleDateFormat sdf = new SimpleDateFormat(RequestProcessor.OUR_DATE_FORMAT);

		write.write("alaska,"+sdf.format(aLittleAfterNow)+"\n");
		write.write("alabama\n");
		write.flush();

		BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		String line = "";
		while ((line =reader.readLine()) != null)
		{
			System.out.println(line);
		}
	}

	public static void listOfArticles() throws IOException {
		Socket soc = new Socket("localhost", 12345);
		BufferedWriter write = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
		write.write("CONTENT_LIST\n");
		write.write("NO_TOKEN\n");
		write.write("*\n");
		write.flush();
		System.out.println("AFTER FLUSH");
		String line = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
		while ((line =reader.readLine()) != null)
		{
			System.out.println(line);
		}

	}
}
