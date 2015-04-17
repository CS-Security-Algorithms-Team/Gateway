import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;


public final class RequestProcessor implements Runnable 
{
	
	public static final String EXPIRED = "EXPIRED";
	public static final String INVALID = "INVALID";
	
	public static final String NEW_CONTENT = "NEW_CONTENT";
	public static final String PREVIOUS_TOKEN = "PREVIOUS_TOKEN";
	public static final String CONTENT_LIST = "CONTENT_LIST";
	
	public static final String INSERT_TOKEN = "INSERT INTO usedTokens (token,expiration,lastArticleId) VALUES (?,?,?)";
	public static final String CHECK_IF_USED = "SELECT token FROM usedTokens WHERE token = ?";
	public static final String MEDIA_FROM_OLD = "SELECT lastArticleId FROM usedTokens WHERE token = ?";

	public static final String OUR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";


	private Socket customer;
	//private Socket contentProvider;
	private MediaParams mediaParams;
	private DBConGetter connectionGetter;
	private Exception exception = null; //not sure how much java 8 to do??
	
	public RequestProcessor(Socket customer, MediaParams mediaParams, DBConGetter connectionGetter)
	{
		this.customer = customer;
		this.mediaParams = mediaParams;
		this.connectionGetter = connectionGetter;
	}
	
	public void run()
	{

		Socket contentProvider = null;
		try {
			contentProvider = mediaParams.makeNewSocket();
			System.out.println("Run started");
			BufferedReader reader = new BufferedReader(new InputStreamReader(customer.getInputStream()));

			String mode = reader.readLine();
			if (!mode.equals("CONTENT_LIST"))
			{
				String token = reader.readLine();
				String mediaRequestOrOldToken = reader.readLine();

				token = token.replace("TOKEN|", "");

				System.out.println("TOKEN NOW HERE: "+ token);

				String[] tokenPieces = token.split("\\|");

				System.out.println("TOKEN PIECes: "+ Arrays.asList(tokenPieces));

				SimpleDateFormat sdf = new SimpleDateFormat(OUR_DATE_FORMAT);
				Date tokenExpires = null;
				try {
					tokenExpires = sdf.parse(tokenPieces[1]);
				} catch (ParseException e) {
					e.printStackTrace();
				}

				System.out.println("GOT HERE!!");
				if (tokenExpires.before(new Date())) {
					customer.getOutputStream().write((EXPIRED + "\n").getBytes());
					return;
				}

				Connection dbCon = connectionGetter.getConnection();

				if (tokenAlreadyUsed(tokenPieces[0], dbCon)) {
					customer.getOutputStream().write((INVALID + "\n").getBytes());
					return;
				} else {
					System.out.println("Token not already used.");
				}



				if (mode.equals(NEW_CONTENT)) {

					contentProvider.getOutputStream().write((mediaRequestOrOldToken + "\n").getBytes());

					copyStream(contentProvider.getInputStream(), customer.getOutputStream());
					recordToken(tokenPieces[0], tokenExpires, Integer.parseInt(mediaRequestOrOldToken), dbCon);
				} else if (mode.equals(PREVIOUS_TOKEN)) {
					//get previous token
					//send request
					//copy back
					String oldMedia = retrieveTokenContent(mediaRequestOrOldToken, dbCon);
					contentProvider.getOutputStream().write((oldMedia + "\n").getBytes());
					copyStream(contentProvider.getInputStream(), customer.getOutputStream());
					recordToken(tokenPieces[0], tokenExpires, Integer.parseInt(oldMedia), dbCon);
				}
			}
			else if (mode.equals(CONTENT_LIST)) {
				//send an array of = sign contents
				System.out.println("GOT HERE");
				contentProvider.getOutputStream().write(new String("*" + "\n").getBytes());
				copyStream(contentProvider.getInputStream(), customer.getOutputStream());
			} else {
				throw new IllegalArgumentException("Unsupported mode: " + mode);
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
			exception = e;
		}
		finally
		{
			try {
				customer.close();
				if (contentProvider != null)
				{
					contentProvider.close();
				}
			} catch (IOException e) {
				exception = e;
				e.printStackTrace();
			}

		}
	}
	
	public Exception getException()
	{
		return exception;
	}
	
	public static void recordToken(String tokenId, Date expires, int mediaRequest, Connection con) throws SQLException
	{
		PreparedStatement ps = con.prepareStatement(INSERT_TOKEN);
		ps.setString(1, tokenId);
		ps.setTimestamp(2, new Timestamp(expires.getTime()));
		ps.setInt(3, mediaRequest);
		ps.execute();
	}
	
	public static boolean tokenAlreadyUsed(String tokenId, Connection con) throws SQLException
	{
		//try w/ resources auto-closes
		try(PreparedStatement ps = con.prepareStatement(CHECK_IF_USED);)
		{
			ps.setString(1, tokenId);
			try (ResultSet rs = ps.executeQuery();)
			{
				return rs.next();
			}
		}
	}
	
	public static String retrieveTokenContent(String tokenId, Connection con) throws SQLException
	{
		//connect to database
		//select article id
		//return article id
		PreparedStatement ps = con.prepareStatement(MEDIA_FROM_OLD);
		ps.setString(1, tokenId);
		ResultSet rs = ps.executeQuery();
		if (!rs.next())
		{
			throw new IllegalArgumentException("No token with id "+tokenId+" found");
		}
		return Integer.toString(rs.getInt("lastArticleId"));
	}
	
	public static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		byte[] buffer = new byte[8192];
		int lenRead = 0;
		while ((lenRead = inputStream.read(buffer,0,buffer.length))!=-1)
		{
			outputStream.write(buffer, 0, lenRead);
		}
		
	}
	
	public static final class MediaParams
	{
		public String url;
		public int port;
		
		public MediaParams(String url, int port)
		{
			this.url = url;
			this.port = port;
		}
		
		public String url()
		{
			return url;
		}
		
		public int port()
		{
			return port;
		}
		
		public Socket makeNewSocket() throws UnknownHostException, IOException
		{
			return new Socket(url,port);
		}
		
	}
	
	public interface DBConGetter
	{
		public Connection getConnection() throws SQLException;
	}
	
}
