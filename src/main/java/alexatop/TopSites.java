package alexatop;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Simple demo showing how to make a successful request to Alexa Top Sites.
 * <p/>
 * Note that you must sign up for Alexa Top Sites
 * at http://aws.amazon.com/alexatopsites before running this demo.
 */
public class TopSites {
    protected static final String ACTION_NAME = "TopSites";
    protected static final String RESPONSE_GROUP_NAME = "Country";
    protected static final String SERVICE_HOST = "ats.amazonaws.com";
    protected static final String AWS_BASE_URL = "http://" + SERVICE_HOST + "/?";

    protected static final int NUMBER_TO_RETURN = 600;
    protected static final int START_NUMBER = 1;
    protected static final String DATEFORMAT_AWS = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String HASH_ALGORITHM = "HmacSHA256";

    private String accessKeyId;
    private String secretAccessKey;
    private String countryCode;

    public TopSites(String accessKeyId, String secretAccessKey, String countryCode) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.countryCode = countryCode;
    }

    /**
     * Computes RFC 2104-compliant HMAC signature.
     *
     * @param data data to be signed.
     * @return base64-encoded RFC 2104-compliant HMAC signature.
     * @throws java.security.SignatureException
     *          when signature generation fails
     */
    protected String generateSignature(String data)
        throws java.security.SignatureException {
        String result;
        try {
            // get an hmac key from the raw key bytes
            SecretKeySpec signingKey =
                new SecretKeySpec(secretAccessKey.getBytes(),
                                  HASH_ALGORITHM);

            // get a mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());

            // base64-encode the hmac
            result = new BASE64Encoder().encode(rawHmac);

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                                         + e.getMessage());
        }
        return result;
    }

    /**
     * Generates a timestamp for use with AWS request signing
     *
     * @param date current date
     * @return timestamp
     */
    public static String getTimestampFromLocalTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(DATEFORMAT_AWS);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }

    /**
     * Builds the query string
     */
    protected String buildQuery() throws UnsupportedEncodingException {
        String timestamp = getTimestampFromLocalTime(Calendar.getInstance().getTime());

        // TreeMap puts keys in alphabetical order
        Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("Action", ACTION_NAME);
        queryParams.put("ResponseGroup", RESPONSE_GROUP_NAME);
        queryParams.put("AWSAccessKeyId", accessKeyId);
        queryParams.put("Timestamp", URLEncoder.encode(timestamp, "UTF-8"));
        queryParams.put("CountryCode", countryCode);
        queryParams.put("Count", "" + NUMBER_TO_RETURN);
        queryParams.put("Start", "" + START_NUMBER);
        queryParams.put("SignatureVersion", "2");
        queryParams.put("SignatureMethod", HASH_ALGORITHM);

        String query = "";
        boolean first = true;
        for (String name : queryParams.keySet()) {
            if (first)
                first = false;
            else
                query += "&";

            query += name + "=" + queryParams.get(name);
        }

        return query;
    }

    /**
     * Parses response and return result as List
     *
     * @param in stream containing response xml
     */
    private static List<String> parseResponse(InputStream in) throws Exception {
        List<String> list =new ArrayList<String>();
    	// Parse the response
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document responseDoc = dbf.newDocumentBuilder().parse(in);

        NodeList responses = responseDoc.getElementsByTagNameNS("*", "Site");

        for (int i = 0; i < responses.getLength(); i++) {
            Element response = (Element) responses.item(i);
            Element node = (Element)
                response.getElementsByTagNameNS("*", "DataUrl").item(0);
            String siteUrl = node.getFirstChild().getNodeValue();
            //System.out.println(siteUrl);
            list.add(siteUrl);
        }
        return list;
    }
    

    /**
     * Makes a request to the Alexa Top Sites web service
     */
    public static void main(String[] args) throws Exception {
//        if (args.length < 2) {
//            System.out.println("Usage: java AlexaTopSites ACCESS_KEY_ID " +
//                               "SECRET_ACCESS_KEY [COUNTRY_CODE]");
//            System.exit(-1);
//        }
        String accessKey ="";
        String secretKey = "";
        String countryCode = "US";

        TopSites topSites = new TopSites(accessKey, secretKey, countryCode);
        String query = topSites.buildQuery();

        String toSign = "GET\n" + SERVICE_HOST + "\n/\n" + query;

        System.out.println("String to sign:\n" + toSign + "\n");

        String signature = topSites.generateSignature(toSign);

        String uri = AWS_BASE_URL + query +
            "&Signature=" + URLEncoder.encode(signature, "UTF-8");

        // Make request
        System.out.println("\nMaking request to: " + uri + "\n");

        URL url = new URL(uri);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
       
        List<String> list=parseResponse(in);
        List<String> tldFilter = Arrays.asList("com");// only gathering .com
        try(BufferedWriter bw=Files.newBufferedWriter(Paths.get(TopSites.class.getResource("/list.txt").getPath()))){
//        	for(String u:list){
//        		System.out.println(u);
//        		bw.write(u+"\n");
//        	}
        	list.stream().forEach(System.out::println);
        }
    }

}
