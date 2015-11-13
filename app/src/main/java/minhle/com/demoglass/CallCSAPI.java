package minhle.com.demoglass;

/**
 * Created by phongnn57 on 11/13/15.
 */
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

public class CallCSAPI {

    private static final String API_KEY = "cNSiII2zZzS_TC9D2D0ZVw";

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();



    public static void main(String[] args) throws Exception {
        CSApi api = new CSApi(
                HTTP_TRANSPORT,
                JSON_FACTORY,
                API_KEY
        );
        CSPostConfig imageToPost = CSPostConfig.newBuilder()
                .withRemoteImageUrl("https://i.ytimg.com/vi/KY4IzMcjX3Y/maxresdefault.jpg")
                .build();

        CSPostResult portResult = api.postImage(imageToPost);

        System.out.println("Post result: " + portResult);

        Thread.sleep(30000);

        CSGetResult scoredResult = api.getImage(portResult);

//        System.out.println(scoredResult);
        System.out.print(scoredResult.getName());
    }
}
