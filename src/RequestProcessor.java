import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public final class RequestProcessor implements Runnable 
{
	
	public static final String EXPIRED = "EXPIRED";
	public static final String INVALID = "INVALID";
	
	public static final String NEW_CONTENT = "NEW_CONTENT";
	public static final String PREVIOUS_TOKEN = "PREVIOUS_TOKEN";
	
	public static final String INSERT_TOKEN = "INSERT INTO usedTokens (token,expiration,lastArticleId) VALUES (?,?,?)";
	public static final String CHECK_IF_USED = "SELECT token FROM usedTokens WHERE token = ?";
	public static final String MEDIA_FROM_OLD = "SELECT lastArticleId FROM usedTokens WHERE token = ?";
		
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
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(customer.getInputStream()));
			
			String mode = reader.readLine();
			String token = reader.readLine();
			String mediaRequest = reader.readLine();
			
			String[] tokenPieces = token.split(",");
			
			LocalDateTime tokenExpires = LocalDateTime.parse(tokenPieces[1],DateTimeFormatter.ISO_LOCAL_DATE_TIME);
				
			if (tokenExpires.isAfter(LocalDateTime.now()))
			{
				customer.getOutputStream().write((EXPIRED+"\n").getBytes());
				return;
			}
			
			Connection dbCon = connectionGetter.getConnection();
			
			if (tokenAlreadyUsed(tokenPieces[1], dbCon))
			{
				customer.getOutputStream().write((INVALID+"\n").getBytes());
			}
			
			Socket contentProvider = mediaParams.makeNewSocket();
			
			if (mode.equals(NEW_CONTENT))
			{
				
				//Can't really check if token is valid so...
				contentProvider.getOutputStream().write(mediaRequest.getBytes());
				
				copyStream(contentProvider.getInputStream(), customer.getOutputStream());
			}
			else if (mode.equals(PREVIOUS_TOKEN))
			{
				//get previous token
				//send request
				//copy back
				throw new UnsupportedOperationException("NOT IMPLEMENTED");
			}
			else
			{
				throw new IllegalArgumentException("Unsupported mode: "+mode);
			}
		}
		catch (IOException |SQLException e)
		{
			e.printStackTrace();
			exception= e;
		}
	}
	
	public Exception getException()
	{
		return exception;
	}
	
	public static void recordToken(String tokenId, LocalDateTime expires, int mediaRequest, Connection con) throws SQLException
	{
		PreparedStatement ps = con.prepareStatement(INSERT_TOKEN);
		ps.setString(1, tokenId);
		ps.setTimestamp(2, Timestamp.valueOf(expires));
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
				return rs.isBeforeFirst(); //will return false if already in use
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
	
	public static interface DBConGetter 
	{
		public Connection getConnection() throws SQLException;
	}
	
}
