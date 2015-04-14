import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ClientTest {
	public static void main(String[] args) throws UnknownHostException, IOException
	{
		//newToken();
		oldToken();
	}

	public static void newToken() throws IOException {
		Socket soc = new Socket("localhost", 12345);
		BufferedWriter write = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
		write.write("NEW_CONTENT\n");
		Date aLittleAfterNow = new Date(new Date().getTime()+3000); //THIS IS OBVIOUSLY INCORRECT
		SimpleDateFormat sdf = new SimpleDateFormat(RequestProcessor.OUR_DATE_FORMAT);

		write.write("alabama,"+sdf.format(aLittleAfterNow)+"\n");
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
}
