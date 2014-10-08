package crawler;
/* TweetTracker. Copyright (c) Arizona Board of Regents on behalf of Arizona State University
 * @author shamanth
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class TweetsStreamingAPI
{
    OAuthTokenSecret OAuthToken;
    final int RECORDS_TO_PROCESS = 1000;
    final int MAX_GEOBOXES = 25;
    final int MAX_KEYWORDS = 400;
    final int MAX_USERS = 5000;
    HashSet<String> Keywords;
    HashSet<String> Geoboxes;
    HashSet<String> Userids;
    final String CONFIG_FILE_PATH = "streaming/query.txt";
    final String DEF_OUTPATH = "streaming/";

    /**
     * Loads the Twitter access token and secret for a user
     */    
    public void LoadTwitterToken()
    {
        OAuthExample oae = new OAuthExample();
        OAuthToken =  oae.GetUserAccessKeySecret();
        //OAuthToken = OAuthExample.DEBUGUserAccessSecret();
    }

    /**
     * Creates a connection to the Streaming Filter API
     * @param baseUrl the URL for Twitter Filter API
     * @param outFilePath Location to place the exported file
     */
    public void CreateStreamingConnection(String baseUrl, String outFilePath)
    {
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(90000));
        //Step 1: Initialize OAuth Consumer
        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(OAuthUtils.CONSUMER_KEY,OAuthUtils.CONSUMER_SECRET);
        consumer.setTokenWithSecret(OAuthToken.getAccessToken(),OAuthToken.getAccessSecret());
        //Step 2: Create a new HTTP POST request and set parameters
        HttpPost httppost = new HttpPost(baseUrl);
        try {            
            httppost.setEntity(new UrlEncodedFormEntity(CreateRequestBody(), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        try {
             //Step 3: Sign the request
                consumer.sign(httppost);
            } catch (OAuthMessageSignerException ex) {
                ex.printStackTrace();
            } catch (OAuthExpectationFailedException ex) {
                ex.printStackTrace();
            } catch (OAuthCommunicationException ex) {
                ex.printStackTrace();
            }
        HttpResponse response;
        InputStream is = null;
        try {
             //Step 4: Connect to the API
                response = httpClient.execute(httppost);
                if (response.getStatusLine().getStatusCode()!= HttpStatus.SC_OK)
                {
                    throw new IOException("Got status " +response.getStatusLine().getStatusCode());
                }
                else
                {
                    System.out.println(OAuthToken.getAccessToken()+ ": Processing from " + baseUrl);
                    HttpEntity entity = response.getEntity();
                    try {
                        is = entity.getContent();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                    //Step 5: Process the incoming Tweet Stream
                    this.ProcessTwitterStream(is, outFilePath);
                }
         } catch (IOException ex) {
            ex.printStackTrace();
        }finally {
            // Abort the method, otherwise releaseConnection() will
            // attempt to finish reading the never-ending response.
            // These methods do not throw exceptions.
            if(is!=null)
            {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     *  Processes a stream of tweets and writes them to a file one tweet per line. Each tweet here is represented by a JSON document.
     * @param is input stream already connected to the streaming API
     * @param outFilePath file to put the collected tweets in
     * @throws InterruptedException
     * @throws IOException
     */
    public void ProcessTwitterStream(InputStream is, String outFilePath)
    {
    	BufferedWriter bwrite = null;
        try {
            JSONTokener jsonTokener = new JSONTokener(new InputStreamReader(is, "UTF-8"));
            ArrayList<JSONObject> rawtweets = new ArrayList<JSONObject>();
            int nooftweetsuploaded = 0;
            while (true) {
                try {                    
                    JSONObject temp = new JSONObject(jsonTokener);                    
                    rawtweets.add(temp);
                    System.out.println(rawtweets.size());
                    if (rawtweets.size() >= RECORDS_TO_PROCESS)
                    {
                        Calendar cal = Calendar.getInstance();
                        String filename = outFilePath + "tweets_" + cal.getTimeInMillis() + ".json";
                        bwrite = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
                        nooftweetsuploaded += RECORDS_TO_PROCESS;
                        //Write the collected tweets to a file
                        for (JSONObject jobj : rawtweets) {
                            bwrite.write(jobj.toString());
                            bwrite.newLine();
                        }
                        System.out.println("Written "+nooftweetsuploaded+" records so far");
                        bwrite.close();
                        rawtweets.clear();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }                
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } 
    }

    public static void main(String[] args)
    {
        TweetsStreamingAPI sae = new TweetsStreamingAPI();
        sae.LoadTwitterToken();
        //load parameters from a TSV file
        String filename = sae.CONFIG_FILE_PATH;
        String outfilepath = sae.DEF_OUTPATH;
        if(args!=null)
        {
            if(args.length>0)
            {
                filename = args[0];
            }
            if(args.length>1)
            {
                File fl = new File(args[1]);
                if(fl.exists()&&fl.isDirectory())
                {
                    outfilepath = args[1];
                }
            }
        }
        sae.ReadParameters(filename);
        sae.CreateStreamingConnection("https://stream.twitter.com/1.1/statuses/filter.json", outfilepath);
    }

    /**
     * Reads the file and loads the parameters to be crawled. Expects that the parameters are tab separated values and the
     * @param filename
     */
    public void ReadParameters(String filename)
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String temp = "";
            int count = 1;
            if(Userids==null)
            {
                Userids = new HashSet<String>();
            }
            if(Geoboxes==null)
            {
                Geoboxes = new HashSet<String>();
            }
            if(Keywords==null)
            {
                Keywords = new HashSet<String>();
            }
            while((temp = br.readLine())!=null)
            {
                if(!temp.isEmpty())
                {
                    if(count==1)
                    {                        
                        String[] keywords = temp.split("\t");
                        HashSet<String> temptags = new HashSet<String>();
                        for(String word:keywords)
                        {
                            if(!temptags.contains(word))
                            {
                                temptags.add(word);
                            }
                        }
                        FilterKeywords(temptags);
                    }
                    else
                    if(count==2)
                    {
                        String[] geoboxes = temp.split("\t");
                        HashSet<String> tempboxes = new HashSet<String>();
                        for(String box:geoboxes)
                        {
                            if(!tempboxes.contains(box))
                            {
                                tempboxes.add(box);
                            }
                        }
                        FilterGeoboxes(tempboxes);
                    }
                    else
                    if(count==3)
                    {
                        String[] userids = temp.split("\t");
                        HashSet<String> tempids = new HashSet<String>();
                        for(String id:userids)
                        {
                            if(!tempids.contains(id))
                            {
                                tempids.add(id);
                            }
                        }
                        FilterUserids(tempids);
                    }
                    count++;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        finally{
            try {
                br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void FilterUserids(HashSet<String> userids)
    {
        if(userids!=null)
        {
            int maxsize = MAX_USERS;
            if(userids.size()<maxsize)
            {
                maxsize = userids.size();
            }
            for(String id:userids)
            {
                Userids.add(id);
            }
        }
    }

    private void FilterGeoboxes(HashSet<String> geoboxes)
    {
        if(geoboxes!=null)
        {
            int maxsize = MAX_GEOBOXES;
            if(geoboxes.size()<maxsize)
            {
                maxsize = geoboxes.size();
            }
            for(String box:geoboxes)
            {
                Geoboxes.add(box);
            }
        }
    }
    /**
     * Keep only the maximum permitted number of parameters for a connection. Ignoring the rest.
     * This can be extended to create multiple sets to be crawled by different threads.
     */
    private void FilterKeywords(HashSet<String> hashtags)
    {          
        if(hashtags!=null)
        {
            int maxsize = MAX_KEYWORDS;
            if(hashtags.size()<maxsize)
            {
                maxsize = hashtags.size();
            }
            for(String tag:hashtags)
            {
                Keywords.add(tag);
            }
        }
             
    }

     private List<NameValuePair> CreateRequestBody()
     {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if(Userids != null&&Userids.size()>0)
        {
            params.add(CreateNameValuePair("follow", Userids));
            System.out.println("userids = "+Userids);
        }
        if (Geoboxes != null&&Geoboxes.size()>0) {
            params.add(CreateNameValuePair("locations", Geoboxes));
            System.out.println("locations = "+Geoboxes);

        }
        if (Keywords != null&&Keywords.size()>0) {
            params.add(CreateNameValuePair("track", Keywords));
            System.out.println("keywords = "+Keywords);
        }
        return params;
    }

    private NameValuePair CreateNameValuePair(String name, Collection<String> items)
    {
        StringBuilder sb = new StringBuilder();
        boolean needComma = false;
        for (String item : items) {
            if (needComma) {
                sb.append(',');
            }
            needComma = true;
            sb.append(item);
        }
        return new BasicNameValuePair(name, sb.toString());
    }
}
